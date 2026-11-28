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

package unit.credentials

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.util.UUID
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.crypto.TOTP2FA

class UserCredentialsServiceTests :
  FunSpec({
    val totp2fa = TOTP2FA()

    fun service() =
      UserCredentialsService(
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
      )

    fun credentials() =
      UserCredentials.create(UserUuid(UUID.randomUUID()), null, totp2fa.generateSecret())

    test("a TOTP code is accepted once") {
      val service = service()
      val credentials = credentials()
      val code = totp2fa.generateTOTP(credentials.totpSecret!!)

      service.verifyCode(credentials, code) shouldBe true
      service.verifyCode(credentials, code) shouldBe false
    }

    test("a code of an older step is refused after a newer one was accepted") {
      val service = service()
      val credentials = credentials()
      val now = System.currentTimeMillis() / 1000
      val current = totp2fa.generateTOTP(credentials.totpSecret!!, now)
      val previous = totp2fa.generateTOTP(credentials.totpSecret, now - 30)

      service.verifyCode(credentials, current) shouldBe true
      if (previous != current) {
        service.verifyCode(credentials, previous) shouldBe false
      }
    }

    test("accepted codes of one account do not affect another account") {
      val service = service()
      val first = credentials()
      val second = UserCredentials.create(UserUuid(UUID.randomUUID()), null, first.totpSecret)
      val code = totp2fa.generateTOTP(first.totpSecret!!)

      service.verifyCode(first, code) shouldBe true
      service.verifyCode(second, code) shouldBe true
    }

    test("a wrong TOTP code is refused") {
      val service = service()
      val credentials = credentials()
      val code = totp2fa.generateTOTP(credentials.totpSecret!!)
      val wrong = if (code == "000000") "000001" else "000000"

      service.verifyCode(credentials, wrong) shouldBe false
      service.verifyCode(credentials, code) shouldBe true
    }
  })
