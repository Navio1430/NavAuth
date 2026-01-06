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

package pl.spcode.navauth.common.migrate

import com.google.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.migrate.migrator.navauth.NavAuthDatabaseMigrator

class MigrationManager
@Inject
constructor(private val migrationConfig: MigrationConfig, val migratorFactory: MigratorFactory) {

  @Volatile
  var isMigrating: Boolean = false
    private set

  private val logger: Logger = LoggerFactory.getLogger(MigrationManager::class.java)

  companion object {
    const val MIGRATION_SOURCE_POOL_NAME = "NavAuth-migration-pool"
  }

  fun startMigration() {
    if (isMigrating) {
      throw IllegalStateException("Migration is already in progress")
    }

    isMigrating = true
    try {
      when (migrationConfig.originPluginType) {
        MigrationOriginPluginType.NAVAUTH -> startDatabaseMigration()
        else -> startPluginMigration()
      }
    } catch (e: Exception) {
      logger.error("Migration failed", e)
      throw e
    } finally {
      isMigrating = false
    }
  }

  fun startDatabaseMigration() {
    NavAuthDatabaseMigrator().migrateTables()
  }

  fun startPluginMigration() {
    val migrator = migratorFactory.getMigrator(migrationConfig.originPluginType)

    migrator.init()
    val total = migrator.getSourceUsersCount()
    logger.info("Starting migration of $total users")

    var offset = 0L
    while (true) {
      val migrated = migrator.migrateNext(offset, migrationConfig.chunkSize)
      if (migrated <= 0) break

      offset += migrated
      logger.info("Migrated $offset / $total users")
    }

    logger.info("Migration finished, migrated $offset users in total")
  }


}
