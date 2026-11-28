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

package pl.spcode.navauth.common.infra.persistence.exposed

import java.util.UUID
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * Stores a [java.util.UUID] as its canonical string form.
 *
 * OrmLite persisted UUIDs as `VARCHAR(48)` (see the Phase 0 captured DDL), so Exposed's built-in
 * Kotlin `Uuid` column type would not match the physical schema. Keeping the string representation
 * here preserves existing installs and the per-dialect Flyway scripts.
 */
class UuidVarcharColumnType : ColumnType<UUID>() {

  override fun sqlType(): String = "VARCHAR(48)"

  override fun notNullValueToDB(value: UUID): Any = value.toString()

  override fun valueFromDB(value: Any): UUID =
    when (value) {
      is UUID -> value
      is String -> UUID.fromString(value)
      else -> UUID.fromString(value.toString())
    }
}

fun Table.uuidVarchar(name: String): Column<UUID> = registerColumn(name, UuidVarcharColumnType())
