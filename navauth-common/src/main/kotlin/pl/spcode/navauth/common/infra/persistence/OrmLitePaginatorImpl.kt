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

package pl.spcode.navauth.common.infra.persistence

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.QueryBuilder

open class OrmLitePaginatorImpl<T, ID>(
  private val dao: Dao<T, ID>,
  private val pageSize: Long,
  private val where: ((QueryBuilder<T, ID>) -> Unit),
  private val order: ((QueryBuilder<T, ID>) -> Unit),
) {

  @Suppress("unused")
  fun getPagesCount(): Long {
    val builder = dao.queryBuilder()
    where.invoke(builder)
    val total = builder.countOf()
    return if (total == 0L) 0 else (total - 1) / pageSize + 1
  }

  fun getPage(page: Long): List<T> {
    val builder = dao.queryBuilder()
    where.invoke(builder)
    order.invoke(builder)
    builder.offset((page - 1) * pageSize).limit(pageSize)
    return dao.query(builder.prepare())
  }
}
