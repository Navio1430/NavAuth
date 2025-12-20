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

package pl.spcode.navauth.common.migrate.migrator.librelogin

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.Date
import java.util.UUID

@DatabaseTable(tableName = "librepremium_data")
class LibreLoginUser(
  @DatabaseField(id = true, dataType = DataType.UUID, columnName = "uuid") var uuid: UUID? = null,
  @DatabaseField(dataType = DataType.UUID, canBeNull = true, columnName = "premium_uuid")
  var premiumUuid: UUID? = null,
  @DatabaseField(canBeNull = true, columnName = "hashed_password") var passwordHash: String? = null,
  @DatabaseField(canBeNull = true, columnName = "salt") var passwordSalt: String? = null,
  @DatabaseField(canBeNull = true, columnName = "algo") var passwordAlgo: String? = null,
  @DatabaseField(canBeNull = false, columnName = "last_nickname") var lastNickname: String? = null,
  @DatabaseField(dataType = DataType.DATE_LONG, columnName = "joined") var joinDate: Date? = null,
  @DatabaseField(dataType = DataType.DATE_LONG, columnName = "last_seen")
  var lastSeen: Date? = null,
  @DatabaseField(canBeNull = true, columnName = "secret") var secret: String? = null,
  @DatabaseField(canBeNull = true, columnName = "ip") var ip: String? = null,
  @DatabaseField(dataType = DataType.DATE_LONG, columnName = "last_authentication")
  var lastAuthentication: Date? = null,
  @DatabaseField(canBeNull = true, columnName = "last_server") var lastServer: String? = null,
  @DatabaseField(canBeNull = true, columnName = "email") var email: String? = null,
)
