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

package pl.spcode.navauth.common.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm

class PasswordsConfig : OkaeriConfig() {

  @Comment(
    "Hashing algorithm to use for passwords. Available algorithms:",
    " - ARGON2",
    " - BCRYPT",
  )
  var hashingAlgorithm: HashingAlgorithm = HashingAlgorithm.BCRYPT

  @Comment(
    "Argon2 settings — used only when hashingAlgorithm is set to ARGON2.",
    "Leave these unchanged unless you know what you're doing.",
    "Changing any of these properties can significantly increase CPU usage or reduce security.",
    "Default Argon version: Argon2ID",
    "Default salt bytes: 16",
  )
  var argon2: Argon2Config = Argon2Config()

  class Argon2Config : OkaeriConfig() {
    @Comment("Hash length in bytes", "default: 16") var hashLength: Int = 16

    @Comment("Memory cost in KB", "default: 65536 = 64 MiB") var memoryKb: Int = 65536

    @Comment("Time cost (iterations)", "default: 3") var iterations: Int = 3

    @Comment("Parallelism (threads)", "default: 4") var parallelism: Int = 4
  }

  @Comment("Minimum length") var minLength: Int = 5

  @Comment("Require uppercase letters?") var requireUppercase: Boolean = false

  @Comment("Require lowercase letters?") var requireLowercase: Boolean = true

  @Comment("Require digits?") var requireDigit: Boolean = true

  @Comment("Require special chars?", "e.g. !@#$%^&*") var requireSpecial: Boolean = false
}
