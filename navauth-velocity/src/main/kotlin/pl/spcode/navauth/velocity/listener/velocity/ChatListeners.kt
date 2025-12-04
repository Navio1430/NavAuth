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

package pl.spcode.navauth.velocity.listener.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

class ChatListeners
@Inject
constructor(val authSessionService: AuthSessionService<VelocityPlayerAdapter>) {

  @Subscribe(order = PostOrder.FIRST)
  fun onChat(event: PlayerChatEvent) {
    val player = event.player

    val sessionId = VelocityUniqueSessionId(player)
    val session = authSessionService.findSession(sessionId)
    if (session != null) {
      @Suppress("DEPRECATION") // IDK why do they mark this as deprecated if there's no other option
      if (!session.isAuthenticated) {
        event.result = PlayerChatEvent.ChatResult.denied()
      }
    }
  }
}
