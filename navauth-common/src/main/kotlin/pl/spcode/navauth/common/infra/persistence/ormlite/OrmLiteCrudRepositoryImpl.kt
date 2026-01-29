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

package pl.spcode.navauth.common.infra.persistence.ormlite

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.DeleteBuilder
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.stmt.UpdateBuilder
import kotlin.reflect.KClass
import pl.spcode.navauth.common.infra.database.DatabaseManager

open class OrmLiteCrudRepositoryImpl<T : Any, ID>(
  private val databaseManager: DatabaseManager,
  private val clazz: KClass<T>,
) : OrmLiteCrudRepository<T, ID> {

  override fun save(entity: T): Dao.CreateOrUpdateStatus {
    return dao().createOrUpdate(entity)
  }

  override fun saveAll(entities: Iterable<T>): List<Dao.CreateOrUpdateStatus> {
    return entities.map { save(it) }
  }

  override fun findById(id: ID): T? {
    return dao().queryForId(id)
  }

  override fun existsById(id: ID): Boolean {
    return dao().idExists(id)
  }

  override fun findAll(): List<T> {
    return dao().queryForAll()
  }

  override fun findAllById(ids: Iterable<ID>): List<T> {
    return ids.mapNotNull { findById(it) }
  }

  override fun count(): Long {
    return dao().countOf()
  }

  override fun deleteById(id: ID): Int {
    return dao().deleteById(id)
  }

  override fun delete(entity: T): Int {
    return dao().delete(entity)
  }

  override fun deleteAll(entities: Iterable<T>): Int {
    return dao().delete(entities.toList())
  }

  override fun deleteAll(): Int {
    return dao().deleteBuilder().delete()
  }

  protected fun queryBuilder(): QueryBuilder<T, ID> {
    return dao().queryBuilder()
  }

  protected fun deleteBuilder(): DeleteBuilder<T, ID> {
    return dao().deleteBuilder()
  }

  protected fun updateBuilder(): UpdateBuilder<T, ID> {
    return dao().updateBuilder()
  }

  protected fun dao(): Dao<T, ID> {
    return databaseManager.getDao(clazz)
  }
}
