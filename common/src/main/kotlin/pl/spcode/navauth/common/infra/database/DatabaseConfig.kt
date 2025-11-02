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

package pl.spcode.navauth.common.infra.database

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment

open class DatabaseConfig : OkaeriConfig() {

  @Comment(
    "Database driver type. Available drivers:" +
      " - H2_MEM (H2 memory based database, ONLY FOR TESTING)"
  )
  var driverType: DatabaseDriverType = DatabaseDriverType.H2_MEM
    protected set

  @Comment("Connection pool size")
  var poolSize: Int = 5
    protected set

  @Comment(
    "Connection timeout in milliseconds.",
    "This is the maximum time to wait for a connection from the pool.",
  )
  var connectionTimeout: Long = 30000
    protected set

  @Comment("Database username that should be used.")
  var username: String = "root"
    protected set

  @Comment("Password to authenticate with provided username.")
  var password: String = "password"
    protected set

  @Comment("Hostname (ip/domain) of the database.", "Not applicable for H2_MEM.")
  var hostname: String = "localhost"
    protected set

  @Comment(
    "Port number of the database server. Common ports:",
    " - H2_MEM: Not applicable (memory based)",
  )
  var port: Int = 0
    protected set

  @Comment("Database name")
  var database: String = "default"
    protected set
}
