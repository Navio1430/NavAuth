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
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.domain.auth.session.AuthSessionType
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.auth.VelocityLoginAuthSession
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId

@Command(name = "login")
class LoginCommand @Inject constructor(val authSessionService: AuthSessionService) {

  @Execute
  fun login(@Context sender: Player, @Arg(value = "password") password: String) {
    val uniqueSessionId = VelocityUniqueSessionId(sender)
    val session = authSessionService.findSession(uniqueSessionId)
    if (session?.getSessionType() != AuthSessionType.LOGIN) {
      sender.sendMessage(Component.text("Can't use this command right now.", TextColors.RED))
      return
    }

    session as VelocityLoginAuthSession

    if (session.authWithPassword(password)) {
      sender.sendMessage(Component.text("Logged in!", TextColors.GREEN))
      return
    }

    sender.sendMessage(Component.text("Wrong password!", TextColors.RED))
  }
}
