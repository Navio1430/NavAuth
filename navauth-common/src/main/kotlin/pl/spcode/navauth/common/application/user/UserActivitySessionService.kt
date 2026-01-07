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

package pl.spcode.navauth.common.application.user

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.domain.common.IPAddress
import pl.spcode.navauth.common.domain.player.PlayerAdapter
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserActivitySession
import pl.spcode.navauth.common.domain.user.UserActivitySessionRepository
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.persistence.Paginator

@Singleton
class UserActivitySessionService
@Inject
constructor(private val userActivitySessionRepository: UserActivitySessionRepository) {

  private val logger: Logger = LoggerFactory.getLogger(UserActivitySessionService::class.java)
  private val playerJoinedAtMap = ConcurrentHashMap<UUID, PlayerSessionData>()

  // we need to save ip address here because it won't be available on disconnect event
  private data class PlayerSessionData(val joinedAt: Date, val ip: IPAddress)

  fun registerPlayerJoin(playerAdapter: PlayerAdapter) {
    playerJoinedAtMap[playerAdapter.getUuid().value] =
      PlayerSessionData(Date(), playerAdapter.getIPAddress())
  }

  fun storePlayerSessionOnLeave(playerAdapter: PlayerAdapter) {
    val data = playerJoinedAtMap.get(playerAdapter.getUuid().value)
    if (data == null) {
      logger.debug("player with UUID {} session was not registered", playerAdapter.getUuid())
      return
    }
    val leftAt = Date()

    val session =
      UserActivitySession.create(
        playerAdapter.getUuid(),
        data.joinedAt,
        leftAt,
        playerAdapter.getIPAddress(),
      )
    userActivitySessionRepository.save(session)
  }

  fun findLatestSession(user: User): UserActivitySession? {
    return userActivitySessionRepository.findLatestByUuid(user.uuid)
  }

  fun getSessionPaginatorByUuid(uuid: UserUuid, pageSize: Long): Paginator<UserActivitySession> {
    return userActivitySessionRepository.getSessionPaginatorByUuid(uuid, pageSize)
  }
}
