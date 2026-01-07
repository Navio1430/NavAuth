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

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.QueryBuilder
import java.util.UUID
import pl.spcode.navauth.common.domain.user.UserActivitySession
import pl.spcode.navauth.common.infra.persistence.OrmLitePaginatorImpl
import pl.spcode.navauth.common.infra.persistence.Paginator
import pl.spcode.navauth.common.infra.persistence.mapper.toDomain

class UserActivitySessionPaginator(
  dao: Dao<UserActivitySessionRecord, UUID>,
  pageSize: Long,
  where: ((QueryBuilder<UserActivitySessionRecord, UUID>) -> Unit),
  order: ((QueryBuilder<UserActivitySessionRecord, UUID>) -> Unit),
) :
  Paginator<UserActivitySession>,
  OrmLitePaginatorImpl<UserActivitySessionRecord, UUID>(dao, pageSize, where, order) {

  override fun paginate(page: Long): List<UserActivitySession> {
    return super.getPage(page).map { it.toDomain() }
  }
}
