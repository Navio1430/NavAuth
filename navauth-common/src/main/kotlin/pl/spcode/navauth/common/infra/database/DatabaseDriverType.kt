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

enum class DatabaseDriverType(
  val driverClassName: String,
  val jdbcUrlFormat: String,
  val migrationDir: String,
) {

  // case-insensitive identifiers: OrmLite created H2 names upper case, V001/Exposed quote lower
  // case

  /** in-memory database, jdbc:h2:mem:database_name */
  H2_MEM("org.h2.Driver", "jdbc:h2:mem:%s;CASE_INSENSITIVE_IDENTIFIERS=TRUE", "h2"),

  /** file-based database, jdbc:h2:database_path */
  H2_FILE("org.h2.Driver", "jdbc:h2:%s;CASE_INSENSITIVE_IDENTIFIERS=TRUE", "h2"),

  /** works for both mysql and mariadb */
  MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%s/%s?sslMode=%s", "mysql"),
  MARIADB(MYSQL.driverClassName, MYSQL.jdbcUrlFormat, "mariadb"),
  SQLITE("org.sqlite.JDBC", "jdbc:sqlite:%s", "sqlite"),
  POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://%s:%s/%s?sslmode=%s", "postgresql");

  companion object {
    fun sslParamForMySQL(enabled: Boolean): String {
      return if (enabled) "REQUIRED" else "DISABLED"
    }

    fun sslParamForPostgreSQL(enabled: Boolean): String {
      return if (enabled) "require" else "disable"
    }
  }
}
