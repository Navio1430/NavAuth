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
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.exception.MissingPermissionException
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.domain.auth.session.AuthSessionType
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.hasher.BCryptCredentialsHasher
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

// we use inverted permission in this command
@Command(name = "register")
class RegisterCommand
@Inject
constructor(
  val authSessionService: AuthSessionService<VelocityPlayerAdapter>,
  val userService: UserService,
) {

  @Async
  @Execute
  @Description(
    "Creates a new account with specified password.",
    "Applicable only for non-premium players.",
    "If you want to disable this command for specific group, then set ",
    "their `${Permissions.USER_REGISTER}` permission value to **FALSE**.",
  )
  fun register(
    @Context sender: Player,
    @Arg(value = "password") password: String,
    @Arg(value = "repeat_password") repeatPassword: String,
  ) {
    // if permission is set explicitly to FALSE
    if (sender.getPermissionValue(Permissions.USER_REGISTER) == Tristate.FALSE) {
      throw MissingPermissionException(Permissions.USER_REGISTER)
    }

    val session = authSessionService.findSession(VelocityUniqueSessionId(sender))
    if (session?.getSessionType() != AuthSessionType.REGISTER || session.isAuthenticated) {
      sender.sendMessage(Component.text("Can't use this command right now.", TextColors.RED))
      return
    }

    if (password != repeatPassword) {
      sender.sendMessage(Component.text("Both passwords must match.", TextColors.RED))
      return
    }

    userService.createAndStoreUserWithNewCredentials(
      User.nonPremium(UserUuid(sender.uniqueId), Username(sender.username)),
      BCryptCredentialsHasher().hash(password),
    )
    session.authenticate()

    sender.sendMessage(Component.text("Account created", TextColor.color(0, 200, 0)))
  }
}
