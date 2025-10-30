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

package pl.spcode.navauth.common.domain.user

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.UUID

@DatabaseTable(tableName = "navauth_users")
class User {
  @DatabaseField(id = true) val uuid: UUID?
  @DatabaseField(canBeNull = false, index = true) val username: String
  @DatabaseField(canBeNull = false) val isPremium: Boolean

  @Suppress("unused") private constructor() : this(null, "", false)

  private constructor(uuid: UUID?, username: String, isPremium: Boolean) {
    this.uuid = uuid
    this.username = username
    this.isPremium = isPremium
  }

  companion object {
    fun create(uuid: UUID, username: String, isPremium: Boolean): User {
      // todo: check username with regex

      return User(uuid, username, isPremium)
    }
  }
}
