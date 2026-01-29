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
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.multification.VelocityMultification

@Command(name = "changepassword")
@Permission(Permissions.USER_CHANGE_PASSWORD)
class ChangePasswordCommand
@Inject
constructor(
  val userService: UserService,
  val userCredentialsService: UserCredentialsService,
  val multification: VelocityMultification,
) {

  @Async
  @Execute
  @Description("Changes account password to new one. Requires current password.")
  fun changePassword(
    @Context sender: Player,
    @Arg(value = "current_password") currentPassword: String,
    @Arg(value = "new_password") newPassword: String,
  ) {
    val user = userService.findUserByExactUsername(sender.username)!!
    val credentials = userCredentialsService.findCredentials(user)!!

    if (!credentials.isPasswordRequired) {
      multification.send(sender) { it.multification.commandPasswordNotSetForAccountError }
      return
    }

    val isCorrectPassword = userCredentialsService.verifyPassword(credentials, currentPassword)
    if (!isCorrectPassword) {
      multification.send(sender) { it.multification.wrongCredentialsError }
      return
    }

    userCredentialsService.updatePassword(user, newPassword)
    multification.send(sender) { it.multification.newPasswordSetSuccess }
  }
}
