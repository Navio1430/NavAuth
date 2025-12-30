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

package pl.spcode.navauth.common.domain.auth.handshake

import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.domain.user.User

/**
 * Represents a session during the authentication handshake process.
 *
 * @property existingUser The existing previously persisted user, or null if the user is not
 *   registered yet.
 * @property connUsername The connection username provided during the handshake.
 * @property requestedEncryptionType The type of encryption requested during the handshake.
 * @property postUsernameResolutionState Represents the state resulting from resolving the username
 *   during the handshake process.
 */
class AuthHandshakeSession(
  val existingUser: User?,
  val connUsername: String,
  val requestedEncryptionType: EncryptionType,
  val postUsernameResolutionState: PostUsernameResolutionState,
) {

  override fun toString(): String {
    return "AuthHandshakeSession(existingUser=$existingUser, connUsername='$connUsername', requestedEncryptionType=$requestedEncryptionType, postUsernameResolutionState=$postUsernameResolutionState)"
  }
}
