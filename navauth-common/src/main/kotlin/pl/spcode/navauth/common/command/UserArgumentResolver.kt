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

package pl.spcode.navauth.common.command

import com.google.inject.Inject
import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import java.util.UUID
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.shared.utils.UuidUtils

class UserArgumentResolver<SENDER>
@Inject
constructor(val userService: UserService, val usernameValidator: UsernameValidator) :
  ArgumentResolver<SENDER, User>() {

  override fun parse(
    invocation: Invocation<SENDER>,
    context: Argument<User>,
    argument: String,
  ): ParseResult<User> {

    val user =
      when (usernameValidator.isValid(argument)) {
        true -> userService.findUserByUsernameIgnoreCase(argument)
        false -> {
          val uuid: UUID
          try {
            uuid = UuidUtils.fromString(argument)
          } catch (e: Exception) {
            return ParseResult.failure("Invalid UUID format")
          }

          userService.findUserByUuid(UserUuid(uuid))
        }
      }

    return if (user != null) ParseResult.success(user) else ParseResult.failure("User not found")
  }

  override fun suggest(
    invocation: Invocation<SENDER>,
    argument: Argument<User>,
    context: SuggestionContext,
  ): SuggestionResult {
    return SuggestionResult.empty()
  }
}
