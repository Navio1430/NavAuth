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

package unit.validator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import pl.spcode.navauth.common.application.validator.PasswordValidator
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.application.validator.ValidationException
import pl.spcode.navauth.common.config.PasswordsConfig
import pl.spcode.navauth.common.config.UsernamesConfig

class UsernameValidatorTests :
  FunSpec({
    val validator = UsernameValidator(UsernamesConfig())

    test("Minecraft names of 3 to 16 letters, digits and underscores are valid") {
      listOf("abc", "Notch", "a_b_c", "Player_1234567890".take(16), "___").forEach {
        validator.isValid(it) shouldBe true
      }
    }

    test("too short, too long, spaces, dashes and non-ASCII names are invalid") {
      listOf("ab", "a".repeat(17), "with space", "dash-name", "żółw", "", ".Bedrock").forEach {
        validator.isValid(it) shouldBe false
      }
    }

    test("validate throws for an invalid name") {
      shouldThrow<ValidationException> { validator.validate("no spaces allowed") }
    }

    test("an invalid configured pattern falls back to the Minecraft pattern") {
      val config = UsernamesConfig().apply { usernamePattern = "([unclosed" }
      val fallback = UsernameValidator(config)

      fallback.isValid("Notch") shouldBe true
      fallback.isValid("ab") shouldBe false
    }
  })

class PasswordValidatorTests :
  FunSpec({
    test("default rules: at least 5 characters with a lowercase letter and a digit") {
      val validator = PasswordValidator(PasswordsConfig())

      validator.isValid("abcd1") shouldBe true
      validator.isValid("abc1") shouldBe false // too short
      validator.isValid("ABCD1") shouldBe false // no lowercase
      validator.isValid("abcde") shouldBe false // no digit
    }

    test("whitespace is always rejected") {
      val validator = PasswordValidator(PasswordsConfig())

      validator.isValid("abcd 1") shouldBe false
      validator.isValid("abcd1\t") shouldBe false
    }

    test("optional uppercase and special character rules") {
      val config =
        PasswordsConfig().apply {
          requireUppercase = true
          requireSpecial = true
        }
      val validator = PasswordValidator(config)

      validator.isValid("abcd1") shouldBe false
      validator.isValid("Abcd1") shouldBe false
      validator.isValid("Abcd1!") shouldBe true
    }
  })
