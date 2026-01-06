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

package pl.spcode.navauth.common.migrate.migrator.navauth

import com.google.inject.Inject
import com.j256.ormlite.dao.Dao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.database.EntitiesRegistrar
import pl.spcode.navauth.common.migrate.MigrationManager
import pl.spcode.navauth.common.migrate.error.SourceDatabaseConnectException
import pl.spcode.navauth.common.migrate.error.SourceTableNotFoundException
import pl.spcode.navauth.common.shared.PluginDirectory

class NavAuthDatabaseMigrator
@Inject
constructor(
  val migrationConfig: MigrationConfig,
  val databaseManager: DatabaseManager,
  pluginDirectory: PluginDirectory,
  entitiesRegistrar: EntitiesRegistrar,
) {

  val logger: Logger = LoggerFactory.getLogger(NavAuthDatabaseMigrator::class.java)

  // source, target
  val daoSourceTargetPairs = mutableListOf<Pair<Dao<*, *>, Dao<*, *>>>()

  val sourceDatabaseManager =
    DatabaseManager(migrationConfig.sourceDatabaseConfig, entitiesRegistrar, pluginDirectory)

  fun init() {
    try {
      sourceDatabaseManager.connectAndInit(MigrationManager.MIGRATION_SOURCE_POOL_NAME)
    } catch (e: Exception) {
      throw SourceDatabaseConnectException(e)
    }

    sourceDatabaseManager.entitiesRegistrar.getTypesWithIds().forEach {
      val sourceDao = sourceDatabaseManager.getDao(it.first, it.second)
      if (!sourceDao.isTableExists) {
        throw SourceTableNotFoundException(sourceDao.tableName)
      }

      val targetDao = databaseManager.getDao(it.first, it.second)

      daoSourceTargetPairs.add(Pair(sourceDao, targetDao))
    }
  }

  fun migrateAll() {
    for ((source, target) in daoSourceTargetPairs) {
      migrateDaoTable(source, target)
    }

    migrationConfig.sourceDatabaseConfig
  }

  private fun migrateDaoTable(sourceDao: Dao<*, *>, targetDao: Dao<*, *>) {
    logger.info("Migrating ${sourceDao.tableName} table...")

    val total = sourceDao.countOf()
    logger.info("Found $total records.")

    var offset = 0L
    while (true) {
      val migrated = migrateNext(sourceDao, targetDao, offset, migrationConfig.chunkSize)
      if (migrated <= 0) break

      offset += migrated
      logger.info("Migrated $offset / $total records of ${sourceDao.tableName} table.")
    }

    logger.info("Migration of ${sourceDao.tableName} table finished.")
  }

  private fun migrateNext(
    sourceDao: Dao<*, *>,
    targetDao: Dao<*, *>,
    offset: Long,
    limit: Long,
  ): Long {
    val sourceDao = sourceDao as Dao<Any, Any>
    val targetDao = targetDao as Dao<Any, Any>
    val qb = sourceDao.queryBuilder().offset(offset).limit(limit).prepare()
    val result = sourceDao.query(qb)
    targetDao.create(result)
    return result.size.toLong()
  }
}
