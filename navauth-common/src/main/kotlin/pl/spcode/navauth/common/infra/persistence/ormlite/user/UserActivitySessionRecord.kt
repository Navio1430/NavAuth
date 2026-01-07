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

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.Date
import java.util.UUID

@DatabaseTable(tableName = "navauth_users_sessions")
class UserActivitySessionRecord(
  @DatabaseField(columnName = "uuid", index = true, canBeNull = false)
  val uuid: UUID = UUID.randomUUID(),
  @DatabaseField(columnName = "joined_at", dataType = DataType.DATE_LONG, canBeNull = false)
  val joinedAt: Date? = null,
  @DatabaseField(
    columnName = "left_at",
    index = true,
    dataType = DataType.DATE_LONG,
    canBeNull = false,
  )
  val leftAt: Date? = null,
  @DatabaseField(columnName = "ip") val ip: Long = 0,
)
