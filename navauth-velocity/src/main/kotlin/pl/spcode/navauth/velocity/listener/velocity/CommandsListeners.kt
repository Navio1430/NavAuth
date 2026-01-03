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

package pl.spcode.navauth.velocity.listener.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent
import com.velocitypowered.api.proxy.Player
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

class CommandsListeners
@Inject
constructor(val authSessionService: AuthSessionService<VelocityPlayerAdapter>) {

  val whitelist = listOf("login", "register", "2fa")

  @Subscribe(order = PostOrder.FIRST)
  fun onCommand(event: CommandExecuteEvent) {
    if (event.commandSource !is Player) {
      return
    }

    val command = event.command.split(" ", ignoreCase = true, limit = 2).first()
    val whitelisted = whitelist.contains(command)
    if (whitelisted) {
      event.result = CommandExecuteEvent.CommandResult.allowed()
      return
    }

    val player = event.commandSource as Player
    val sessionId = VelocityUniqueSessionId(player)
    val session = authSessionService.findSession(sessionId)
    if (session == null || !session.isAuthenticated) {
      event.result = CommandExecuteEvent.CommandResult.denied()
      return
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  fun onPlayerAvailableCommands(event: PlayerAvailableCommandsEvent) {
    val sessionId = VelocityUniqueSessionId(event.player)
    val session = authSessionService.findSession(sessionId)
    if (session == null) {
      event.rootNode.children.clear()
    } else if (!session.isAuthenticated) {
      event.rootNode.children.removeIf { !whitelist.contains(it.name) }
    }
  }
}
