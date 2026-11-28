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

package unit.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import pl.spcode.navauth.common.infra.crypto.TOTP2FA

class TOTP2FATests :
  FunSpec({
    val totp2fa = TOTP2FA()

    test("TOTP2FA generate and verify") {
      val secret = totp2fa.generateSecret()
      val totp = totp2fa.generateTOTP(secret)

      val correctCodeResult = totp2fa.verifyTOTP(secret, totp)
      val incorrectCodeResult = totp2fa.verifyTOTP(secret, "111111")

      correctCodeResult shouldBe true
      incorrectCodeResult shouldBe false
    }

    test("TOTP2FA accepts codes of the previous, current and next step") {
      val secret = totp2fa.generateSecret()
      val now = 1_700_000_015L
      val step = now / 30

      for (offset in -1L..1L) {
        val code = totp2fa.generateTOTP(secret, (step + offset) * 30)
        totp2fa.findMatchingStep(secret, code, timestampSeconds = now) shouldBe step + offset
      }
    }

    test("TOTP2FA refuses codes outside the window") {
      val secret = totp2fa.generateSecret()
      val now = 1_700_000_015L
      val step = now / 30
      val inWindow = (-1L..1L).map { totp2fa.generateTOTP(secret, (step + it) * 30) }.toSet()

      for (offset in listOf(-3L, -2L, 2L, 3L)) {
        val code = totp2fa.generateTOTP(secret, (step + offset) * 30)
        if (code !in inWindow) {
          totp2fa.findMatchingStep(secret, code, timestampSeconds = now) shouldBe null
        }
      }
      totp2fa.findMatchingStep(secret, "", timestampSeconds = now) shouldBe null
      totp2fa.findMatchingStep(secret, "1234567", timestampSeconds = now) shouldBe null
    }
  })
