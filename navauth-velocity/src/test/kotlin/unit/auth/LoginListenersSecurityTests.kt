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

import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.api.proxy.InboundConnection
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.util.GameProfile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.UUID
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.application.auth.username.UsernameResolutionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.config.UsernamesConfig
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.application.auth.InvalidSessionCacheService
import pl.spcode.navauth.velocity.application.auth.session.VelocityAuthSessionFactory
import pl.spcode.navauth.velocity.infra.auth.VelocityAutoLoginAuthSession
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.listener.velocity.LoginListeners

class LoginListenersSecurityTests :
  FunSpec({
    lateinit var userService: UserService
    lateinit var usernameValidator: UsernameValidator
    lateinit var authHandshakeSessionService: AuthHandshakeSessionService
    lateinit var usernameResolutionService: UsernameResolutionService
    lateinit var invalidSessionCacheService: InvalidSessionCacheService
    lateinit var authSessionFactory: VelocityAuthSessionFactory
    lateinit var messagesConfig: MessagesConfig
    lateinit var generalConfig: GeneralConfig
    lateinit var listeners: LoginListeners

    beforeTest {
      userService = mockk(relaxed = true)
      usernameValidator = UsernameValidator(UsernamesConfig())
      authHandshakeSessionService = AuthHandshakeSessionService()
      usernameResolutionService = mockk()
      generalConfig = GeneralConfig()
      messagesConfig = MessagesConfig()
      invalidSessionCacheService = InvalidSessionCacheService(generalConfig)
      authSessionFactory = mockk(relaxed = true)
      listeners =
        LoginListeners(
          userService,
          usernameValidator,
          authHandshakeSessionService,
          usernameResolutionService,
          invalidSessionCacheService,
          authSessionFactory,
          messagesConfig,
          generalConfig,
        )
    }

    test("VelocityUniqueSessionId distinguishes different IPs using the same source port") {
      val ipv4Socket = InetSocketAddress(InetAddress.getByName("192.0.2.1"), 54321)
      val ipv6Socket = InetSocketAddress(InetAddress.getByName("2001:db8::1"), 54321)
      val otherIpv4Socket = InetSocketAddress(InetAddress.getByName("198.51.100.2"), 54321)

      val id1 = VelocityUniqueSessionId("Admin", ipv4Socket)
      val id2 = VelocityUniqueSessionId("Admin", ipv6Socket)
      val id3 = VelocityUniqueSessionId("Admin", otherIpv4Socket)

      id1 shouldNotBe id2
      id1 shouldNotBe id3
      id2 shouldNotBe id3
    }

    test("InvalidSessionCacheService handles IPv6 connections without throwing") {
      val ipv6Conn = mockk<InboundConnection>()
      every { ipv6Conn.remoteAddress } returns
        InetSocketAddress(InetAddress.getByName("2001:db8::1"), 54321)
      every { ipv6Conn.protocolVersion } returns ProtocolVersion.MINECRAFT_1_21_4

      invalidSessionCacheService.isInvalidSessionReconnect("Admin", ipv6Conn).shouldBeFalse()
      invalidSessionCacheService.cachePremiumConnection("Admin", ipv6Conn)
      invalidSessionCacheService.isInvalidSessionReconnect("Admin", ipv6Conn) shouldNotBe false
    }

    test("onPreLogin fails closed and denies connection on unexpected exception") {
      val conn = mockk<InboundConnection>()
      every { conn.remoteAddress } returns
        InetSocketAddress(InetAddress.getByName("192.0.2.1"), 54321)
      every { userService.findUserByUsernameIgnoreCase("Admin") } throws
        RuntimeException("database failure")

      val event = PreLoginEvent(conn, "Admin")
      listeners.onPreLogin(event)

      event.result.isAllowed.shouldBeFalse()
    }

    test("onPostLogin rejects ENFORCE_PREMIUM handshake when player is not in online mode") {
      val adminUuid = UUID.randomUUID()
      val adminUser = User.premium(UserUuid(adminUuid), Username("Admin"), MojangId(adminUuid))
      val remoteAddress = InetSocketAddress(InetAddress.getByName("192.0.2.1"), 54321)
      val sessionId = VelocityUniqueSessionId("Admin", remoteAddress)

      authHandshakeSessionService.createSession(
        sessionId,
        adminUser,
        "Admin",
        UsernameResResult.Success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.NO_CHANGE,
        ),
      )

      val offlinePlayer = mockk<Player>(relaxed = true)
      every { offlinePlayer.username } returns "Admin"
      every { offlinePlayer.uniqueId } returns adminUuid
      every { offlinePlayer.remoteAddress } returns remoteAddress
      every { offlinePlayer.isOnlineMode } returns false
      every { offlinePlayer.isActive } returns true

      val autoLoginSession = mockk<VelocityAutoLoginAuthSession>(relaxed = true)
      every { authSessionFactory.createPremiumAuthSession(any(), any()) } returns autoLoginSession

      listeners.onPostLogin(PostLoginEvent(offlinePlayer))

      verify(exactly = 0) { authSessionFactory.createPremiumAuthSession(any(), any()) }
      verify(exactly = 0) { autoLoginSession.authenticate() }
      verify(atLeast = 1) { offlinePlayer.disconnect(any()) }
    }

    test("onGameProfile does not throw when handshake session is missing") {
      val conn = mockk<InboundConnection>()
      every { conn.remoteAddress } returns
        InetSocketAddress(InetAddress.getByName("192.0.2.1"), 54321)
      val originalProfile = GameProfile(UUID.randomUUID(), "Admin", emptyList())
      val event = GameProfileRequestEvent(conn, originalProfile, false)

      listeners.onGameProfile(event)
    }

    test(
      "onGameProfile closes handshake session and refuses profile rewrite on Mojang UUID mismatch"
    ) {
      val storedMojangUuid = UUID.randomUUID()
      val verifiedMojangUuid = UUID.randomUUID()
      val adminUser =
        User.premium(UserUuid(storedMojangUuid), Username("Admin"), MojangId(storedMojangUuid))
      val remoteAddress = InetSocketAddress(InetAddress.getByName("192.0.2.1"), 54321)
      val sessionId = VelocityUniqueSessionId("Admin", remoteAddress)

      authHandshakeSessionService.createSession(
        sessionId,
        adminUser,
        "Admin",
        UsernameResResult.Success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.NO_CHANGE,
        ),
      )

      val conn = mockk<InboundConnection>()
      every { conn.remoteAddress } returns remoteAddress
      val originalProfile = GameProfile(verifiedMojangUuid, "Admin", emptyList())
      val event = GameProfileRequestEvent(conn, originalProfile, true)

      listeners.onGameProfile(event)

      event.gameProfile.id shouldNotBe storedMojangUuid
      (authHandshakeSessionService.findSession(sessionId) == null) shouldNotBe false
    }

    test(
      "IPv4 aborted premium preLogin followed by IPv6 offline connection on same port cannot authenticate"
    ) {
      val adminUuid = UUID.randomUUID()
      val adminUser = User.premium(UserUuid(adminUuid), Username("Admin"), MojangId(adminUuid))
      val sharedPort = 54321
      val ipv4Addr = InetSocketAddress(InetAddress.getByName("192.0.2.1"), sharedPort)
      val ipv6Addr = InetSocketAddress(InetAddress.getByName("2001:db8::1"), sharedPort)

      every { userService.findUserByUsernameIgnoreCase("Admin") } returns adminUser
      every {
        usernameResolutionService.resolveUsernameConflicts(Username("Admin"), adminUser)
      } returns
        UsernameResResult.Success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.NO_CHANGE,
        )

      // Connection 1 (IPv4): creates handshake session with ENFORCE_PREMIUM, then aborts before
      // PostLogin
      val conn1 = mockk<InboundConnection>()
      every { conn1.remoteAddress } returns ipv4Addr
      every { conn1.protocolVersion } returns ProtocolVersion.MINECRAFT_1_21_4
      val preLogin1 = PreLoginEvent(conn1, "Admin")
      listeners.onPreLogin(preLogin1)

      // Connection 2 (IPv6, same source port): must NOT leave PreLogin allowed without online mode,
      // and must NOT authenticate as Admin in PostLogin when offline
      val conn2 = mockk<InboundConnection>()
      every { conn2.remoteAddress } returns ipv6Addr
      every { conn2.protocolVersion } returns ProtocolVersion.MINECRAFT_1_21_4
      val preLogin2 = PreLoginEvent(conn2, "Admin")
      runCatching { listeners.onPreLogin(preLogin2) }

      val offlinePlayer = mockk<Player>(relaxed = true)
      every { offlinePlayer.username } returns "Admin"
      every { offlinePlayer.uniqueId } returns adminUuid
      every { offlinePlayer.remoteAddress } returns ipv6Addr
      every { offlinePlayer.isOnlineMode } returns false
      every { offlinePlayer.isActive } returns true

      val autoLoginSession = mockk<VelocityAutoLoginAuthSession>(relaxed = true)
      every { authSessionFactory.createPremiumAuthSession(any(), any()) } returns autoLoginSession

      listeners.onPostLogin(PostLoginEvent(offlinePlayer))

      verify(exactly = 0) { authSessionFactory.createPremiumAuthSession(any(), any()) }
      verify(exactly = 0) { autoLoginSession.authenticate() }
      verify(atLeast = 1) { offlinePlayer.disconnect(any()) }
    }
  })
