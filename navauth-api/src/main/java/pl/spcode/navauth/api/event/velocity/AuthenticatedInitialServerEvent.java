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

package pl.spcode.navauth.api.event.velocity;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import pl.spcode.navauth.api.event.NavAuthEvent;

/**
 * Represents an event that occurs after a player is connected and authenticated,
 * (either by auto-login or manual login) providing information about
 * the player and the initial server they are directed to.
 * The initial server associated with this event can be modified during event
 * processing to redirect the player to a different server.
 */
public class AuthenticatedInitialServerEvent implements NavAuthEvent {

  private final Player player;
  private @Nullable RegisteredServer initialServer;

  /**
   * @param player the player that was connected
   * @param initialServer the initial server selected by NavAuth, may be {@code null}
   */
  public AuthenticatedInitialServerEvent(Player player, @Nullable RegisteredServer initialServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.initialServer = initialServer;
  }

  public Player getPlayer() {
    return player;
  }

  public Optional<RegisteredServer> getInitialServer() {
    return Optional.ofNullable(initialServer);
  }

  /**
   * Sets the new initial server.
   *
   * @param server the initial server the player should connect to
   */
  public void setInitialServer(@Nullable RegisteredServer server) {
    this.initialServer = server;
  }

  @Override
  public String toString() {
    return "InitialServerEvent{" + "player=" + player + ", initialServer=" + initialServer + '}';
  }
}
