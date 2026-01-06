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

package pl.spcode.navauth.common.domain.user

import java.util.Date
import pl.spcode.navauth.common.domain.common.IPAddress

data class UserActivitySession
private constructor(val uuid: UserUuid, val joinedAt: Date, val leftAt: Date, val ip: IPAddress) {
  init {
    require(joinedAt.before(leftAt)) { "joined at must be before left at" }
  }

  companion object Factory {
    fun create(uuid: UserUuid, joinedAt: Date, leftAt: Date, ip: IPAddress) =
      UserActivitySession(uuid, joinedAt, leftAt, ip)
  }
}
