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

package pl.spcode.navauth.common.infra.repository

import com.google.inject.Inject
import java.util.UUID
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.shared.data.OrmLiteCrudRepositoryImpl

class UserRepositoryImpl @Inject constructor(databaseManager: DatabaseManager) :
  OrmLiteCrudRepositoryImpl<User, UUID>(databaseManager, User::class), UserRepository {

  override fun findByUsername(username: String): User? {
    val query = queryBuilder().where().eq("username", username)
    return dao().queryForFirst(query.prepare())
  }
}
