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

package integration.auth

import com.google.inject.Inject
import extension.app.UsernameResolutionTestExtension
import fake.FakeProfileService
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.application.auth.username.UsernameResFailureReason
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.application.auth.username.UsernameResolutionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import utils.generateRandomString
import utils.invertCase

@ExtendWith(UsernameResolutionTestExtension::class)
class UsernameResolutionServiceIntegrationTests {

  @Inject private lateinit var fakeProfileService: FakeProfileService
  @Inject private lateinit var usernameResolutionService: UsernameResolutionService
  @Inject private lateinit var userService: UserService
  @Inject private lateinit var userRepository: UserRepository

  @BeforeEach
  fun setUp() {
    fakeProfileService.reset()
  }

  @Test
  fun `new premium user returns success premium encryption type`() {
    val username = Username(generateRandomString(10))
    fakeProfileService.addProfile(username, MojangProfile(MojangId(UUID.randomUUID()), username))

    val result = usernameResolutionService.resolveUsernameConflicts(username, null)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.NEW_ACCOUNT,
      ),
      result,
    )
  }

  @Test
  fun `new nonpremium user returns success none encryption type`() {
    val username = Username(generateRandomString(10))

    val result = usernameResolutionService.resolveUsernameConflicts(username, null)

    assertEquals(
      UsernameResResult.Success(EncryptionType.NONE, PostUsernameResolutionState.NEW_ACCOUNT),
      result,
    )
  }

  @Test
  fun `existing premium user same connection username returns success premium encryption`() {
    val username = Username(generateRandomString(10))
    val mojangId = MojangId(UUID.randomUUID())
    val userUuid = UserUuid(UUID.randomUUID())
    userRepository.save(User.premium(userUuid, username, mojangId))
    fakeProfileService.addProfile(username, MojangProfile(mojangId, username))

    val existingUser = userService.findUserByUsernameIgnoreCase(username.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(username, existingUser)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.NO_CHANGE,
      ),
      result,
    )
  }

  @Test
  fun `existing nonpremium user same connection username returns success none encryption`() {
    val username = Username(generateRandomString(10))
    userRepository.save(User.nonPremium(UserUuid(UUID.randomUUID()), username))

    val existingUser = userService.findUserByUsernameIgnoreCase(username.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(username, existingUser)

    assertEquals(
      UsernameResResult.Success(EncryptionType.NONE, PostUsernameResolutionState.NO_CHANGE),
      result,
    )
  }

  @Test
  fun `existing nonpremium user conflict with different case premium username returns failure`() {
    val username = Username(generateRandomString(10))
    val premiumUsername = Username(invertCase(username.value))
    val premiumProfile = MojangProfile(MojangId(UUID.randomUUID()), premiumUsername)
    fakeProfileService.addProfile(username, premiumProfile)
    userRepository.save(User.nonPremium(UserUuid(UUID.randomUUID()), username))

    val existingUser = userService.findUserByUsernameIgnoreCase(username.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(username, existingUser)

    assertEquals(
      UsernameResResult.Failure(
        UsernameResFailureReason.NonPremiumWithPremiumConflict(premiumUsername.value)
      ),
      result,
    )
  }

  @Test
  fun `premium username with existing nonpremium user and same username returns success`() {
    val username = Username(generateRandomString(10))
    fakeProfileService.addProfile(username, MojangProfile(MojangId(UUID.randomUUID()), username))
    userRepository.save(User.nonPremium(UserUuid(UUID.randomUUID()), username))

    val existingUser = userService.findUserByUsernameIgnoreCase(username.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(username, existingUser)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.NONE,
        PostUsernameResolutionState.NONPREMIUM_WITH_SAME_PREMIUM_NICKNAME,
      ),
      result,
    )
  }

  @Test
  fun `existing nonpremium user different connection username failure`() {
    val username = Username(generateRandomString(10))
    val connUsername = Username(generateRandomString(10))
    userRepository.save(User.nonPremium(UserUuid(UUID.randomUUID()), username))

    val existingUser = userService.findUserByUsernameIgnoreCase(username.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(connUsername, existingUser)

    assertEquals(
      UsernameResResult.Failure(
        UsernameResFailureReason.NonPremiumUsernameNotIdentical(username.value)
      ),
      result,
    )
  }

  @Test
  fun `existing premium user different connection username failure`() {
    val username = Username(generateRandomString(10))
    val connUsername = Username(generateRandomString(10))
    val mojangId = MojangId(UUID.randomUUID())
    userRepository.save(User.premium(UserUuid(UUID.randomUUID()), username, mojangId))
    fakeProfileService.addProfile(
      connUsername,
      MojangProfile(MojangId(UUID.randomUUID()), connUsername),
    )

    val existingUser = userService.findUserByUsernameIgnoreCase(username.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(connUsername, existingUser)

    assertEquals(
      UsernameResResult.Failure(
        UsernameResFailureReason.PremiumUsernameNotIdentical(username.value)
      ),
      result,
    )
  }

  @Test
  fun `migrates successfully with existing premium user and different mojang username`() {
    val mojangId = MojangId(UUID.randomUUID())
    val existingUsername = Username(generateRandomString(10))
    val newUsername = Username(generateRandomString(10))
    userRepository.save(User.premium(UserUuid(UUID.randomUUID()), existingUsername, mojangId))
    fakeProfileService.addProfile(newUsername, MojangProfile(mojangId, newUsername))

    val result = usernameResolutionService.resolveUsernameConflicts(newUsername, null)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
      ),
      result,
    )
  }

  @Test
  fun `migrates successfully with existing premium user and different letter case mojang username`() {
    val mojangId = MojangId(UUID.randomUUID())
    val existingUsername = Username(generateRandomString(10))
    val newUsername = Username(invertCase(existingUsername.value))
    userRepository.save(User.premium(UserUuid(UUID.randomUUID()), existingUsername, mojangId))
    fakeProfileService.addProfile(newUsername, MojangProfile(mojangId, newUsername))

    val result = usernameResolutionService.resolveUsernameConflicts(newUsername, null)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
      ),
      result,
    )
  }

  @Test
  fun `existing nonpremium user with premium player who renamed to same name`() {
    val nonPremiumUuid = UserUuid(UUID.randomUUID())
    val username = Username(generateRandomString(10))
    fakeProfileService.addProfile(username, MojangProfile(MojangId(UUID.randomUUID()), username))
    userRepository.save(User.nonPremium(nonPremiumUuid, username))

    val existingUser = userService.findUserByUsernameIgnoreCase(username.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(username, existingUser)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.NONE,
        PostUsernameResolutionState.NONPREMIUM_WITH_SAME_PREMIUM_NICKNAME,
      ),
      result,
    )
  }

  @Test
  fun `premium user already renamed by a concurrent login resolves without failing`() {
    val mojangId = MojangId(UUID.randomUUID())
    val userUuid = UserUuid(UUID.randomUUID())
    val username = Username(generateRandomString(10))
    // State left behind by a login that already committed the rename.
    userRepository.save(User.premium(userUuid, username, mojangId))
    fakeProfileService.addProfile(username, MojangProfile(mojangId, username))

    // The caller looked the user up before that rename was committed, so it hands over a stale
    // null while the row already carries the Mojang username.
    val result = usernameResolutionService.resolveUsernameConflicts(username, null)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
      ),
      result,
    )
    assertEquals(username, userRepository.findByUserUuid(userUuid)!!.username)
  }

  @Test
  fun `concurrent logins of a renamed premium user never fail the resolution`() {
    val executor = Executors.newFixedThreadPool(2)
    try {
      repeat(32) {
        val mojangId = MojangId(UUID.randomUUID())
        val userUuid = UserUuid(UUID.randomUUID())
        val oldUsername = Username(generateRandomString(10))
        val newUsername = Username(generateRandomString(10))
        userRepository.save(User.premium(userUuid, oldUsername, mojangId))
        fakeProfileService.addProfile(newUsername, MojangProfile(mojangId, newUsername))

        // Two PreLoginEvents racing for the same account. Both read the pre-rename state, then
        // the second one resolves after the first has committed the migration, so it works on a
        // stale null while the row already carries the new username. The latch only makes that
        // interleaving deterministic; on a live proxy it is what two reconnects milliseconds
        // apart produce.
        val bothLookedUp = CyclicBarrier(2)
        val firstResolved = CountDownLatch(1)
        val logins =
          (1..2).map { login ->
            executor.submit(
              Callable {
                val existingUser = userService.findUserByUsernameIgnoreCase(newUsername.value)
                bothLookedUp.await(20, TimeUnit.SECONDS)
                if (login == 2) check(firstResolved.await(20, TimeUnit.SECONDS))
                try {
                  usernameResolutionService.resolveUsernameConflicts(newUsername, existingUser)
                } finally {
                  if (login == 1) firstResolved.countDown()
                }
              }
            )
          }

        logins.forEach { login ->
          assertEquals(
            UsernameResResult.Success(
              EncryptionType.ENFORCE_PREMIUM,
              PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
            ),
            login.get(30, TimeUnit.SECONDS),
          )
        }
        assertEquals(newUsername, userRepository.findByUserUuid(userUuid)!!.username)
      }
    } finally {
      executor.shutdownNow()
    }
  }

  @Test
  fun `existing premium user with different case mojang username found by case-insensitive lookup migrates successfully`() {
    val mojangId = MojangId(UUID.randomUUID())
    val existingUsername = Username(generateRandomString(10))
    val newUsername = Username(invertCase(existingUsername.value))
    userRepository.save(User.premium(UserUuid(UUID.randomUUID()), existingUsername, mojangId))
    fakeProfileService.addProfile(newUsername, MojangProfile(mojangId, newUsername))

    val existingUser = userService.findUserByUsernameIgnoreCase(newUsername.value)!!
    val result = usernameResolutionService.resolveUsernameConflicts(newUsername, existingUser)

    assertEquals(
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
      ),
      result,
    )
  }
}
