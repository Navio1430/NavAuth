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
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import pl.spcode.navauth.common.infra.database.DatabaseConfig
import pl.spcode.navauth.common.infra.database.DatabaseDriverType
import pl.spcode.navauth.common.module.DataPersistenceModule

@Singleton
class NavAuthVelocity @Inject constructor(val logger: Logger, val parentInjector: Injector, val proxyServer: ProxyServer) {

  lateinit var pluginInstance: Bootstrap
  lateinit var injector: Injector

  fun init(event: ProxyInitializeEvent, pluginInstance: Bootstrap) {
    logger.info("Initializing NavAuth plugin...")

    proxyServer.eventManager.register(pluginInstance, this)

    val databaseConfig =
      DatabaseConfig(DatabaseDriverType.H2_MEM, 5, 30000, "", "", "", 0, "default")
    injector = parentInjector.createChildInjector(DataPersistenceModule(databaseConfig))
  }

  @Subscribe
  fun shutdown(proxyShutdownEvent: ProxyShutdownEvent) {
    // todo: close database connection on plugin shutdown
  }

}
