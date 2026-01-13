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

package pl.spcode.navauth.common.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.Variable
import java.time.Duration
import pl.spcode.navauth.common.infra.database.DatabaseConfig

open class GeneralConfig : OkaeriConfig() {

  @Comment("Database connection config")
  var databaseConfig: DatabaseConfig = DatabaseConfig()
    protected set

  @Comment(
    "The backend servers players should be sent to after successful authentication.",
    "Players are LoadBalanced with 'least conn' by default.",
    "If no servers are defined, then we won't do anything on the initial server event.",
  )
  var initialServers: List<String> = listOf("paper")
    protected set

  @Comment(
    "The limbo server players should be sent to while waiting for an authentication.",
    "Players are LoadBalanced with 'least conn' by default.",
    "There must be at least 1 properly registered server available.",
  )
  var limboServers: List<String> = listOf("limbo")
    protected set

  @Comment("Maximum number of login attempts before kicking the player.")
  var maxLoginAttempts: Int = 3

  @Comment("Maximum time of registration.")
  var maxRegistrationDuration: Duration = Duration.ofSeconds(30)

  @Comment("Maximum time of login.") var maxLoginDuration: Duration = Duration.ofSeconds(20)

  @Comment("Usernames config") var usernamesConfig: UsernamesConfig = UsernamesConfig()

  @Comment("Two-factor (2FA) TOTP authentication config")
  var twoFactorAuthConfig: TwoFactorAuthConfig = TwoFactorAuthConfig()

  @Comment("Passwords config") var passwordsConfig: PasswordsConfig = PasswordsConfig()

  @Variable("CONFIG_VERSION")
  @Comment("Config version. DO NOT CHANGE this property!")
  var configVersion: Int = 0
    protected set
}
