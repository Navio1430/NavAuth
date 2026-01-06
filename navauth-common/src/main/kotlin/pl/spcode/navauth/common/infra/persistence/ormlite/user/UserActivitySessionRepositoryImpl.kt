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

package pl.spcode.navauth.common.infra.persistence.ormlite.user

import com.google.inject.Inject
import java.util.UUID
import pl.spcode.navauth.common.domain.user.UserActivitySession
import pl.spcode.navauth.common.domain.user.UserActivitySessionRepository
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.persistence.mapper.toRecord
import pl.spcode.navauth.common.shared.data.OrmLiteCrudRepositoryImpl

class UserActivitySessionRepositoryImpl @Inject constructor(databaseManager: DatabaseManager) :
  OrmLiteCrudRepositoryImpl<UserActivitySessionRecord, UUID>(
    databaseManager,
    UserActivitySessionRecord::class,
  ),
  UserActivitySessionRepository {

  override fun save(session: UserActivitySession) {
    dao().create(session.toRecord())
  }
}
