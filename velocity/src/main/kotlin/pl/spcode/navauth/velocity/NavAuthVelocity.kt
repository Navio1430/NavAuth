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
import com.velocitypowered.api.proxy.ProxyServer
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.velocity.LiteVelocityFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.infra.database.DatabaseConfig
import pl.spcode.navauth.common.infra.database.DatabaseDriverType
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.module.DataPersistenceModule
import pl.spcode.navauth.velocity.command.CommandsRegistrar
import pl.spcode.navauth.velocity.command.user.LoginCommand

@Singleton
class NavAuthVelocity
@Inject
constructor(val parentInjector: Injector, val proxyServer: ProxyServer) {

  private val logger: Logger = LoggerFactory.getLogger(NavAuthVelocity::class.java)

  lateinit var pluginInstance: Bootstrap
  lateinit var injector: Injector

  lateinit var liteCommands: LiteCommands<CommandSource>

  fun init(event: ProxyInitializeEvent, pluginInstance: Bootstrap) {
    // todo: do not let proxy to start on any errors

    logger.info("Initializing NavAuth plugin...")
    this.pluginInstance = pluginInstance

    proxyServer.eventManager.register(pluginInstance, this)

    val databaseConfig =
      DatabaseConfig(DatabaseDriverType.H2_MEM, 5, 30000, "", "", "", 0, "default")
    injector = parentInjector.createChildInjector(DataPersistenceModule(databaseConfig))

    registerCommands()
  }

  fun registerCommands() {

    // todo: inject into commands

    this.liteCommands = LiteVelocityFactory.builder(this.proxyServer)
        .commands(
            *CommandsRegistrar.commands.toTypedArray()
        )
        .build();
  }

  @Suppress("UNNECESSARY_SAFE_CALL")
  @Subscribe
  fun shutdown(proxyShutdownEvent: ProxyShutdownEvent) {
    injector?.getInstance(DatabaseManager::class.java)?.closeConnections()
    liteCommands?.unregister()
  }
}
