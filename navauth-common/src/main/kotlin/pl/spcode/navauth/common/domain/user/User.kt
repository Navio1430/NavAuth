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

import java.util.UUID

@JvmInline value class UserId(val value: UUID)

@JvmInline value class MojangId(val value: UUID)

@JvmInline
value class Username(val value: String) {
  init {
    // todo check regex
  }
}

@ConsistentCopyVisibility
data class User
private constructor(
  val id: UserId,
  val username: Username,
  val credentialsRequired: Boolean,
  val mojangUuid: MojangId? = null, // null = non-premium
) {
  val isPremium: Boolean
    get() = mojangUuid != null

  companion object Factory {
    fun nonPremium(id: UserId, username: Username): User = User(id, username, true)

    fun premium(
      id: UserId,
      username: Username,
      mojangUuid: MojangId,
      needsCreds: Boolean = false,
    ): User = User(id, username, needsCreds, mojangUuid)
  }

  fun withNewUsername(username: Username): User {
    return User(this.id, username, this.credentialsRequired, this.mojangUuid)
  }

  override fun toString(): String {
    return "User(id=$id, username=$username, mojangUuid=$mojangUuid, credentialsRequired=$credentialsRequired)"
  }
}
