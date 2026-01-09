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

package pl.spcode.navauth.api.domain;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface AuthUser {

  /**
   * Retrieves the global UUID identifier associated with the user.
   *
   * @return the UUID of the user.
   */
  UUID getUUID();

  /**
   * Retrieves the Mojang UUID associated with the user, if available. NOTE: User can have a premium
   * username with non-premium status.
   *
   * @return the Mojang UUID of the user, or {@code null} if the user does not have one.
   */
  @Nullable
  UUID getMojangUUID();

  /**
   * Retrieves the username associated with the user.
   *
   * @return the username of the user.
   */
  String getUsername();

  /**
   * Checks whether the user is considered premium based on the presence of a Mojang UUID. NOTE:
   * User can have a premium username with non-premium status.
   *
   * @return {@code true} if the user has a Mojang UUID, indicating premium status; {@code false}
   *     otherwise.
   */
  default Boolean isPremium() {
    return this.getMojangUUID() != null;
  }
}
