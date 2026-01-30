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
import eu.okaeri.configs.annotation.Header
import pl.spcode.navauth.common.infra.database.DatabaseConfig
import pl.spcode.navauth.common.migrate.MigrationOriginPluginType

@Header(
  "Database migration config",
  "This file contains database migration settings.",
  "Check https://navio1430.github.io/NavAuth/docs/migration/migration.html for more information",
)
class MigrationConfig : OkaeriConfig() {

  @Comment(
    "Chunk size for migration",
    "This is the number of records that will be migrated at once.",
    "Higher chunk size means more memory usage but faster migration.",
  )
  var chunkSize: Long = 200

  @Comment(
    "Source database connection config",
    "This is the database where the data will be migrated from.",
    "Target database is the one specified in general config.",
  )
  var sourceDatabaseConfig: DatabaseConfig = DatabaseConfig()

  @Comment("Origin plugin type. Supported types:", " - NAVAUTH", " - LIBRELOGIN")
  var originPluginType: MigrationOriginPluginType = MigrationOriginPluginType.LIBRELOGIN
}
