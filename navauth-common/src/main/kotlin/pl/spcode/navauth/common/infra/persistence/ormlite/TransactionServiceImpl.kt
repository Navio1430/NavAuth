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

import com.google.inject.Inject
import com.j256.ormlite.misc.TransactionManager
import java.sql.SQLException
import pl.spcode.navauth.common.domain.common.TransactionService
import pl.spcode.navauth.common.infra.database.DatabaseManager

class TransactionServiceImpl @Inject constructor(val databaseManager: DatabaseManager) :
  TransactionService {

  @Suppress("OVERRIDE_BY_INLINE")
  override inline fun <R> inTransaction(crossinline block: () -> R): R {
    return TransactionManager.callInTransaction(databaseManager.connectionSource) {
      try {
        block()
      } catch (e: Exception) {
        throw SQLException("OrmLite SQL transaction failed", e)
      }
    }
  }
}
