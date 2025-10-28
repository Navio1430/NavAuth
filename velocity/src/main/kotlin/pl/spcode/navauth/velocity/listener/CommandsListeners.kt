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

package pl.spcode.navauth.velocity.listener

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent
import com.velocitypowered.api.proxy.Player

class CommandsListeners {

  val whitelist = listOf("login", "register")

  @Subscribe(order = PostOrder.FIRST)
  fun onCommand(event: CommandExecuteEvent) {
    if (event.commandSource !is Player) {
      return
    }

    val player = event.commandSource as Player
    val authenticated = false // todo check if player is authenticated
    if (authenticated) {
      return
    }

    val command = event.command.split(" ", ignoreCase = true, limit = 2).first()

    // todo check if command is whitelisted
    val whitelisted = whitelist.contains(command)

    if (!whitelisted) {
      event.result = CommandExecuteEvent.CommandResult.denied()
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  fun onPlayerAvailableCommands(event: PlayerAvailableCommandsEvent) {
    event.rootNode.children.removeIf { !whitelist.contains(it.name) }
  }
}
