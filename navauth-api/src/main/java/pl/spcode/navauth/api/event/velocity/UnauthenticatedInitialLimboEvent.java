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

public class UnauthenticatedInitialLimboEvent implements NavAuthEvent {

  private final Player player;
  private @Nullable RegisteredServer initialLimbo;

  /**
   * @param player the player that was connected
   * @param initialLimbo the initial limbo server selected by NavAuth, may be {@code null}
   */
  public UnauthenticatedInitialLimboEvent(Player player, @Nullable RegisteredServer initialLimbo) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.initialLimbo = initialLimbo;
  }

  public Player getPlayer() {
    return player;
  }

  public Optional<RegisteredServer> getInitialLimbo() {
    return Optional.ofNullable(initialLimbo);
  }

  /**
   * Sets the new initial limbo.
   *
   * @param server the initial limbo the player should connect to handle the authentication
   */
  public void setInitialLimbo(@Nullable RegisteredServer server) {
    this.initialLimbo = server;
  }

  @Override
  public String toString() {
    return "UnauthenticatedInitialLimboEvent{"
        + "player="
        + player
        + ", initialServer="
        + initialLimbo
        + '}';
  }
}
