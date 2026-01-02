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

import com.google.inject.Inject
import java.util.UUID
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.shared.utils.UuidUtils

class UserArgumentResolver
@Inject
constructor(val userService: UserService, val usernameValidator: UsernameValidator) {

  /**
   * Resolves a user based on the provided username or UUID.
   *
   * @param usernameOrUuid the username or UUID string to resolve the user for
   * @return the resolved User object
   * @throws UserResolveException.UsernameNotFound if the username is not found
   * @throws UserResolveException.UuidNotFound if the UUID is not found
   * @throws UserResolveException.InvalidUuid if the input string is not a valid UUID
   */
  fun resolve(usernameOrUuid: UsernameOrUuidRaw): User {
    val raw = usernameOrUuid.value
    return when (usernameValidator.isValid(raw)) {
      true -> {
        userService.findUserByUsernameIgnoreCase(raw)
          ?: throw UserResolveException.UsernameNotFound(raw)
      }
      false -> {
        val uuid: UUID
        try {
          uuid = UuidUtils.fromString(raw)
        } catch (e: Exception) {
          throw UserResolveException.InvalidUuid(raw)
        }

        userService.findUserByUuid(UserUuid(uuid)) ?: throw UserResolveException.UuidNotFound(raw)
      }
    }
  }
}
