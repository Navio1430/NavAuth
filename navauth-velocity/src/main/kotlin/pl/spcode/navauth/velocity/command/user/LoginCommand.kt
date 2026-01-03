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
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.domain.auth.session.AuthSessionType
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.infra.auth.VelocityLoginAuthSession
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

// we use inverted permission in this command
@Command(name = "login")
class LoginCommand
@Inject
constructor(val authSessionService: AuthSessionService<VelocityPlayerAdapter>) {

  @Async
  @Execute
  @Description("Manual login command for non-premium players")
  fun login(
    @Context sender: Player,
    @Arg(value = "password") password: String,
    @Arg(value = "2fa") twoFactorCode: Optional<String>,
  ) {
    // if permission is set explicitly to FALSE
    if (sender.getPermissionValue(Permissions.USER_LOGIN) == Tristate.FALSE) {
      // todo unify missing permission handler
      sender.sendMessage(
        Component.text("You don't have permission to use this command.", TextColors.RED)
      )
      return
    }

    val uniqueSessionId = VelocityUniqueSessionId(sender)
    val session = authSessionService.findSession(uniqueSessionId)
    if (session?.getSessionType() != AuthSessionType.LOGIN || session.isAuthenticated) {
      sender.sendMessage(Component.text("Can't use this command right now.", TextColors.RED))
      return
    }

    session as VelocityLoginAuthSession

    if (session.userCredentials.isTwoFactorEnabled) {
      if (!twoFactorCode.isPresent) {
        sender.sendMessage(
          Component.text("Please provide two-factor authentication code.", TextColors.RED)
        )
        return
      }
    }

    if (session.auth(password, twoFactorCode.getOrNull())) {
      sender.sendMessage(Component.text("Logged in!", TextColors.GREEN))
      return
    }

    sender.sendMessage(Component.text("Wrong credentials!", TextColors.RED))
  }
}
