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

package pl.spcode.navauth.common.shared.data

import com.j256.ormlite.dao.Dao

interface OrmLiteCrudRepository<T : Any, ID> {

  fun save(entity: T): Dao.CreateOrUpdateStatus

  fun saveAll(entities: Iterable<T>): List<Dao.CreateOrUpdateStatus>

  fun findById(id: ID): T?

  fun existsById(id: ID): Boolean

  fun findAll(): List<T>

  fun findAllById(ids: Iterable<ID>): List<T>

  fun count(): Long

  fun deleteById(id: ID): Int

  fun delete(entity: T): Int

  fun deleteAll(entities: Iterable<T>): Int

  fun deleteAll(): Int
}
