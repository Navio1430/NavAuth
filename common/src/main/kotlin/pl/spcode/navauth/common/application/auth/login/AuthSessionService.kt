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

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.credentials.CredentialsService
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.auth.LoginAuthSession
import pl.spcode.navauth.common.infra.auth.PremiumAuthSession
import pl.spcode.navauth.common.infra.auth.RegisterAuthSession

@Singleton
class AuthSessionService @Inject constructor(val credentialsService: CredentialsService) {

  val logger: Logger = LoggerFactory.getLogger(AuthSessionService::class.java)

  val sessionsMap = ConcurrentHashMap<String, AuthSession>()

  fun createLoginAuthSession(existingUser: User): LoginAuthSession {
    val username = existingUser.username
    val credentials =
      credentialsService.findCredentials(existingUser)
        ?: throw AuthSessionException("user $username credentials not found")

    val session = LoginAuthSession(credentials, credentialsService)
    sessionsMap[username] = session
    logger.debug("created auth session (login) for user {}: {}", username, session)
    return session
  }

  fun createRegisterAuthSession(username: String): AuthSession {
    val session = RegisterAuthSession()
    logger.debug("created auth session (register) for user {}: {}", username, session)
    return session
  }

  fun createPremiumAuthSession(username: String): PremiumAuthSession {
    val session = PremiumAuthSession()
    sessionsMap[username] = session
    logger.debug("created auth session (premium) for user {}: {}", username, session)
    return session
  }

  fun findSession(username: String): AuthSession? {
    return sessionsMap.get(username)
  }

  fun invalidateSession(username: String) {
    sessionsMap.remove(username)
  }
}
