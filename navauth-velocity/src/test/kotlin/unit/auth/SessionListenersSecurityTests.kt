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

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.UUID
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.common.domain.auth.session.AuthSessionState
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.listener.velocity.ChatListeners
import pl.spcode.navauth.velocity.listener.velocity.CommandsListeners
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

    fun server(name: String): RegisteredServer {
      val server = mockk<RegisteredServer>(relaxed = true)
      every { server.serverInfo } returns ServerInfo(name, InetSocketAddress("127.0.0.1", 25566))
      return server
    }

    /** Listeners for a player who waits in a limbo for the login handler. */
    fun waitingForHandlerListeners(vararg limboServers: String): ConnectionListeners {
      val session = mockk<AuthSession<VelocityPlayerAdapter>>()
      every { session.isAuthenticated } returns false
      every { session.state } returns AuthSessionState.WAITING_FOR_HANDLER
      val authSessionService = mockk<AuthSessionService<VelocityPlayerAdapter>>()
      every { authSessionService.findSession(any()) } returns session
      val generalConfig = mockk<GeneralConfig>()
      every { generalConfig.limboServers } returns limboServers.toList()
      return ConnectionListeners(
        authSessionService,
        mockk(relaxed = true),
        mockk(relaxed = true),
        generalConfig,
      )
    }

    test("a connection redirected by another plugin to a non-limbo server is denied") {
      val event = ServerPreConnectEvent(player(), server("limbo"), null)
      event.result = ServerPreConnectEvent.ServerResult.allowed(server("lobby"))

      waitingForHandlerListeners("limbo").onServerPreConnect(event)

      event.result.isAllowed.shouldBeFalse()
    }

    test("a connection redirected by another plugin to another limbo server is allowed") {
      val limbo2 = server("limbo2")
      val event = ServerPreConnectEvent(player(), server("limbo"), null)
      event.result = ServerPreConnectEvent.ServerResult.allowed(limbo2)

      waitingForHandlerListeners("limbo", "limbo2").onServerPreConnect(event)

      event.result.isAllowed.shouldBeTrue()
      event.result.server.get() shouldBe limbo2
    }

    test("a connection to a limbo server is allowed while waiting for the login handler") {
      val event = ServerPreConnectEvent(player(), server("limbo"), null)

      waitingForHandlerListeners("limbo").onServerPreConnect(event)

      event.result.isAllowed.shouldBeTrue()
    }

    test("a connection another plugin denied stays denied") {
      val event = ServerPreConnectEvent(player(), server("limbo"), null)
      event.result = ServerPreConnectEvent.ServerResult.denied()

      waitingForHandlerListeners("limbo").onServerPreConnect(event)

      event.result.isAllowed.shouldBeFalse()
    }

    test("a player who is not logged in cannot chat") {
      val session = mockk<AuthSession<VelocityPlayerAdapter>>()
      every { session.isAuthenticated } returns false
      val authSessionService = mockk<AuthSessionService<VelocityPlayerAdapter>>()
      every { authSessionService.findSession(any()) } returns session
      val event = PlayerChatEvent(player(), "hello")

      ChatListeners(authSessionService).onChat(event)

      event.result.isAllowed.shouldBeFalse()
    }

    test("other plugins see the chat of a player who is not logged in as denied") {
      val session = mockk<AuthSession<VelocityPlayerAdapter>>()
      every { session.isAuthenticated } returns false
      val authSessionService = mockk<AuthSessionService<VelocityPlayerAdapter>>()
      every { authSessionService.findSession(any()) } returns session
      val event = PlayerChatEvent(player(), "hello")

      ChatListeners(authSessionService).onChatFirst(event)

      event.result.isAllowed.shouldBeFalse()
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

    test("chat, command and server connection listeners run last") {
      fun priorityOf(listener: Class<*>, method: String, event: Class<*>) =
        listener.getMethod(method, event).getAnnotation(Subscribe::class.java).priority

      priorityOf(ChatListeners::class.java, "onChat", PlayerChatEvent::class.java) shouldBe
        Short.MIN_VALUE
      priorityOf(
        CommandsListeners::class.java,
        "onCommand",
        CommandExecuteEvent::class.java,
      ) shouldBe Short.MIN_VALUE
      priorityOf(
        CommandsListeners::class.java,
        "onPlayerAvailableCommands",
        PlayerAvailableCommandsEvent::class.java,
      ) shouldBe Short.MIN_VALUE
      priorityOf(
        ConnectionListeners::class.java,
        "onServerPreConnect",
        ServerPreConnectEvent::class.java,
      ) shouldBe Short.MIN_VALUE
    }

    test("chat and command listeners also run first") {
      fun orderOf(listener: Class<*>, method: String, event: Class<*>) =
        @Suppress("DEPRECATION")
        listener.getMethod(method, event).getAnnotation(Subscribe::class.java).order

      orderOf(ChatListeners::class.java, "onChatFirst", PlayerChatEvent::class.java) shouldBe
        PostOrder.FIRST
      orderOf(
        CommandsListeners::class.java,
        "onCommandFirst",
        CommandExecuteEvent::class.java,
      ) shouldBe PostOrder.FIRST
    }
  })
