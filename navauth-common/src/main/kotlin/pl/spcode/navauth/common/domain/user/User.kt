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
import pl.spcode.navauth.api.domain.AuthUser

@JvmInline
value class UserUuid(val value: UUID) {
  override fun toString(): String = value.toString()
}

@JvmInline
value class MojangId(val value: UUID) {
  override fun toString(): String = value.toString()
}

@JvmInline
value class Username(val value: String) {
  override fun toString(): String = value
}

@ConsistentCopyVisibility
data class User
private constructor(
  val uuid: UserUuid,
  val username: Username,
  val credentialsRequired: Boolean,
  val mojangUuid: MojangId? = null, // null = non-premium
) {
  val isPremium: Boolean
    get() = mojangUuid != null

  companion object Factory {
    fun nonPremium(id: UserUuid, username: Username): User = User(id, username, true)

    fun premium(
      id: UserUuid,
      username: Username,
      mojangUuid: MojangId,
      requiresCredentials: Boolean = false,
    ): User = User(id, username, requiresCredentials, mojangUuid)
  }

  fun withNewUsername(username: Username): User {
    return User(this.uuid, username, this.credentialsRequired, this.mojangUuid)
  }

  fun toNonPremium(): User {
    return copy(mojangUuid = null, credentialsRequired = true)
  }

  fun withCredentialsRequired(required: Boolean = true) = copy(credentialsRequired = required)

  fun toAuthUser(): AuthUser {
    return object : AuthUser {
      override fun getUUID(): UUID {
        return this@User.uuid.value
      }

      override fun getMojangUUID(): UUID? {
        return this@User.mojangUuid?.value
      }

      override fun getUsername(): String {
        return this@User.username.value
      }
    }
  }

  override fun toString(): String {
    return "User(id=$uuid, username=$username, mojangUuid=$mojangUuid, credentialsRequired=$credentialsRequired)"
  }
}
