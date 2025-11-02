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

package pl.spcode.navauth.common.application.auth.session

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.credentials.CredentialsService
import pl.spcode.navauth.common.domain.auth.UniqueSessionId
import pl.spcode.navauth.common.domain.auth.session.AuthSession

/** Maintains user sessions for as long as they are active on the server. */
@Singleton
open class AuthSessionService @Inject constructor(val credentialsService: CredentialsService) {

  private val logger: Logger = LoggerFactory.getLogger(AuthSessionService::class.java)

  private val sessionsMap = ConcurrentHashMap<UniqueSessionId, AuthSession>()

  fun <T : AuthSession> registerSession(uniqueSessionId: UniqueSessionId, session: T): T {
    sessionsMap[uniqueSessionId] = session
    logger.debug(
      "registered new session auth session (type='{}') with {} ID",
      session.getSessionType(),
      uniqueSessionId,
    )
    return session
  }

  //  fun createLoginAuthSession(existingUser: User): LoginAuthSession {
  //    val username = existingUser.username
  //    val credentials =
  //      credentialsService.findCredentials(existingUser)
  //        ?: throw AuthSessionException("user $username credentials not found")
  //
  //    val session = LoginAuthSession(credentials, credentialsService)
  //    sessionsMap[username] = session
  //    logger.debug("created auth session (login) for user {}: {}", username, session)
  //    return session
  //  }
  //
  //  fun createRegisterAuthSession(username: String): AuthSession {
  //    val session = RegisterAuthSession()
  //    sessionsMap[username] = session
  //    logger.debug("created auth session (register) for user {}: {}", username, session)
  //    return session
  //  }
  //
  //  fun createPremiumAuthSession(username: String): PremiumAuthSession {
  //    val session = PremiumAuthSession()
  //    sessionsMap[username] = session
  //    logger.debug("created auth session (premium) for user {}: {}", username, session)
  //    return session
  //  }

  fun findSession(uniqueSessionId: UniqueSessionId): AuthSession? {
    return sessionsMap.get(uniqueSessionId)
  }

  fun invalidateSession(uniqueSessionId: UniqueSessionId): Boolean {
    val session = sessionsMap.remove(uniqueSessionId)
    session?.destroy()
    return session != null
  }
}
