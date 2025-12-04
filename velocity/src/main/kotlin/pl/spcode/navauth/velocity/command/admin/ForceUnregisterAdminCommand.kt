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

package pl.spcode.navauth.velocity.command.admin

import com.google.inject.Inject
import com.velocitypowered.api.proxy.Player
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.component.TextColors

@Command(name = "forceunregister")
@Permission(Permissions.ADMIN_FORCE_UNREGISTER)
class ForceUnregisterAdminCommand
@Inject
constructor(val userService: UserService, val userCredentialsService: UserCredentialsService) {

  @Async
  @Execute
  fun forceUnregister(@Context sender: Player, @Arg(value = "playerName") playerName: String) {
    val user = userService.findUserByUsernameLowercase(playerName.lowercase())

    if (user == null) {
      sender.sendMessage(Component.text("User '${playerName}' not found.", TextColors.RED))
      return
    }

    if (user.isPremium) {
      sender.sendMessage(
        Component.text(
          "Can't execute the command! Account '${user.username}' is set to premium mode.",
          TextColors.RED,
        )
      )
      return
    }

    val userCredentials = userCredentialsService.findCredentials(user)
    if (userCredentials == null) {
      sender.sendMessage(Component.text("User is already unregistered.", TextColors.RED))
      return
    }

    userCredentialsService.deleteUserCredentials(user)
    sender.sendMessage(
      Component.text("Success! User '${user.username}' credentials deleted.", TextColors.GREEN)
    )
  }
}
