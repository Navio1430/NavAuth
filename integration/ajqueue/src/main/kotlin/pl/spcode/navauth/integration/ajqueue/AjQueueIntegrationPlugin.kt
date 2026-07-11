/*
 * NavAuth
 * Copyright © 2026 Oliwier Fijas (Navio1430)
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

package pl.spcode.navauth.integration.ajqueue

import com.google.inject.Inject
import com.google.inject.Injector
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import java.nio.file.Path
import org.slf4j.Logger
import pl.spcode.navauth.api.NavAuthAPI
import pl.spcode.navauth.common.module.YamlConfigModule
import pl.spcode.navauth.integration.ajqueue.config.GeneralConfig
import pl.spcode.navauth.integration.ajqueue.listener.AjQueueListeners
import pl.spcode.navauth.integration.ajqueue.listener.NavAuthListeners

@Plugin(
  id = "navauth-ajqueue",
  name = "NavAuth AJQueue Integration",
  version = BuildParameters.VERSION,
  dependencies = [Dependency(id = "navauth"), Dependency(id = "ajqueue")],
  authors = ["Navio1430"],
)
class AjQueueIntegrationPlugin
@Inject
constructor(
  val parentInjector: Injector,
  @param:DataDirectory val dataDirectory: Path,
  val logger: Logger,
) {

  @Subscribe
  fun onProxyInitializeEvent(event: ProxyInitializeEvent) {
    try {
      logger.info("Initializing NavAuth AJQueue integration...")

      val configModule =
        YamlConfigModule(GeneralConfig::class, dataDirectory.resolve("config.yml").toFile())

      val childInjector = parentInjector.createChildInjector(configModule)

      val eventBus = NavAuthAPI.getInstance().eventBus
      eventBus.register(childInjector.getInstance(NavAuthListeners::class.java))

      val ajQueueListeners = childInjector.getInstance(AjQueueListeners::class.java)
      ajQueueListeners.registerAll()

      logger.info("NavAuth AJQueue integration initialized successfully")
    } catch (ex: Exception) {
      logger.error("Failed to initialize NavAuth AJQueue integration", ex)
    }
  }
}
