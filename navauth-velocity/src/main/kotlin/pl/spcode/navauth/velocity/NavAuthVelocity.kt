/*
 * NavAuth
 * Copyright © 2025 Oliwier Fijas (Navio1430)
 *
 * NavAuth is free software; You can redistribute it and/or modify it under the terms of:
 * the GNU Affero General Public License version 3 as published by the Free Software Foundation.
 *
 * NavAuth is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with NavAuth. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 *
 */

package pl.spcode.navauth.velocity

import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Singleton
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.velocity.LiteVelocityFactory
import java.nio.file.Path
import net.kyori.adventure.text.Component
import org.bstats.velocity.Metrics
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.api.NavAuthAPI
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.command.exception.MissingPermissionException
import pl.spcode.navauth.common.command.exception.UserResolveException
import pl.spcode.navauth.common.command.handler.UserResolveExceptionHandler
import pl.spcode.navauth.common.command.user.UsernameOrUuidParser
import pl.spcode.navauth.common.command.user.UsernameOrUuidRaw
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.module.*
import pl.spcode.navauth.velocity.command.CommandsRegistry
import pl.spcode.navauth.velocity.infra.command.VelocityInvalidUsageHandler
import pl.spcode.navauth.velocity.infra.command.VelocityMissingPermissionExceptionHandler
import pl.spcode.navauth.velocity.infra.command.VelocityMissingPermissionHandler
import pl.spcode.navauth.velocity.infra.component.VelocityAudienceProvider
import pl.spcode.navauth.velocity.listener.VelocityListenersRegistry
import pl.spcode.navauth.velocity.listener.application.UserAuthenticatedEventListener
import pl.spcode.navauth.velocity.module.SchedulerModule
import pl.spcode.navauth.velocity.module.VelocityCommandsModule
import pl.spcode.navauth.velocity.module.VelocityMultificationsModule
import pl.spcode.navauth.velocity.module.VelocityServicesModule
import pl.spcode.navauth.velocity.multification.VelocityViewerProvider

@Singleton
class NavAuthVelocity
@Inject
constructor(
  val parentInjector: Injector,
  val proxyServer: ProxyServer,
  @param:DataDirectory val dataDirectory: Path,
  val metricsFactory: Metrics.Factory,
) : VelocityPluginProvider {

  private val logger: Logger = LoggerFactory.getLogger(NavAuthVelocity::class.java)

  // chicken or egg problem, unfortunately
  var pluginInstance: Bootstrap? = null
  lateinit var injector: Injector

  lateinit var liteCommands: LiteCommands<CommandSource>

  fun init() {
    try {
      logger.info("Initializing NavAuth plugin...")

      val generalConfigModule =
        YamlConfigModule(
          GeneralConfig::class,
          dataDirectory.resolve("general.yml").toFile(),
          autoBindSubconfigs = true,
        )

      val velocityViewerProvider = VelocityViewerProvider(proxyServer)
      val messagesConfigModule =
        YamlConfigModule(MessagesConfig::class, dataDirectory.resolve("messages.yml").toFile())

      val migrationConfigModule =
        YamlConfigModule(MigrationConfig::class, dataDirectory.resolve("migration.yml").toFile())

      injector =
        parentInjector.createChildInjector(
          PluginDirectoryModule(dataDirectory),
          // loading hierarchy here is crucial
          generalConfigModule,
          messagesConfigModule,
          migrationConfigModule,
          EventsModule(),
          VelocityMultificationsModule(velocityViewerProvider),
          VelocityCommandsModule(),
          SchedulerModule(this, proxyServer.scheduler),
          HttpClientModule(),
          DataPersistenceModule(),
          ServicesModule(),
          VelocityServicesModule(),
          MigrationModule(),
        )

      val eventBus = injector.getInstance(NavAuthEventBus::class.java)
      eventBus.register(injector.getInstance(UserAuthenticatedEventListener::class.java))

      connectAndInitDatabase()

      val apiImpl = injector.getInstance(NavAuthApiImpl::class.java)
      NavAuthAPI.setAPIInstance(apiImpl)
    } catch (ex: Exception) {
      logger.error("Could not initialize NavAuth plugin, shutting down the server...", ex)
      proxyServer.shutdown(Component.text("NavAuth initialization failure"))
    }
  }

  fun onProxyInitializeEvent(pluginInstance: Bootstrap) {
    try {
      this.pluginInstance = pluginInstance

      // initialize bstats
      val pluginId = 28777
      metricsFactory.make(pluginInstance, pluginId)

      registerListeners(injector)
      registerCommands(injector)
    } catch (ex: Exception) {
      logger.error("Could not initialize NavAuth plugin, shutting down the server...", ex)
      proxyServer.shutdown(Component.text("NavAuth initialization failure"))
    }
  }

  fun connectAndInitDatabase() {
    injector.getInstance(DatabaseManager::class.java).connectAndInit()
  }

  fun registerCommands(injector: Injector) {
    val commands = CommandsRegistry.getWithInjection(injector)

    val userArgumentParser =
      injector.getInstance(object : Key<UsernameOrUuidParser<CommandSource>>() {})

    this.liteCommands =
      LiteVelocityFactory.builder(this.proxyServer)
        .commands(*commands.toTypedArray())
        .argumentParser(UsernameOrUuidRaw::class.java, userArgumentParser)
        .exception(
          UserResolveException::class.java,
          UserResolveExceptionHandler(VelocityAudienceProvider(proxyServer)),
        )
        .exception(
          MissingPermissionException::class.java,
          injector.getInstance(VelocityMissingPermissionExceptionHandler::class.java),
        )
        .missingPermission(injector.getInstance(VelocityMissingPermissionHandler::class.java))
        .invalidUsage(injector.getInstance(VelocityInvalidUsageHandler::class.java))
        .build()
  }

  fun registerListeners(injector: Injector) {
    // register self as listener because of the shutdown event
    proxyServer.eventManager.register(pluginInstance, this)

    val listeners = VelocityListenersRegistry.getWithInjection(injector)
    listeners.forEach { proxyServer.eventManager.register(pluginInstance, it) }
  }

  @Suppress("UNNECESSARY_SAFE_CALL")
  @Subscribe
  fun shutdown(event: ProxyShutdownEvent) {
    injector?.getInstance(DatabaseManager::class.java)?.closeConnections()
    liteCommands?.unregister()

    logger.info("Goodbye!")
  }

  override fun provideInstance(): Bootstrap {
    require(pluginInstance != null) { "plugin was not initialized yet" }
    return pluginInstance!!
  }
}
