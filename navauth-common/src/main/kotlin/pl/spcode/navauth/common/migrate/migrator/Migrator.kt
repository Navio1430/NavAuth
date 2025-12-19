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

package pl.spcode.navauth.common.migrate.migrator

interface Migrator {

  /**
   * Prepares the migrator for the migration process.
   * This function is called to initialize any required resources or perform
   * setup operations before the migration can begin.
   */
  fun init()

  /**
   * @return the total number of user records in the source database
   */
  fun getSourceRecordsCount(): Long

  /**
   * Migrates a chunk of records starting from the specified offset.
   *
   * @param offset the starting position of records to migrate
   * @param limit the maximum number of records to migrate in this operation
   * @return the number of records successfully migrated
   */
  fun migrateNext(offset: Long, limit: Long): Long
}