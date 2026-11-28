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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.credentials.queue.EncryptionTaskAlreadyQueuedException
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.player.PlayerAdapter
import pl.spcode.navauth.common.infra.auth.LoginAuthSession

private const val MAX_LOGIN_ATTEMPTS = 3

private class CountingLoginSession(credentials: UserCredentials, service: UserCredentialsService) :
  LoginAuthSession<PlayerAdapter>(
    mockk<PlayerAdapter>(relaxed = true) {
      every { identifier } returns UUID.randomUUID()
      every { isOnline() } returns true
    },
    credentials,
    service,
    MAX_LOGIN_ATTEMPTS,
    mockk<NavAuthEventBus>(relaxed = true),
  ) {
  val tooManyAttemptsCalls = AtomicInteger()

  override fun onTooManyLoginAttempts() {
    tooManyAttemptsCalls.incrementAndGet()
  }
}

class LoginAuthSessionTests :
  FunSpec({
    fun twoFactorOnlyCredentials(): UserCredentials =
      mockk<UserCredentials> {
        every { isTwoFactorEnabled } returns true
        every { isPasswordRequired } returns false
      }

    test("parallel wrong 2FA codes are verified at most maxLoginAttempts times") {
      val credentials = twoFactorOnlyCredentials()
      val verifications = AtomicInteger()
      val service =
        mockk<UserCredentialsService> {
          every { findUnusedCodeStep(credentials, any()) } answers
            {
              verifications.incrementAndGet()
              Thread.sleep(2)
              null
            }
        }
      val session = CountingLoginSession(credentials, service)

      val threads = 32
      val executor = Executors.newFixedThreadPool(threads)
      val start = CountDownLatch(1)
      val tooManyResults = AtomicInteger()
      try {
        val futures =
          (1..threads).map {
            executor.submit {
              start.await()
              repeat(20) {
                try {
                  val result = session.enqueueAuthTask(null, "000000").get(5, TimeUnit.SECONDS)
                  if (result == LoginAuthSession.AuthTaskResult.FailedTooManyAttempts) {
                    tooManyResults.incrementAndGet()
                  }
                } catch (_: EncryptionTaskAlreadyQueuedException) {
                  // refused while another attempt is verified
                }
              }
            }
          }
        start.countDown()
        futures.forEach { it.get(30, TimeUnit.SECONDS) }
      } finally {
        executor.shutdownNow()
      }

      verifications.get() shouldBeLessThanOrEqual MAX_LOGIN_ATTEMPTS
      session.tooManyAttemptsCalls.get() shouldBe 1
      (tooManyResults.get() >= 1) shouldBe true
    }

    test("a second attempt while one is verified is refused") {
      val credentials = twoFactorOnlyCredentials()
      val entered = CountDownLatch(1)
      val release = CountDownLatch(1)
      val service =
        mockk<UserCredentialsService> {
          every { findUnusedCodeStep(credentials, any()) } answers
            {
              entered.countDown()
              release.await(5, TimeUnit.SECONDS)
              null
            }
        }
      val session = CountingLoginSession(credentials, service)

      val first = Thread { session.enqueueAuthTask(null, "000000") }
      first.start()
      entered.await(5, TimeUnit.SECONDS) shouldBe true

      shouldThrow<EncryptionTaskAlreadyQueuedException> { session.enqueueAuthTask(null, "111111") }

      release.countDown()
      first.join(5000)

      // the attempt slot is free again once the first attempt completed
      session.enqueueAuthTask(null, "222222").get(5, TimeUnit.SECONDS) shouldBe
        LoginAuthSession.AuthTaskResult.WrongCredentials
    }

    test("no code is verified once the attempts are used up") {
      val credentials = twoFactorOnlyCredentials()
      val verifications = AtomicInteger()
      val service =
        mockk<UserCredentialsService> {
          every { findUnusedCodeStep(credentials, any()) } answers
            {
              verifications.incrementAndGet()
              null
            }
        }
      val session = CountingLoginSession(credentials, service)

      repeat(MAX_LOGIN_ATTEMPTS + 2) { session.enqueueAuthTask(null, "000000").get() }

      verifications.get() shouldBe MAX_LOGIN_ATTEMPTS
      session.tooManyAttemptsCalls.get() shouldBe 1
    }

    test("an invalidated session does not verify codes") {
      val credentials = twoFactorOnlyCredentials()
      val verifications = AtomicInteger()
      val service =
        mockk<UserCredentialsService> {
          every { findUnusedCodeStep(credentials, any()) } answers
            {
              verifications.incrementAndGet()
              1L
            }
          every { useCodeStep(credentials, any()) } returns true
        }
      val session = CountingLoginSession(credentials, service)
      session.markInvalidated()

      session.enqueueAuthTask(null, "000000").get() shouldBe
        LoginAuthSession.AuthTaskResult.FailedTooManyAttempts
      verifications.get() shouldBe 0
      session.isAuthenticated shouldBe false
    }
  })
