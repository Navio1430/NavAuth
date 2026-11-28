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

package unit.command

import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.config.TwoFactorAuthConfig
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.TOTP2FA
import pl.spcode.navauth.velocity.application.totp.TotpSetupSessionFactory
import pl.spcode.navauth.velocity.application.totp.TotpSetupSessionService
import pl.spcode.navauth.velocity.command.user.TwoFactorDisableCommand
import pl.spcode.navauth.velocity.command.user.TwoFactorSetupCommand
import pl.spcode.navauth.velocity.multification.VelocityMultification

/** 2FA commands for accounts without a password. */
class TwoFactorCommandsTests :
  FunSpec({
    fun player(uuid: UUID, onlineMode: Boolean = true): Player {
      val player = mockk<Player>(relaxed = true)
      every { player.username } returns "Player"
      every { player.isOnlineMode } returns onlineMode
      every { player.uniqueId } returns uuid
      every { player.getPermissionValue(any()) } returns Tristate.UNDEFINED
      return player
    }

    test("premium account without credentials can start 2FA setup without a password") {
      val uuid = UUID.randomUUID()
      val user = User.premium(UserUuid(uuid), Username("Player"), MojangId(uuid))
      val userService = mockk<UserService>()
      every { userService.findUserByUuid(UserUuid(uuid)) } returns user
      val credentialsService = mockk<UserCredentialsService>()
      every { credentialsService.findCredentials(user) } returns null
      val sessions = TotpSetupSessionService()
      val multification = mockk<VelocityMultification>(relaxed = true)

      val command =
        TwoFactorSetupCommand(
          userService,
          credentialsService,
          TotpSetupSessionFactory(),
          sessions,
          TwoFactorAuthConfig(),
          MessagesConfig(),
          multification,
        )
      sessions.findSession(UserUuid(uuid)).shouldBeNull()

      command.setupTwoFactor(player(uuid), Optional.empty())

      sessions.findSession(UserUuid(uuid)).shouldNotBeNull()
      verify(exactly = 0) { credentialsService.enqueueVerifyPassword(any(), any(), any()) }
      verify { multification.create() }
    }

    test("a connection not authenticated by Mojang cannot start 2FA setup without a password") {
      val uuid = UUID.randomUUID()
      val user = User.premium(UserUuid(uuid), Username("Player"), MojangId(uuid))
      val userService = mockk<UserService>()
      every { userService.findUserByUuid(UserUuid(uuid)) } returns user
      val credentialsService = mockk<UserCredentialsService>()
      every { credentialsService.findCredentials(user) } returns null
      val sessions = TotpSetupSessionService()
      val multification = mockk<VelocityMultification>(relaxed = true)

      val command =
        TwoFactorSetupCommand(
          userService,
          credentialsService,
          TotpSetupSessionFactory(),
          sessions,
          TwoFactorAuthConfig(),
          MessagesConfig(),
          multification,
        )
      sessions.findSession(UserUuid(uuid)).shouldBeNull()

      command.setupTwoFactor(player(uuid, onlineMode = false), Optional.empty())

      sessions.findSession(UserUuid(uuid)).shouldBeNull()
      verify(exactly = 0) { credentialsService.enqueueVerifyPassword(any(), any(), any()) }
    }

    test("cracked account with only 2FA cannot disable it and keeps its credentials") {
      val uuid = UUID.randomUUID()
      val user = User.nonPremium(UserUuid(uuid), Username("Player"))
      val secret = TOTP2FA().generateSecret()
      val userService = mockk<UserService>(relaxed = true)
      every { userService.findUserByUuid(UserUuid(uuid)) } returns user
      val credentialsService = mockk<UserCredentialsService>()
      every { credentialsService.findCredentials(user) } returns
        UserCredentials.create(user, null, secret)
      every { credentialsService.verifyCode(any(), any()) } returns true
      val multification = mockk<VelocityMultification>(relaxed = true)

      TwoFactorDisableCommand(userService, credentialsService, multification)
        .disableTwoFactor(player(uuid), "000000")

      verify(exactly = 0) { userService.disableTwoFactorAuth(any()) }
      verify(exactly = 1) { multification.send(any(), any()) }
    }
  })
