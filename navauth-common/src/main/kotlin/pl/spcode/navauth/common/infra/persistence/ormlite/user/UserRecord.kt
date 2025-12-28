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

package pl.spcode.navauth.common.infra.persistence.ormlite.user

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.UUID

@DatabaseTable(tableName = "navauth_users")
data class UserRecord(
  @DatabaseField(columnName = "uuid", id = true) val uuid: UUID = UUID.randomUUID(),
  @DatabaseField(columnName = "mojang_uuid", canBeNull = true, index = true)
  val mojangUuid: UUID? = null,
  @DatabaseField(columnName = "username", canBeNull = false, index = true)
  val username: String = "",
  // we use another field for username lowercased, because it is easier than creating a separate
  // index
  @DatabaseField(columnName = "username_lowercase", canBeNull = false, index = true)
  val usernameLowercase: String = "",
  @DatabaseField(columnName = "credentials_required", canBeNull = false)
  val credentialsRequired: Boolean = true,
) {
  init {
    require(usernameLowercase == username.lowercase()) { "usernameLowercase must be lowercase" }
  }
}
