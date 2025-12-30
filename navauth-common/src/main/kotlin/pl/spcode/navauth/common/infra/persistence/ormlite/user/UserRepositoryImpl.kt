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

package pl.spcode.navauth.common.infra.persistence.ormlite.user

import com.google.inject.Inject
import com.j256.ormlite.dao.Dao
import java.util.UUID
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.persistence.mapper.toDomain
import pl.spcode.navauth.common.infra.persistence.mapper.toRecord
import pl.spcode.navauth.common.shared.data.OrmLiteCrudRepositoryImpl

class UserRepositoryImpl @Inject constructor(databaseManager: DatabaseManager) :
  OrmLiteCrudRepositoryImpl<UserRecord, UUID>(databaseManager, UserRecord::class), UserRepository {

  override fun save(user: User): Dao.CreateOrUpdateStatus {
    return save(user.toRecord())
  }

  override fun findByExactUsername(username: String): User? {
    val query = queryBuilder().where().eq("username", username)
    return dao().queryForFirst(query.prepare())?.toDomain()
  }

  override fun findByUsernameIgnoreCase(username: String): User? {
    val query = queryBuilder().where().eq("username_lowercase", username.lowercase())
    return dao().queryForFirst(query.prepare())?.toDomain()
  }

  override fun findByMojangUuid(uuid: MojangId): User? {
    val query = queryBuilder().where().eq("mojang_uuid", uuid.value)
    return dao().queryForFirst(query.prepare())?.toDomain()
  }
}
