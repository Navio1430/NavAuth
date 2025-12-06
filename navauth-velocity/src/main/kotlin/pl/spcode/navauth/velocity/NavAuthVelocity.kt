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
import com.google.inject.Singleton
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.velocity.LiteVelocityFactory
import net.kyori.adventure.text.Component
import java.nio.file.Path
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.module.DataPersistenceModule
import pl.spcode.navauth.common.module.HttpClientModule
import pl.spcode.navauth.common.module.ServicesModule
import pl.spcode.navauth.common.module.YamlConfigModule
import pl.spcode.navauth.velocity.command.CommandsRegistry
import pl.spcode.navauth.velocity.listener.VelocityListenersRegistry
import pl.spcode.navauth.velocity.module.SchedulerModule
import pl.spcode.navauth.velocity.module.VelocityServicesModule

@Singleton
class NavAuthVelocity
@Inject
constructor(
  val parentInjector: Injector,
  val proxyServer: ProxyServer,
  @param:DataDirectory val dataDirectory: Path,
) {

  private val logger: Logger = LoggerFactory.getLogger(NavAuthVelocity::class.java)

  lateinit var pluginInstance: Bootstrap
  lateinit var injector: Injector

  lateinit var liteCommands: LiteCommands<CommandSource>

  fun init(event: ProxyInitializeEvent, pluginInstance: Bootstrap) {
    try {
      logger.info("Initializing NavAuth plugin...")
      this.pluginInstance = pluginInstance

      // register self as listener because of the shutdown event
      proxyServer.eventManager.register(pluginInstance, this)

      val generalConfigModule =
          YamlConfigModule(GeneralConfig::class, dataDirectory.resolve("general.yml").toFile())

      injector =
          parentInjector.createChildInjector(
              // loading hierarchy here is crucial
              generalConfigModule,
              SchedulerModule(pluginInstance, proxyServer.scheduler),
              HttpClientModule(),
              DataPersistenceModule(),
              ServicesModule(),
              VelocityServicesModule(),
          )

      connectAndInitDatabase()

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
    this.liteCommands =
      LiteVelocityFactory.builder(this.proxyServer).commands(*commands.toTypedArray()).build()
  }

  fun registerListeners(injector: Injector) {
    val listeners = VelocityListenersRegistry.getWithInjection(injector)
    listeners.forEach { proxyServer.eventManager.register(pluginInstance, it) }
  }

  @Suppress("UNNECESSARY_SAFE_CALL")
  @Subscribe
  fun shutdown(proxyShutdownEvent: ProxyShutdownEvent) {
    injector?.getInstance(DatabaseManager::class.java)?.closeConnections()
    liteCommands?.unregister()

    logger.info("Goodbye!")
  }
}
