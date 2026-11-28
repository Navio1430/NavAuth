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

package unit.auth

import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.UUID
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.listener.velocity.ChatListeners
import pl.spcode.navauth.velocity.listener.velocity.ConnectionListeners

/** A player without an auth session, or a listener that throws, never gets past NavAuth. */
class SessionListenersSecurityTests :
  FunSpec({
    fun player(): Player {
      val player = mockk<Player>(relaxed = true)
      every { player.username } returns "Admin"
      every { player.uniqueId } returns UUID.randomUUID()
      every { player.remoteAddress } returns
        InetSocketAddress(InetAddress.getByName("2001:db8::1"), 54321)
      every { player.isActive } returns true
      return player
    }

    fun connectionListeners(authSessionService: AuthSessionService<VelocityPlayerAdapter>) =
      ConnectionListeners(
        authSessionService,
        mockk(relaxed = true),
        mockk(relaxed = true),
        GeneralConfig(),
      )

    test("player without an auth session cannot chat") {
      val event = PlayerChatEvent(player(), "hello")

      ChatListeners(AuthSessionService()).onChat(event)

      event.result.isAllowed.shouldBeFalse()
    }

    test("player without an auth session gets no initial server") {
      val player = player()
      val event = PlayerChooseInitialServerEvent(player, mockk<RegisteredServer>())

      connectionListeners(AuthSessionService()).onPlayerChooseInitialServer(event)

      event.initialServer.isPresent.shouldBeFalse()
      verify(atLeast = 1) { player.disconnect(any()) }
    }

    test("an exception at ServerPreConnect denies the connection") {
      val authSessionService = mockk<AuthSessionService<VelocityPlayerAdapter>>()
      every { authSessionService.findSession(any()) } throws RuntimeException("unexpected")
      val player = player()
      val event = ServerPreConnectEvent(player, mockk<RegisteredServer>(relaxed = true), null)

      connectionListeners(authSessionService).onServerPreConnect(event)

      event.result.isAllowed.shouldBeFalse()
      verify(atLeast = 1) { player.disconnect(any()) }
    }

    test("an exception at PlayerChooseInitialServer leaves no initial server") {
      val authSessionService = mockk<AuthSessionService<VelocityPlayerAdapter>>()
      every { authSessionService.findSession(any()) } throws RuntimeException("unexpected")
      val player = player()
      val event = PlayerChooseInitialServerEvent(player, mockk<RegisteredServer>())

      connectionListeners(authSessionService).onPlayerChooseInitialServer(event)

      event.initialServer.isPresent.shouldBeFalse()
      verify(atLeast = 1) { player.disconnect(any()) }
    }
  })
