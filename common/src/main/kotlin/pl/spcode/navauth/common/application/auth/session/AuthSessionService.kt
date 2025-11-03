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

import com.google.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.domain.auth.UniqueSessionId
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.common.domain.player.DisconnectReason
import pl.spcode.navauth.common.domain.player.PlayerAdapter

/** Maintains user sessions for as long as they are active on the server. */
@Singleton
open class AuthSessionService<P : PlayerAdapter> {

  private val logger: Logger = LoggerFactory.getLogger(AuthSessionService::class.java)

  private val sessionsMap = ConcurrentHashMap<UniqueSessionId, AuthSession<P>>()

  fun <T : AuthSession<P>> registerSession(uniqueSessionId: UniqueSessionId, session: T): T {
    sessionsMap[uniqueSessionId] = session
    logger.debug(
      "registered new auth session (type='{}') with ID {}",
      session.getSessionType(),
      uniqueSessionId,
    )
    return session
  }

  fun findSession(uniqueSessionId: UniqueSessionId): AuthSession<P>? {
    return sessionsMap.get(uniqueSessionId)
  }

  fun closeSession(uniqueSessionId: UniqueSessionId): Boolean {
    val session = sessionsMap.remove(uniqueSessionId)
    if (session != null) {
      session.playerAdapter.disconnect(DisconnectReason.AUTH_SESSION_CLOSED)
      session.destroy()
      logger.debug(
        "invalidated auth session (type='{}') with ID {}",
        session.getSessionType(),
        uniqueSessionId,
      )
    } else {
      logger.debug("can't invalidate non-existing auth session with ID {}", uniqueSessionId)
    }
    return session != null
  }
}
