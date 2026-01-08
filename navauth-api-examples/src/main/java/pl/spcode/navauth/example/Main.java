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

package pl.spcode.navauth.example;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;
import pl.spcode.navauth.api.NavAuthAPI;
import pl.spcode.navauth.api.event.NavAuthEventListener;
import pl.spcode.navauth.example.listener.NavAuthListeners;

@Plugin(
    id = "navauthexamples",
    name = "NavAuthExamples",
    version = "1.0",
    url = "https://spcode.pl/navauth",
    description = "Example usage of NavAuth API",
    authors = {"Navio1430"},
    dependencies = {@Dependency(id = "navauth")})
public class Main implements NavAuthEventListener {

  @Inject private Logger logger;
  @Inject private Injector injector;

  @Subscribe
  void onProxyInitialization(final ProxyInitializeEvent event) {
    logger.info("Loading NavAuth examples plugin...");
    var api = NavAuthAPI.getInstance();
    api.getEventBus().register(injector.getInstance(NavAuthListeners.class));
  }
}
