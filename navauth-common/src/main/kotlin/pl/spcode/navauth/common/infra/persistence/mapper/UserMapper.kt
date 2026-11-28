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

package pl.spcode.navauth.common.infra.persistence.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.persistence.exposed.UsersTable

fun ResultRow.toUser(): User {
  val uuid = UserUuid(this[UsersTable.uuid])
  val username = Username(this[UsersTable.username])
  val mojangUuid = this[UsersTable.mojangUuid]
  val credentialsRequired = this[UsersTable.credentialsRequired]

  return if (mojangUuid != null) {
    User.premium(
      id = uuid,
      username = username,
      mojangUuid = MojangId(mojangUuid),
      requiresCredentials = credentialsRequired,
    )
  } else {
    User.nonPremium(id = uuid, username = username)
  }
}
