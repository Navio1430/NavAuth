/*
 * NavAuth
 * Copyright © 2026 Oliwier Fijas (Navio1430)
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

package pl.spcode.navauth.velocity.application.totp

import com.google.common.cache.CacheBuilder
import com.google.inject.Singleton
import java.time.Duration
import pl.spcode.navauth.common.domain.user.UserUuid

@Singleton
class TotpSetupSessionService {

  companion object {
    const val SESSION_LIFETIME_SECONDS: Long = 300
  }

  val sessions =
    CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(SESSION_LIFETIME_SECONDS))
      .build<UserUuid, TotpSetupSession>()

  fun registerSession(uuid: UserUuid, session: TotpSetupSession) {
    sessions.put(uuid, session)
  }

  fun findSession(uuid: UserUuid): TotpSetupSession? {
    return sessions.getIfPresent(uuid)
  }

  fun closeSession(uuid: UserUuid) {
    sessions.invalidate(uuid)
  }
}
