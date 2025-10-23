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

package pl.spcode.navauth.velocity;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(
    id = "navauth",
    name = "NavAuth",
    version = "@version@",
    url = "https://spcode.pl/navauth",
    description = "Minecraft login plugin built for speed, security, and seamless player authentication.",
    authors = {"Navio1430"})
public class Bootstrap {
  @Inject
  private Logger logger;
  @Inject
  private Injector injector;

  @Subscribe
  void onProxyInitialization(final ProxyInitializeEvent event) {
    logger.info("Bootstrapping velocity plugin...");
    NavAuthVelocity instance = injector.getInstance(NavAuthVelocity.class);
    instance.init(event, this);
  }
}