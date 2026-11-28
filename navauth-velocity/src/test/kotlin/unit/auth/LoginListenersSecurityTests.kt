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

import com.google.common.base.Ticker
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.api.proxy.InboundConnection
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.util.GameProfile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
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

/**
 * The login listeners must fail closed: a premium handshake is never completed by a connection that
 * Mojang did not authenticate, whatever went wrong before (an exception at PreLogin, a handshake
 * session of another connection, another plugin forcing offline mode).
 */
class LoginListenersSecurityTests :
  FunSpec({
    lateinit var userService: UserService
    lateinit var authHandshakeSessionService: AuthHandshakeSessionService
    lateinit var usernameResolutionService: UsernameResolutionService
    lateinit var invalidSessionCacheService: InvalidSessionCacheService
    lateinit var authSessionFactory: VelocityAuthSessionFactory
    lateinit var autoLoginSession: VelocityAutoLoginAuthSession
    lateinit var listeners: LoginListeners

    val ipv4 = InetSocketAddress(InetAddress.getByName("192.0.2.1"), 54321)
    val ipv6 = InetSocketAddress(InetAddress.getByName("2001:db8::1"), 54321)

    val enforcePremium =
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.NO_CHANGE,
      )

    fun premiumUser(name: String = "Admin"): User {
      val mojangId = UUID.randomUUID()
      return User.premium(UserUuid(mojangId), Username(name), MojangId(mojangId))
    }

    fun connection(address: InetSocketAddress): InboundConnection {
      val connection = mockk<InboundConnection>()
      every { connection.remoteAddress } returns address
      every { connection.protocolVersion } returns ProtocolVersion.MINECRAFT_1_21_4
      return connection
    }

    fun player(
      address: InetSocketAddress,
      onlineMode: Boolean,
      name: String = "Admin",
      uuid: UUID = UUID.randomUUID(),
    ): Player {
      val player = mockk<Player>(relaxed = true)
      every { player.username } returns name
      every { player.uniqueId } returns uuid
      every { player.remoteAddress } returns address
      every { player.isOnlineMode } returns onlineMode
      every { player.isActive } returns true
      return player
    }

    fun handshake(address: InetSocketAddress, user: User?, name: String = "Admin") =
      authHandshakeSessionService.createSession(
        VelocityUniqueSessionId(name, address),
        user,
        name,
        enforcePremium,
      )

    fun premiumResolution(user: User?, name: String = "Admin") {
      every { userService.findUserByUsernameIgnoreCase(name) } returns user
      every { usernameResolutionService.resolveUsernameConflicts(Username(name), user) } returns
        enforcePremium
    }

    fun verifyNoPremiumLogin(player: Player) {
      verify(exactly = 0) { authSessionFactory.createPremiumAuthSession(any(), any()) }
      verify(exactly = 0) { authSessionFactory.createLoginAuthSession(any(), any(), any()) }
      verify(exactly = 0) { autoLoginSession.authenticate() }
      verify(exactly = 0) { userService.storePremiumUser(any()) }
      verify(atLeast = 1) { player.disconnect(any()) }
    }

    fun loginListeners(handshakes: AuthHandshakeSessionService) =
      LoginListeners(
        userService,
        UsernameValidator(UsernamesConfig()),
        handshakes,
        usernameResolutionService,
        invalidSessionCacheService,
        authSessionFactory,
        MessagesConfig(),
        GeneralConfig(),
      )

    beforeTest {
      userService = mockk(relaxed = true)
      every { userService.removeLeftoverPremiumPassword(any()) } answers { firstArg() }
      every { userService.findUserByUuid(any()) } returns null
      authHandshakeSessionService = AuthHandshakeSessionService()
      usernameResolutionService = mockk()
      invalidSessionCacheService = InvalidSessionCacheService(GeneralConfig())
      authSessionFactory = mockk(relaxed = true)
      autoLoginSession = mockk(relaxed = true)
      every { authSessionFactory.createPremiumAuthSession(any(), any()) } returns autoLoginSession
      listeners = loginListeners(authHandshakeSessionService)
    }

    test("session ids of the same name and source port differ by IP address and family") {
      val otherIpv4 = InetSocketAddress(InetAddress.getByName("198.51.100.2"), 54321)

      val fromIpv4 = VelocityUniqueSessionId("Admin", ipv4)
      val fromIpv6 = VelocityUniqueSessionId("Admin", ipv6)
      val fromOtherIpv4 = VelocityUniqueSessionId("Admin", otherIpv4)

      fromIpv4 shouldNotBe fromIpv6
      fromIpv4 shouldNotBe fromOtherIpv4
      fromIpv6 shouldNotBe fromOtherIpv4
      VelocityUniqueSessionId("Admin", ipv6) shouldBe fromIpv6
    }

    test("invalid session cache handles IPv6 connections") {
      val ipv6Connection = connection(ipv6)

      invalidSessionCacheService.isInvalidSessionReconnect("Admin", ipv6Connection).shouldBeFalse()
      invalidSessionCacheService.cachePremiumConnection("Admin", ipv6Connection)

      invalidSessionCacheService.isInvalidSessionReconnect("Admin", ipv6Connection).shouldBeTrue()
      invalidSessionCacheService
        .isInvalidSessionReconnect("Admin", connection(ipv4))
        .shouldBeFalse()
    }

    test("premium name over IPv6 forces online mode") {
      premiumResolution(premiumUser())
      val event = PreLoginEvent(connection(ipv6), "Admin", null)

      listeners.onPreLogin(event)

      event.result.isOnlineModeAllowed.shouldBeTrue()
      authHandshakeSessionService
        .findSession(VelocityUniqueSessionId("Admin", ipv6))
        .shouldNotBeNull()
    }

    test("an exception at PreLogin denies the connection") {
      every { userService.findUserByUsernameIgnoreCase("Admin") } throws
        RuntimeException("database failure")
      val event = PreLoginEvent(connection(ipv4), "Admin", null)

      listeners.onPreLogin(event)

      event.result.isAllowed.shouldBeFalse()
    }

    test("PreLogin drops a handshake session left by an earlier connection with the same id") {
      handshake(ipv4, premiumUser())
      every { userService.findUserByUsernameIgnoreCase("Admin") } throws
        RuntimeException("database failure")

      listeners.onPreLogin(PreLoginEvent(connection(ipv4), "Admin", null))

      authHandshakeSessionService.findSession(VelocityUniqueSessionId("Admin", ipv4)).shouldBeNull()
    }

    test("offline connection with a premium handshake of an existing account is refused") {
      handshake(ipv4, premiumUser())
      val offlinePlayer = player(ipv4, onlineMode = false)

      listeners.onPostLogin(PostLoginEvent(offlinePlayer))

      verifyNoPremiumLogin(offlinePlayer)
    }

    test("offline connection with a premium handshake of a new name creates no account") {
      handshake(ipv4, null)
      val offlinePlayer = player(ipv4, onlineMode = false)

      listeners.onPostLogin(PostLoginEvent(offlinePlayer))

      verifyNoPremiumLogin(offlinePlayer)
    }

    test("offline connection is flagged at GameProfileRequest and denied at LoginEvent") {
      val stored = premiumUser()
      val session = handshake(ipv4, stored)
      val offlineProfile = GameProfile(UUID.randomUUID(), "Admin", emptyList())
      val profileEvent = GameProfileRequestEvent(connection(ipv4), offlineProfile, false)

      listeners.onGameProfile(profileEvent)

      profileEvent.gameProfile.id shouldBe offlineProfile.id
      session.premiumIdentityMismatch.shouldBeTrue()
      val loginEvent = LoginEvent(player(ipv4, onlineMode = false), null)
      listeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeFalse()
    }

    test("a new player whose UUID belongs to an account is denied and gets no auth session") {
      val session =
        authHandshakeSessionService.createSession(
          VelocityUniqueSessionId("Admin", ipv4),
          null,
          "Admin",
          UsernameResResult.Success(EncryptionType.NONE, PostUsernameResolutionState.NO_CHANGE),
        )
      val profile = GameProfile(UUID.randomUUID(), "Admin", emptyList())
      every { userService.findUserByUuid(UserUuid(profile.id)) } returns
        User.nonPremium(UserUuid(profile.id), Username("Renamed"))
      val profileEvent = GameProfileRequestEvent(connection(ipv4), profile, false)

      listeners.onGameProfile(profileEvent)

      session.uuidInUseByAnotherAccount.shouldBeTrue()
      val offlinePlayer = player(ipv4, onlineMode = false, uuid = profile.id)
      val loginEvent = LoginEvent(offlinePlayer, null)
      listeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeFalse()

      listeners.onPostLogin(PostLoginEvent(offlinePlayer))
      verify(exactly = 0) { authSessionFactory.createRegisterAuthSession(any(), any()) }
      verify(atLeast = 1) { offlinePlayer.disconnect(any()) }
    }

    test("a new player whose UUID belongs to no account is not flagged") {
      val session =
        authHandshakeSessionService.createSession(
          VelocityUniqueSessionId("Admin", ipv4),
          null,
          "Admin",
          UsernameResResult.Success(EncryptionType.NONE, PostUsernameResolutionState.NO_CHANGE),
        )
      val profile = GameProfile(UUID.randomUUID(), "Admin", emptyList())

      listeners.onGameProfile(GameProfileRequestEvent(connection(ipv4), profile, false))

      session.uuidInUseByAnotherAccount.shouldBeFalse()
      val loginEvent = LoginEvent(player(ipv4, onlineMode = false, uuid = profile.id), null)
      listeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeTrue()
    }

    test("Mojang UUID other than the stored one keeps its own profile and is denied") {
      val stored = premiumUser()
      val session = handshake(ipv4, stored)
      val otherAccount = GameProfile(UUID.randomUUID(), "Admin", emptyList())
      val profileEvent = GameProfileRequestEvent(connection(ipv4), otherAccount, true)

      listeners.onGameProfile(profileEvent)

      profileEvent.gameProfile.id shouldBe otherAccount.id
      session.premiumIdentityMismatch.shouldBeTrue()

      val onlinePlayer = player(ipv4, onlineMode = true, uuid = otherAccount.id)
      val loginEvent = LoginEvent(onlinePlayer, null)
      listeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeFalse()

      listeners.onPostLogin(PostLoginEvent(onlinePlayer))
      verifyNoPremiumLogin(onlinePlayer)
    }

    test("stored owner authenticated by Mojang gets the stored identity") {
      val stored = premiumUser()
      handshake(ipv4, stored)
      val owner = GameProfile(stored.mojangUuid!!.value, "Admin", emptyList())
      val profileEvent = GameProfileRequestEvent(connection(ipv4), owner, true)

      listeners.onGameProfile(profileEvent)

      profileEvent.gameProfile.id shouldBe stored.uuid.value
      val ownerPlayer = player(ipv4, onlineMode = true, uuid = stored.uuid.value)
      val loginEvent = LoginEvent(ownerPlayer, null)
      listeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeTrue()

      listeners.onPostLogin(PostLoginEvent(ownerPlayer))
      verify(exactly = 1) { autoLoginSession.authenticate() }
    }

    test("missing handshake session: GameProfileRequest does not throw, LoginEvent denies") {
      val profile = GameProfile(UUID.randomUUID(), "Admin", emptyList())

      listeners.onGameProfile(GameProfileRequestEvent(connection(ipv4), profile, true))

      val loginEvent = LoginEvent(player(ipv4, onlineMode = true), null)
      listeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeFalse()
    }

    test("an exception at GameProfileRequest drops the handshake session, at LoginEvent denies") {
      val failingHandshakes = mockk<AuthHandshakeSessionService>(relaxed = true)
      every { failingHandshakes.findSession(any()) } throws RuntimeException("unexpected")
      val failingListeners = loginListeners(failingHandshakes)
      val profile = GameProfile(UUID.randomUUID(), "Admin", emptyList())

      failingListeners.onGameProfile(GameProfileRequestEvent(connection(ipv4), profile, true))
      verify { failingHandshakes.closeSession(VelocityUniqueSessionId("Admin", ipv4)) }

      val loginEvent = LoginEvent(player(ipv4, onlineMode = true), null)
      failingListeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeFalse()
    }

    test("an exception at PostLogin disconnects the player") {
      handshake(ipv4, null)
      every { authSessionFactory.createPremiumAuthSession(any(), any()) } throws
        RuntimeException("unexpected")
      val onlinePlayer = player(ipv4, onlineMode = true)

      listeners.onPostLogin(PostLoginEvent(onlinePlayer))

      verify(atLeast = 1) { onlinePlayer.disconnect(any()) }
      authHandshakeSessionService.findSession(VelocityUniqueSessionId(onlinePlayer)).shouldBeNull()
    }

    test("connections sharing a source port never share a premium login") {
      val stored = premiumUser()
      premiumResolution(stored)

      val firstPreLogin = PreLoginEvent(connection(ipv4), "Admin", null)
      listeners.onPreLogin(firstPreLogin)
      firstPreLogin.result.isOnlineModeAllowed.shouldBeTrue()

      val secondPreLogin = PreLoginEvent(connection(ipv6), "Admin", null)
      listeners.onPreLogin(secondPreLogin)
      secondPreLogin.result.isOnlineModeAllowed.shouldBeTrue()

      val offlineProfile = GameProfile(UUID.randomUUID(), "Admin", emptyList())
      listeners.onGameProfile(GameProfileRequestEvent(connection(ipv6), offlineProfile, false))
      val offlinePlayer = player(ipv6, onlineMode = false, uuid = offlineProfile.id)
      val loginEvent = LoginEvent(offlinePlayer, null)
      listeners.onLogin(loginEvent)
      loginEvent.result.isAllowed.shouldBeFalse()

      listeners.onPostLogin(PostLoginEvent(offlinePlayer))
      verifyNoPremiumLogin(offlinePlayer)
    }

    test("LoginEvent runs last, so no later plugin can allow a connection NavAuth denied") {
      val onLogin =
        LoginListeners::class
          .java
          .getMethod("onLogin", LoginEvent::class.java)
          .getAnnotation(Subscribe::class.java)
      val onPostLogin =
        LoginListeners::class
          .java
          .getMethod("onPostLogin", PostLoginEvent::class.java)
          .getAnnotation(Subscribe::class.java)

      onLogin.priority shouldBe Short.MIN_VALUE
      @Suppress("DEPRECATION") onLogin.order shouldBe PostOrder.NORMAL
      @Suppress("DEPRECATION") onPostLogin.order shouldBe PostOrder.FIRST
    }

    test("a connection another plugin denied before LoginEvent stays denied") {
      handshake(ipv4, premiumUser())
      val reason = Component.text("denied by another plugin")
      val loginEvent = LoginEvent(player(ipv4, onlineMode = true), null)
      loginEvent.result = ResultedEvent.ComponentResult.denied(reason)

      listeners.onLogin(loginEvent)

      loginEvent.result.isAllowed.shouldBeFalse()
      loginEvent.result.reasonComponent.get() shouldBe reason
    }

    test("Mojang authentication slower than the 15 s handshake session is denied at LoginEvent") {
      val clock = AtomicLong()
      val ticker =
        object : Ticker() {
          override fun read(): Long = clock.get()
        }
      val handshakes = AuthHandshakeSessionService(ticker)
      val slowListeners = loginListeners(handshakes)
      handshakes.createSession(
        VelocityUniqueSessionId("Admin", ipv4),
        null,
        "Admin",
        enforcePremium,
      )

      clock.addAndGet(Duration.ofSeconds(15).toNanos())
      val loginEvent = LoginEvent(player(ipv4, onlineMode = true), null)
      slowListeners.onLogin(loginEvent)

      loginEvent.result.isAllowed.shouldBeFalse()
      (loginEvent.result.reasonComponent.get() as TextComponent).content() shouldBe
        "NavAuth: Auth session expired, please try again"
    }
  })
