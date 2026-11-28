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
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.BatchUpsertStatement
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.persistence.exposed.CredentialsTable
import pl.spcode.navauth.common.infra.persistence.exposed.UsersTable
import pl.spcode.navauth.common.infra.persistence.exposed.isMysqlFamily
import pl.spcode.navauth.common.infra.persistence.impl.credentials.UserCredentialsRepositoryImpl
import pl.spcode.navauth.common.infra.persistence.impl.user.UserRepositoryImpl
import pl.spcode.navauth.common.infra.persistence.mapper.toUser
import pl.spcode.navauth.common.infra.persistence.mapper.toUserCredentials
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
) {

  val logger: Logger = LoggerFactory.getLogger(NavAuthDatabaseMigrator::class.java)

  val sourceDatabaseManager = DatabaseManager(migrationConfig.sourceDatabaseConfig, pluginDirectory)

  private lateinit var sourceDatabase: Database

  private val targetDatabase: Database
    get() = databaseManager.database

  // H2 runs a batch upsert as MERGE ... USING (VALUES (?, ...)) and types the VALUES columns from
  // the first row of the batch: a NULL there (a cracked user's Mojang UUID, a 2FA-only password)
  // turns that column into NULL for every later row of the batch. H2 targets therefore upsert
  // row by row, like the repositories do.
  private val targetUsers by lazy { UserRepositoryImpl(targetDatabase) }
  private val targetCredentials by lazy { UserCredentialsRepositoryImpl(targetDatabase) }

  private fun Database.isH2(): Boolean = dialect is H2Dialect

  fun init() {
    try {
      sourceDatabaseManager.connect(MigrationManager.MIGRATION_SOURCE_POOL_NAME)
    } catch (e: Exception) {
      throw SourceDatabaseConnectException(e)
    }

    sourceDatabase = DatabaseManager.connectExposed(sourceDatabaseManager.dataSource)

    verifyTableExists(UsersTable)
    verifyTableExists(CredentialsTable)
  }

  fun migrateAll() {
    migrateTable("navauth_users", { countRows(UsersTable) }) { offset, limit ->
      copyUsers(offset, limit)
    }
    migrateTable("navauth_credentials", { countRows(CredentialsTable) }) { offset, limit ->
      copyCredentials(offset, limit)
    }
  }

  private fun countRows(table: Table): Long =
    transaction(sourceDatabase) { table.selectAll().count() }

  private fun verifyTableExists(table: Table) {
    try {
      transaction(sourceDatabase) { table.selectAll().limit(1).toList() }
    } catch (e: Exception) {
      throw SourceTableNotFoundException(table.tableName)
    }
  }

  private fun migrateTable(
    label: String,
    sourceCount: () -> Long,
    copyChunk: (offset: Long, limit: Long) -> Long,
  ) {
    logger.info("Migrating $label table...")
    val total = sourceCount()
    logger.info("Found $total records.")

    var offset = 0L
    while (true) {
      val migrated = copyChunk(offset, migrationConfig.chunkSize)
      if (migrated <= 0) break

      offset += migrated
      logger.info("Migrated $offset / $total records of $label table.")
    }

    logger.info("Migration of $label table finished.")
  }

  private fun copyUsers(offset: Long, limit: Long): Long {
    val users =
      transaction(sourceDatabase) {
        UsersTable.selectAll()
          .orderBy(UsersTable.uuid, SortOrder.ASC)
          .limit(limit.toInt())
          .offset(offset)
          .map { it.toUser() }
      }
    if (users.isEmpty()) return 0

    transaction(targetDatabase) {
      val setValues: BatchUpsertStatement.(User) -> Unit = { user ->
        this[UsersTable.uuid] = user.uuid.value
        this[UsersTable.mojangUuid] = user.mojangUuid?.value
        this[UsersTable.username] = user.username.value
        this[UsersTable.usernameLowercase] = user.username.value.lowercase()
        this[UsersTable.credentialsRequired] = user.credentialsRequired
      }

      when {
        targetDatabase.isH2() -> users.forEach { targetUsers.save(it) }
        targetDatabase.isMysqlFamily() -> UsersTable.batchUpsert(users, body = setValues)
        else -> UsersTable.batchUpsert(users, UsersTable.uuid, body = setValues)
      }
    }

    return users.size.toLong()
  }

  private fun copyCredentials(offset: Long, limit: Long): Long {
    val credentials =
      transaction(sourceDatabase) {
        CredentialsTable.selectAll()
          .orderBy(CredentialsTable.uuid, SortOrder.ASC)
          .limit(limit.toInt())
          .offset(offset)
          .map { it.toUserCredentials() }
      }
    if (credentials.isEmpty()) return 0

    transaction(targetDatabase) {
      val setValues: BatchUpsertStatement.(UserCredentials) -> Unit = { credentialsRow ->
        this[CredentialsTable.uuid] = credentialsRow.userUuid.value
        this[CredentialsTable.passwordHash] = credentialsRow.hashedPassword?.passwordHash?.value
        this[CredentialsTable.algo] = credentialsRow.hashedPassword?.algo
        this[CredentialsTable.twoFactorSecret] = credentialsRow.totpSecret?.value
      }

      when {
        targetDatabase.isH2() -> credentials.forEach { targetCredentials.save(it) }
        targetDatabase.isMysqlFamily() ->
          CredentialsTable.batchUpsert(credentials, body = setValues)
        else -> CredentialsTable.batchUpsert(credentials, CredentialsTable.uuid, body = setValues)
      }
    }

    return credentials.size.toLong()
  }
}
