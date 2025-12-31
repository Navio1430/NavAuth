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

package unit.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.application.auth.username.UsernameResFailureReason
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.application.auth.username.UsernameResolutionService
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import utils.generateRandomString
import utils.invertCase

class UsernameResolutionServiceTests :
  FunSpec({
    lateinit var mockUserService: UserService
    lateinit var mockProfileService: MojangProfileService
    lateinit var service: UsernameResolutionService

    beforeTest {
      mockUserService = mockk()
      mockProfileService = mockk()
      service = UsernameResolutionService(mockUserService, mockProfileService)
    }

    test("new premium user returns success, premium encryption type") {
      val username = Username(generateRandomString(10))
      val premiumProfile = MojangProfile(MojangId(UUID.randomUUID()), username)
      every { mockProfileService.fetchProfileInfo(username) } returns premiumProfile
      every { mockUserService.findUserByMojangUuid(MojangId(any())) } returns null

      val result = service.resolveUsernameConflicts(username, null)

      result shouldBe
        UsernameResResult.Success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.NEW_ACCOUNT,
        )
    }

    test("new nonpremium user returns success, none encryption type") {
      val username = Username(generateRandomString(10))
      every { mockProfileService.fetchProfileInfo(username) } returns null
      every { mockUserService.findUserByMojangUuid(MojangId(any())) } returns null

      val result = service.resolveUsernameConflicts(username, null)

      result shouldBe
        UsernameResResult.Success(EncryptionType.NONE, PostUsernameResolutionState.NEW_ACCOUNT)
    }

    test("existing premium user same connection username returns success, premium encryption") {
      val username = Username(generateRandomString(10))
      val premiumProfile = MojangProfile(MojangId(UUID.randomUUID()), username)
      every { mockProfileService.fetchProfileInfo(username) } returns premiumProfile
      val existingUser =
        User.premium(UserUuid(premiumProfile.uuid.value), username, premiumProfile.uuid, false)

      val result = service.resolveUsernameConflicts(username, existingUser)

      result shouldBe
        UsernameResResult.Success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.NO_CHANGE,
        )
    }

    test("existing nonpremium user same connection username returns success, none encryption") {
      val username = Username(generateRandomString(10))
      every { mockProfileService.fetchProfileInfo(username) } returns null
      val existingUser = User.nonPremium(UserUuid(UUID.randomUUID()), username)

      val result = service.resolveUsernameConflicts(username, existingUser)

      result shouldBe
        UsernameResResult.Success(EncryptionType.NONE, PostUsernameResolutionState.NO_CHANGE)
    }

    test("premium username conflict with existing nonpremium user returns failure") {
      val username = Username(generateRandomString(10))
      val premiumProfile = MojangProfile(MojangId(UUID.randomUUID()), username)
      every { mockProfileService.fetchProfileInfo(username) } returns premiumProfile
      val existingUser = User.nonPremium(UserUuid(premiumProfile.uuid.value), username)

      val result = service.resolveUsernameConflicts(username, existingUser)

      result shouldBe
        UsernameResResult.Failure(
          UsernameResFailureReason.NonPremiumWithPremiumConflict(username.value)
        )
    }

    test("existing nonpremium user different connection username failure") {
      val username = Username(generateRandomString(10))
      val connUsername = Username(generateRandomString(10))
      every { mockProfileService.fetchProfileInfo(connUsername) } returns null
      val existingUser = User.nonPremium(UserUuid(UUID.randomUUID()), username)

      val result = service.resolveUsernameConflicts(connUsername, existingUser)

      result shouldBe
        UsernameResResult.Failure(
          UsernameResFailureReason.NonPremiumUsernameNotIdentical(existingUser.username.value)
        )
    }

    test("existing premium user different connection username failure") {
      val username = Username(generateRandomString(10))
      val connUsername = Username(generateRandomString(10))
      val premiumProfile = MojangProfile(MojangId(UUID.randomUUID()), username)
      every { mockProfileService.fetchProfileInfo(connUsername) } returns premiumProfile
      every { mockUserService.findUserByMojangUuid(MojangId(any())) } returns null
      val existingUser =
        User.premium(UserUuid(UUID.randomUUID()), username, premiumProfile.uuid, false)

      val result = service.resolveUsernameConflicts(connUsername, existingUser)

      result shouldBe
        UsernameResResult.Failure(
          UsernameResFailureReason.PremiumUsernameNotIdentical(existingUser.username.value)
        )
    }

    fun mockMigrationSetup(
      mojangId: MojangId,
      existingUser: User,
      updatedUsername: Username,
    ): MojangProfile {
      val updatedProfile = MojangProfile(mojangId, updatedUsername)
      every { mockProfileService.fetchProfileInfo(updatedUsername) } returns updatedProfile
      every { mockUserService.findUserByMojangUuid(mojangId) } returns existingUser
      every { mockUserService.migrateUsername(existingUser, updatedUsername) } returns
        existingUser.withNewUsername(updatedUsername)
      return updatedProfile
    }

    test("migrates successfully with existing premium user and different mojang username") {
      val mojangId = MojangId(UUID.randomUUID())
      val existingUser =
        User.premium(UserUuid(UUID.randomUUID()), Username(generateRandomString(10)), mojangId)
      val updatedUsername = Username(generateRandomString(10))
      mockMigrationSetup(mojangId, existingUser, updatedUsername)

      val result = service.resolveUsernameConflicts(updatedUsername, null)

      result shouldBe
        UsernameResResult.Success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
        )
      verify { mockUserService.migrateUsername(existingUser, updatedUsername) }
    }

    test(
      "migrates successfully with existing premium user and different letter case mojang username"
    ) {
      val mojangId = MojangId(UUID.randomUUID())
      val existingUser =
        User.premium(UserUuid(UUID.randomUUID()), Username(generateRandomString(10)), mojangId)
      val updatedUsername = Username(invertCase(existingUser.username.value))
      mockMigrationSetup(mojangId, existingUser, updatedUsername)

      val result = service.resolveUsernameConflicts(updatedUsername, null)

      result shouldBe
        UsernameResResult.Success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
        )
      verify { mockUserService.migrateUsername(existingUser, updatedUsername) }
    }
  })
