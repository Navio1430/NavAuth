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

package pl.spcode.navauth.common.infra.persistence.ormlite.credentials

import com.google.inject.Inject
import com.j256.ormlite.dao.Dao
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.persistence.mapper.toDomain
import pl.spcode.navauth.common.infra.persistence.mapper.toRecord
import pl.spcode.navauth.common.shared.data.OrmLiteCrudRepositoryImpl
import java.util.UUID

class UserCredentialsRepositoryImpl @Inject constructor(databaseManager: DatabaseManager) :
  OrmLiteCrudRepositoryImpl<UserCredentialsRecord, UUID>(databaseManager, UserCredentialsRecord::class),
    UserCredentialsRepository {

  override fun save(userCredentials: UserCredentials): Dao.CreateOrUpdateStatus {
    return save(userCredentials.toRecord())
  }

  override fun findByUser(user: User): UserCredentials? {
    val query = queryBuilder().where().eq("uuid", user.id.value)
    return dao().queryForFirst(query.prepare())?.toDomain()
  }
}