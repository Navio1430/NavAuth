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

package pl.spcode.navauth.common.application.auth.login

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.credentials.CredentialsService
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.AuthState
import pl.spcode.navauth.common.domain.auth.login.LoginSession
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.auth.login.LoginSessionImpl
import java.time.Duration

@Singleton
class LoginSessionService(val credentialsService: CredentialsService) {

  val logger: Logger = LoggerFactory.getLogger(LoginSessionService::class.java)

  val sessionsByUsername: Cache<String, LoginSession> =
      CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(10)).build()

  fun createLoginSession(user: User): LoginSession {
    val username = user.username
    val credentials = credentialsService.findCredentials(user)
        ?: throw LoginSessionException("user $username credentials not found")

    val session = LoginSessionImpl(credentials, credentialsService)
    sessionsByUsername.put(username, session)
    logger.debug("created login session for {} username {}", username, session)
    return session
  }

  fun findSession(username: String): LoginSession? {
    return sessionsByUsername.getIfPresent(username)
  }
}