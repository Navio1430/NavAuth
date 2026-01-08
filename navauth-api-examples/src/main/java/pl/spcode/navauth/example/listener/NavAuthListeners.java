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

package pl.spcode.navauth.example.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import pl.spcode.navauth.api.event.NavAuthEventListener;
import pl.spcode.navauth.api.event.Subscribe;
import pl.spcode.navauth.api.event.user.UserAuthenticatedEvent;

public class NavAuthListeners implements NavAuthEventListener {

  @Inject private ProxyServer server;

  @Subscribe
  public void handleUserAuthenticatedEvent(UserAuthenticatedEvent event) {
    server
        .getPlayer(event.getPlayer().getIdentifier())
        .get()
        .sendMessage(
            Component.text("You are now logged in! Session type: " + event.getSessionType()));
  }
}
