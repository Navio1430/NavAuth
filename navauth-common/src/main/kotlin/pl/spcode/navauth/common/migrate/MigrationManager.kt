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

class MigrationManager
@Inject
constructor(private val migrationConfig: MigrationConfig,
    val migratorFactory: MigratorFactory) {

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

    val migrator = migratorFactory.getMigrator(migrationConfig.originPluginType)

    isMigrating = true
    try {
      migrator.init()

      val total = migrator.getSourceRecordsCount()
      logger.info("Starting migration of $total records")

      var offset = 0L
      while (true) {
        val migrated = migrator.migrateNext(offset, migrationConfig.chunkSize)
        if (migrated <= 0) break

        offset += migrated
        logger.info("Migrated $offset / $total records")
      }

      logger.info("Migration finished, migrated $offset records in total")
    } catch (e: Exception) {
      logger.error("Migration failed", e)
    } finally {
      isMigrating = false
    }
  }

}