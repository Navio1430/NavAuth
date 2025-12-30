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

package pl.spcode.navauth.common.application.auth.handshake

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.inject.Singleton
import java.time.Duration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.domain.auth.UniqueSessionId
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.user.User

@Singleton
class AuthHandshakeSessionService {

  private val logger: Logger = LoggerFactory.getLogger(AuthHandshakeSessionService::class.java)

  private val sessionsCache: Cache<UniqueSessionId, AuthHandshakeSession> =
    CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(15)).build()

  /**
   * @param sessionId id which must be unique for each connection of using the same user data at the
   *   same time (e.g., socket port)
   * @param existingUser user who already exists in the database
   * @param connUsername username who made the initial connection
   * @param state current state of the handshake session
   */
  fun createSession(
    sessionId: UniqueSessionId,
    existingUser: User?,
    connUsername: String,
    usernameResResult: UsernameResResult,
  ): AuthHandshakeSession {
    require(usernameResResult is UsernameResResult.Success) {
      "username resolution must succeed to create handshake auth session"
    }

    val session =
      AuthHandshakeSession(
        existingUser,
        connUsername,
        usernameResResult.requestedEncryption,
        usernameResResult.postResolutionState,
      )
    sessionsCache.put(sessionId, session)
    logger.debug(
      "created auth handshake session (id='{}') for user {}: {}",
      sessionId,
      connUsername,
      session,
    )
    return session
  }

  fun findSession(sessionId: UniqueSessionId): AuthHandshakeSession? {
    return sessionsCache.getIfPresent(sessionId)
  }

  fun closeSession(sessionId: UniqueSessionId) {
    logger.debug("invalidated auth handshake session with id {}", sessionId.id)
    sessionsCache.invalidate(sessionId)
  }
}
