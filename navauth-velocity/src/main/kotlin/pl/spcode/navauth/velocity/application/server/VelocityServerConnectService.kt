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

package pl.spcode.navauth.velocity.application.server

import com.google.inject.Inject
import com.google.inject.Singleton
import com.velocitypowered.api.proxy.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Singleton
class VelocityServerConnectService
@Inject
constructor(val serverSelectionService: VelocityServerSelectionService) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(VelocityServerConnectService::class.java)
  }

  /** Sends the player directly to the initial server selected for an authenticated user. */
  fun sendPlayerToInitialServer(player: Player) {
    val initialServer = serverSelectionService.getInitialServer(player)
    if (initialServer == null) {
      logger.warn(
        "sendPlayerToInitialServer: no initial server found for user '{}'",
        player.username,
      )
      return
    }

    logger.debug(
      "sendPlayerToInitialServer: sending user '{}' to initial server '{}'",
      player.username,
      initialServer.serverInfo.name,
    )
    player.createConnectionRequest(initialServer).connect()
  }
}
