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
import java.util.Locale
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.config.CommandsConfig
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

class CommandsListeners
@Inject
constructor(
  val authSessionService: AuthSessionService<VelocityPlayerAdapter>,
  commandsConfig: CommandsConfig,
) {

  companion object {
    /** Original names of the commands a player who is not logged in may run. */
    val AUTH_COMMANDS = listOf("login", "register", "2fa")

    /**
     * The names and aliases the auth commands are registered under, lower case (Velocity matches
     * commands case-insensitively). `commands.yml` can rename them, replace their aliases or
     * disable them, exactly as `CommandConfigurer` applies it; a disabled command is not allowed.
     */
    fun allowedCommandNames(commandsConfig: CommandsConfig): Set<String> =
      AUTH_COMMANDS.flatMap { original ->
          val config = commandsConfig.commands[original] ?: return@flatMap listOf(original)
          if (!config.enabled) return@flatMap emptyList()
          val name = config.name?.takeIf { it.isNotEmpty() } ?: original
          listOf(name) + config.aliases.filter { it.isNotEmpty() }
        }
        .map { it.lowercase(Locale.ROOT) }
        .toSet()
  }

  /** Commands a player who is not logged in may run, see [allowedCommandNames]. */
  val whitelist: Set<String> = allowedCommandNames(commandsConfig)

  private fun isWhitelisted(command: String) = whitelist.contains(command.lowercase(Locale.ROOT))

  @Subscribe(order = PostOrder.FIRST)
  fun onCommand(event: CommandExecuteEvent) {
    if (event.commandSource !is Player) {
      return
    }

    val command = event.command.split(" ", ignoreCase = true, limit = 2).first()
    if (isWhitelisted(command)) {
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
      event.rootNode.children.removeIf { !isWhitelisted(it.name) }
    }
  }
}
