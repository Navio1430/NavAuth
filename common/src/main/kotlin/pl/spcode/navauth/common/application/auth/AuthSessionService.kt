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

package pl.spcode.navauth.common.application.auth

import com.google.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import pl.spcode.navauth.common.domain.auth.AuthSession
import pl.spcode.navauth.common.domain.auth.AuthState

@Singleton
class AuthSessionService {

  val sessionsByUsername = ConcurrentHashMap<String, AuthSession>()

  // todo add "hash" made out of unique properties such as connection id
  fun createSession(username: String, isPremium: Boolean, state: AuthState): AuthSession {
    // todo persist session
    val session = AuthSession(username, isPremium, state)
    sessionsByUsername[username] = session
    return session
  }

  fun findSession(username: String): AuthSession? {
    return sessionsByUsername.get(username)
  }

  fun invalidateSession(session: AuthSession) {
    sessionsByUsername.remove(session.username)
  }
}
