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
   * Prepares the migrator for the migration process. This function is called to initialize any
   * required resources or perform setup operations before the migration can begin.
   */
  fun init()

  /** @return the total number of user records in the source database */
  fun getSourceUsersCount(): Long

  /**
   * Migrates a chunk of source records starting from the specified offset. A record that cannot be
   * imported is skipped and logged; it never fails the chunk.
   *
   * @param offset the position of the first source record of this chunk
   * @param limit the maximum number of source records to read in this chunk
   * @return what happened to the records of this chunk; the next chunk starts at `offset +
   *   result.read`, and `read == 0` means no records are left
   */
  fun migrateNext(offset: Long, limit: Long): MigrationChunkResult
}

/**
 * @property read source records read in this chunk, imported or skipped
 * @property migrated NavAuth accounts written
 * @property skipped source records not imported (already in NavAuth, invalid or unusable)
 */
data class MigrationChunkResult(val read: Long, val migrated: Long, val skipped: Long)
