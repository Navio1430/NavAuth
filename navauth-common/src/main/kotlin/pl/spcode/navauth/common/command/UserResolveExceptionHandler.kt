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

package pl.spcode.navauth.common.command

import dev.rollczi.litecommands.handler.exception.ExceptionHandler
import dev.rollczi.litecommands.handler.result.ResultHandlerChain
import dev.rollczi.litecommands.invocation.Invocation
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.component.AudienceProvider

class UserResolveExceptionHandler<SENDER> constructor(val audienceProvider: AudienceProvider) :
  ExceptionHandler<SENDER, UserResolveException> {

  override fun handle(
    invocation: Invocation<SENDER>,
    exception: UserResolveException,
    chain: ResultHandlerChain<SENDER>,
  ) {
    val audience = audienceProvider.getAudience(invocation)
    when (exception) {
      is UserResolveException.InvalidUuid -> {
        audience.sendMessage(Component.text("UUID '${exception.uuid}' is not valid"))
      }
      is UserResolveException.UsernameNotFound -> {
        audience.sendMessage(Component.text("User '${exception.username}' not found"))
      }
      is UserResolveException.UuidNotFound -> {
        audience.sendMessage(Component.text("User with '${exception.uuid}' UUID not found"))
      }
    }
  }
}
