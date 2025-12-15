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

package pl.spcode.navauth.velocity.command.user

import com.google.inject.Inject
import com.velocitypowered.api.proxy.Player
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.component.TextColors

@Command(name = "unregister")
@Permission(Permissions.USER_UNREGISTER)
class UnregisterCommand
@Inject
constructor(val userService: UserService, val userCredentialsService: UserCredentialsService) {

  @Async
  @Execute
  @Description("Unregister your account.", "**NOTE**: someone will be able to acquire and register the account after.")
  fun unregister(
    @Context sender: Player,
    @Arg(value = "current_password") currentPassword: String,
  ) {
    val user = userService.findUserByUsername(sender.username)!!
    if (user.isPremium) {
      sender.sendMessage(
        Component.text(
          "Can't execute this command right now: your account is set to premium mode.",
          TextColors.RED,
        )
      )
      return
    }

    val credentials = userCredentialsService.findCredentials(user)!!
    val isCorrectPassword = userCredentialsService.verifyPassword(credentials, currentPassword)
    if (!isCorrectPassword) {
      sender.sendMessage(Component.text("Wrong password!", TextColors.RED))
      return
    }

    userCredentialsService.deleteUserCredentials(user)

    sender.sendMessage(Component.text("Success! Credentials deleted.", TextColors.GREEN))
  }
}
