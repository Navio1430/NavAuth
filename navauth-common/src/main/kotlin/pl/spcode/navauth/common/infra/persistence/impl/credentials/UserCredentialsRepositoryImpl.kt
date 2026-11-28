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

package pl.spcode.navauth.common.infra.persistence.impl.credentials

import com.google.inject.Inject
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.persistence.exposed.CredentialsTable
import pl.spcode.navauth.common.infra.persistence.exposed.isMysqlFamily
import pl.spcode.navauth.common.infra.persistence.mapper.toUserCredentials

class UserCredentialsRepositoryImpl @Inject constructor(private val database: Database) :
  UserCredentialsRepository {

  override fun save(userCredentials: UserCredentials) {
    transaction(database) {
      val setValues: CredentialsTable.(UpsertStatement<Long>) -> Unit = { stmt ->
        stmt[CredentialsTable.uuid] = userCredentials.userUuid.value
        stmt[CredentialsTable.passwordHash] = userCredentials.hashedPassword?.passwordHash?.value
        stmt[CredentialsTable.algo] = userCredentials.hashedPassword?.algo
        stmt[CredentialsTable.twoFactorSecret] = userCredentials.totpSecret?.value
      }

      if (database.isMysqlFamily()) {
        CredentialsTable.upsert(body = setValues)
      } else {
        CredentialsTable.upsert(CredentialsTable.uuid, body = setValues)
      }
    }
  }

  override fun insert(userCredentials: UserCredentials) {
    transaction(database) {
      CredentialsTable.insert { stmt ->
        stmt[CredentialsTable.uuid] = userCredentials.userUuid.value
        stmt[CredentialsTable.passwordHash] = userCredentials.hashedPassword?.passwordHash?.value
        stmt[CredentialsTable.algo] = userCredentials.hashedPassword?.algo
        stmt[CredentialsTable.twoFactorSecret] = userCredentials.totpSecret?.value
      }
    }
  }

  override fun findByUser(user: User): UserCredentials? =
    transaction(database) {
      CredentialsTable.selectAll()
        .where { CredentialsTable.uuid eq user.uuid.value }
        .map { it.toUserCredentials() }
        .singleOrNull()
    }

  override fun deleteByUser(user: User) {
    transaction(database) {
      CredentialsTable.deleteWhere { CredentialsTable.uuid eq user.uuid.value }
    }
  }
}
