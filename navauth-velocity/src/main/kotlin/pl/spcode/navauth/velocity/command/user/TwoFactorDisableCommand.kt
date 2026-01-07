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

package pl.spcode.navauth.velocity.command.user

import com.google.inject.Inject
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.RootCommand
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.exception.MissingPermissionException
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.velocity.command.Permissions

// we use inverted permission in this command
@RootCommand
class TwoFactorDisableCommand
@Inject
constructor(val userService: UserService, val userCredentialsService: UserCredentialsService) {

  @Async
  @Execute(name = "disable2fa")
  // todo description
  fun disableTwoFactor(@Context sender: Player, @Arg(value = "2fa_code") code: String) {
    // if permission is set explicitly to FALSE
    if (sender.getPermissionValue(Permissions.USER_TWO_FACTOR_DISABLE) == Tristate.FALSE) {
      throw MissingPermissionException(Permissions.USER_TWO_FACTOR_DISABLE)
    }

    val user = userService.findUserByUuid(UserUuid(sender.uniqueId))!!
    val credentials = userCredentialsService.findCredentials(user)
    if (credentials == null || !credentials.isTwoFactorEnabled) {
      sender.sendMessage(Component.text("Your account has 2FA disabled already.", TextColors.RED))
      return
    }

    if (!userCredentialsService.verifyCode(credentials, code)) {
      sender.sendMessage(Component.text("Wrong code!", TextColors.RED))
      return
    }

    userService.disableTwoFactorAuth(user)
    sender.sendMessage(Component.text("2FA disabled!", TextColors.GREEN))
    return
  }
}
