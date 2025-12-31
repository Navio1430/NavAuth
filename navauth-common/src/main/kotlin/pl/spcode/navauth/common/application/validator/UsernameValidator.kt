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

package pl.spcode.navauth.common.application.validator

import com.google.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.config.UsernamesConfig
import pl.spcode.navauth.common.domain.user.Username

class UsernameValidator @Inject constructor(usernamesConfig: UsernamesConfig) {

  private val logger: Logger = LoggerFactory.getLogger(UsernameValidator::class.java)
  private var usernameRegex: Regex

  companion object {
    val FALLBACK_REGEX: Regex = Regex("^[a-zA-Z0-9_]{3,16}$")
  }

  init {
    try {
      usernameRegex = Regex(usernamesConfig.usernamePattern)
    } catch (e: Exception) {
      logger.warn(
        "Invalid username pattern: ${usernamesConfig.usernamePattern}, using default pattern as fallback."
      )
      usernameRegex = FALLBACK_REGEX
    }
  }

  fun validate(username: String): Username {
    usernameRegex.matches(username).takeIf { it }
      ?: throw ValidationException("Invalid username: $username")
    return Username(username)
  }

  fun isValid(username: String): Boolean {
    return usernameRegex.matches(username)
  }
}
