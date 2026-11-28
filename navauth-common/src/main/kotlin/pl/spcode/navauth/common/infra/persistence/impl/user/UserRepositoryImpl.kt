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

package pl.spcode.navauth.common.infra.persistence.impl.user

import com.google.inject.Inject
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.persistence.exposed.UsersTable
import pl.spcode.navauth.common.infra.persistence.exposed.isMysqlFamily
import pl.spcode.navauth.common.infra.persistence.mapper.toUser

class UserRepositoryImpl @Inject constructor(private val database: Database) : UserRepository {

  private val logger: Logger = LoggerFactory.getLogger(UserRepositoryImpl::class.java)

  override fun save(user: User) {
    transaction(database) {
      val setValues: UsersTable.(UpsertStatement<Long>) -> Unit = { stmt ->
        stmt[UsersTable.uuid] = user.uuid.value
        stmt[UsersTable.mojangUuid] = user.mojangUuid?.value
        stmt[UsersTable.username] = user.username.value
        stmt[UsersTable.usernameLowercase] = user.username.value.lowercase()
        stmt[UsersTable.credentialsRequired] = user.credentialsRequired
      }

      if (database.isMysqlFamily()) {
        UsersTable.upsert(body = setValues)
      } else {
        UsersTable.upsert(UsersTable.uuid, body = setValues)
      }
    }
  }

  override fun insert(user: User) {
    transaction(database) {
      UsersTable.insert { stmt ->
        stmt[UsersTable.uuid] = user.uuid.value
        stmt[UsersTable.mojangUuid] = user.mojangUuid?.value
        stmt[UsersTable.username] = user.username.value
        stmt[UsersTable.usernameLowercase] = user.username.value.lowercase()
        stmt[UsersTable.credentialsRequired] = user.credentialsRequired
      }
    }
  }

  /**
   * MySQL/MariaDB compare strings with the column's collation, which is case-insensitive by
   * default, so `username = ?` alone would also match other letter cases there. The exact
   * comparison is done here to behave the same on every database.
   */
  override fun findByExactUsername(username: String): User? =
    transaction(database) {
      val sameName =
        UsersTable.selectAll()
          .where { UsersTable.usernameLowercase eq username.lowercase() }
          .orderBy(UsersTable.uuid to SortOrder.ASC)
          .map { it.toUser() }
          .filter { it.username.value == username }
      if (sameName.size > 1) {
        logger.warn(
          "More than one NavAuth user is named '{}' ({}). Using {}. " +
            "Remove or rename the duplicate rows in navauth_users.",
          username,
          sameName.map { it.uuid },
          sameName.first().uuid,
        )
      }
      sameName.firstOrNull()
    }

  override fun findByUsernameIgnoreCase(username: String): User? =
    transaction(database) {
      UsersTable.selectAll()
        .where { UsersTable.usernameLowercase eq username.lowercase() }
        .firstUser("username (ignore case)", username)
    }

  override fun findByMojangUuid(uuid: MojangId): User? =
    transaction(database) {
      UsersTable.selectAll()
        .where { UsersTable.mojangUuid eq uuid.value }
        .firstUser("mojang uuid", uuid)
    }

  override fun findByUserUuid(uuid: UserUuid): User? =
    transaction(database) {
      UsersTable.selectAll().where { UsersTable.uuid eq uuid.value }.firstUser("uuid", uuid)
    }

  /**
   * No unique constraint has ever existed on `username_lowercase` or `mojang_uuid`, so a lookup can
   * match several rows. Like OrmLite's `queryForFirst`, the first row wins; ordering by `uuid`
   * makes that choice the same on every database. Duplicates are logged for the admin to resolve.
   */
  private fun Query.firstUser(lookupKind: String, lookupValue: Any): User? {
    val rows = orderBy(UsersTable.uuid to SortOrder.ASC).limit(2).map { it.toUser() }
    if (rows.size > 1) {
      logger.warn(
        "More than one NavAuth user matches {} '{}' (e.g. {} and {}). Using {}. " +
          "Remove or rename the duplicate rows in navauth_users.",
        lookupKind,
        lookupValue,
        rows[0].uuid,
        rows[1].uuid,
        rows[0].uuid,
      )
    }
    return rows.firstOrNull()
  }
}
