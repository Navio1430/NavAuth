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
  })
