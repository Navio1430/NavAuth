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
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.auth.session.AuthSessionType
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.crypto.BCryptCredentialsHasher
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

@Command(name = "register")
class RegisterCommand
@Inject
constructor(
  val authSessionService: AuthSessionService<VelocityPlayerAdapter>,
  val userService: UserService,
) {

  @Async
  @Execute
  fun register(
    @Context sender: Player,
    @Arg(value = "password") password: String,
    @Arg(value = "repeat_password") repeatPassword: String,
  ) {
    val session = authSessionService.findSession(VelocityUniqueSessionId(sender))
    if (session?.getSessionType() != AuthSessionType.REGISTER) {
      sender.sendMessage(Component.text("Can't use this command right now.", TextColors.RED))
      return
    }

    if (password != repeatPassword) {
      // todo send error message
      return
    }

    userService.storeUserWithCredentials(
      User.create(sender.uniqueId, sender.username, false),
      BCryptCredentialsHasher().hash(password),
    )
    session.authenticate()

    sender.sendMessage(Component.text("Account created", TextColor.color(0, 200, 0)))
    // todo move player to lobby
  }
}
