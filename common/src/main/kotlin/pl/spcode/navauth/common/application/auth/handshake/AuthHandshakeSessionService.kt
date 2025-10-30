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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.AuthState
import java.time.Duration

@Singleton
class AuthHandshakeSessionService {

  val logger: Logger = LoggerFactory.getLogger(AuthHandshakeSessionService::class.java)

  val sessionsByUsername: Cache<String, AuthHandshakeSession> =
    CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(10)).build()

  // todo add "hash" made out of unique properties such as connection id
  fun createSession(connUsername: String, state: AuthState): AuthHandshakeSession {
    val session = AuthHandshakeSession(connUsername, state)
    sessionsByUsername.put(connUsername, session)
    logger.debug("created auth handshake session for {} username {}", connUsername, session)
    return session
  }

  fun findSession(connUsername: String): AuthHandshakeSession? {
    return sessionsByUsername.getIfPresent(connUsername)
  }

  fun authenticateSession(session: AuthHandshakeSession) {
    session.state = AuthState.AUTHENTICATED
    invalidateSession(session.username)
  }

  fun invalidateSession(connUsername: String) {
    logger.debug("invalidated auth handshake session for {} username", connUsername)
    sessionsByUsername.invalidate(connUsername)
  }
}