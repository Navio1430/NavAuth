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

package pl.spcode.navauth.velocity

import com.google.inject.Inject
import com.velocitypowered.api.proxy.ProxyServer
import java.util.UUID
import pl.spcode.navauth.api.NavAuthAPI
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

class NavAuthApiImpl
@Inject
constructor(
  val eventBusInstance: NavAuthEventBus,
  val authSessionService: AuthSessionService<VelocityPlayerAdapter>,
  val proxyServer: ProxyServer,
) : NavAuthAPI() {

  override fun provideEventBus(): NavAuthEventBus {
    return eventBusInstance
  }

  override fun isAuthenticated(playerUuid: UUID): Boolean {
    val player = proxyServer.getPlayer(playerUuid).orElse(null) ?: return false
    val sessionId = VelocityUniqueSessionId(player)
    val session = authSessionService.findSession(sessionId)
    return session?.isAuthenticated == true
  }
}
