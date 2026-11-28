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

import java.util.UUID
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

  /**
   * True once [verifyPremiumIdentity] refused the connection: it was not authenticated by Mojang,
   * or Mojang authenticated another account than [existingUser]'s.
   */
  var premiumIdentityMismatch: Boolean = false
    private set

  /**
   * Checks the connection against this handshake. [EncryptionType.ENFORCE_PREMIUM] requires a
   * connection authenticated by Mojang (online mode), for new and existing accounts alike: an
   * offline connection proves nothing about who the player is. For an existing account, the Mojang
   * UUID the proxy authenticated must also be the stored one. Username resolution decides from
   * (cached) profile API answers; this check uses the session server's answer, so it also holds
   * when the profile cache is stale.
   *
   * @param onlineMode whether the proxy authenticated this connection with Mojang
   * @param authenticatedMojangId UUID of the connection's profile (the Mojang UUID in online mode)
   * @return false when the player must not receive a premium identity, stored or new; the session
   *   is then flagged with [premiumIdentityMismatch]
   */
  fun verifyPremiumIdentity(onlineMode: Boolean, authenticatedMojangId: UUID): Boolean {
    if (requestedEncryptionType != EncryptionType.ENFORCE_PREMIUM) return true
    val user = existingUser
    val matches = onlineMode && (user == null || user.mojangUuid?.value == authenticatedMojangId)
    if (!matches) premiumIdentityMismatch = true
    return matches
  }

  override fun toString(): String {
    return "AuthHandshakeSession(existingUser=$existingUser, connUsername='$connUsername', requestedEncryptionType=$requestedEncryptionType, postUsernameResolutionState=$postUsernameResolutionState, premiumIdentityMismatch=$premiumIdentityMismatch)"
  }
}
