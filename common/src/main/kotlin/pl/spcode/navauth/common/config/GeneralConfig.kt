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
import pl.spcode.navauth.common.domain.server.LoadBalancerType
import pl.spcode.navauth.common.infra.database.DatabaseConfig

open class GeneralConfig : OkaeriConfig() {

  @Comment("Database connection config")
  var databaseConfig: DatabaseConfig = DatabaseConfig()
    protected set

  @Comment("The limbo servers, players should be sent to, while waiting for an authentication")
  var limboServers: List<String> = listOf("limbo")
    protected set

  @Comment(
    "Type of LoadBalancer to use for picking an available limbo server.",
    "Available types:",
    " - LEAST_CONN (picks the server with the least number of players)",
    " - ROUND_ROBIN (just round-robin)",
  )
  var limboLoadBalancer: LoadBalancerType = LoadBalancerType.LEAST_CONN
    protected set

  @Variable("CONFIG_VERSION")
  @Comment("Config version. DO NOT CHANGE this property!")
  var configVersion: Int = 0
    protected set
}
