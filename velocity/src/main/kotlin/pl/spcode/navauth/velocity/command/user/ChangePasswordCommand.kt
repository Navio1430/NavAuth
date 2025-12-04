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
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.infra.crypto.BCryptCredentialsHasher
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.component.TextColors

@Command(name = "changepassword")
@Permission(Permissions.USER_CHANGE_PASSWORD)
class ChangePasswordCommand
@Inject
constructor(val userService: UserService, val userCredentialsService: UserCredentialsService) {

  @Async
  @Execute
  fun changePassword(
    @Context sender: Player,
    @Arg(value = "current_password") currentPassword: String,
    @Arg(value = "new_password") newPassword: String,
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

    val newCredentials = UserCredentials.create(user, BCryptCredentialsHasher().hash(newPassword))
    userCredentialsService.storeUserCredentials(newCredentials)

    sender.sendMessage(Component.text("Success! New password set.", TextColors.GREEN))
  }
}
