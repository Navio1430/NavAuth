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

package pl.spcode.navauth.common.application.validator

import com.google.inject.Inject
import pl.spcode.navauth.common.config.PasswordsConfig

class PasswordValidator @Inject constructor(val config: PasswordsConfig) {

  fun isValid(password: String): Boolean {
    // length check
    if (password.length < config.minLength) {
      return false
    }

    // uppercase check
    if (config.requireUppercase && !password.any { it.isUpperCase() }) {
      return false
    }

    // lowercase check
    if (config.requireLowercase && !password.any { it.isLowerCase() }) {
      return false
    }

    // digit check
    if (config.requireDigit && !password.any { it.isDigit() }) {
      return false
    }

    // special char check
    if (config.requireSpecial && !password.any { !it.isLetterOrDigit() }) {
      return false
    }

    // whitespace check (always enforced)
    if (password.any { it.isWhitespace() }) {
      return false
    }

    return true
  }
}
