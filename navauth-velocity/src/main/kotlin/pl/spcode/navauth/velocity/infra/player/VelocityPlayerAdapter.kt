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

package pl.spcode.navauth.velocity.infra.player

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.domain.player.DisconnectReason
import pl.spcode.navauth.common.domain.player.PlayerAdapter

class VelocityPlayerAdapter(val velocityPlayer: Player) : PlayerAdapter {

  override fun disconnect(reason: DisconnectReason) {
    when (reason) {
      DisconnectReason.AUTH_SESSION_CLOSED ->
        velocityPlayer.disconnect(
          Component.text(
            "NavAuth: Session closed. This is a security measure to prevent users from staying on the server without an active session.",
            TextColors.RED,
          )
        )
      DisconnectReason.TOO_MANY_LOGIN_ATTEMPTS ->
        velocityPlayer.disconnect(
          Component.text("Too many login attempts. Please try again later.", TextColors.RED)
        )
    }
  }

  override fun isOnline(): Boolean {
    return velocityPlayer.isActive
  }
}
