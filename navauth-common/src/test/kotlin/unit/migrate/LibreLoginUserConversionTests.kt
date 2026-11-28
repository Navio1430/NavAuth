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

package unit.migrate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.migrate.migrator.librelogin.LibreLoginUser
import pl.spcode.navauth.common.migrate.migrator.librelogin.convertLibreLoginUser

/** LibreLogin import rules, including issue #46 (premium players asked for a password). */
class LibreLoginUserConversionTests :
  FunSpec({
    val name = Username("Player")
    val password = HashedPassword(PasswordHash("hash"), HashingAlgorithm.BCRYPT)
    val secret = "JBSWY3DPEHPK3PXP"

    fun libreUser(
      uuid: UUID = UUID.randomUUID(),
      premiumUuid: UUID? = null,
      hasPassword: Boolean = true,
      secret: String? = null,
    ) =
      LibreLoginUser(
        uuid = uuid,
        premiumUuid = premiumUuid,
        passwordHash = if (hasPassword) "hash" else null,
        passwordSalt = if (hasPassword) "salt" else null,
        passwordAlgo = if (hasPassword) "BCrypt-2A" else null,
        lastNickname = name.value,
        secret = secret,
      )

    test("premium player with a stored password logs in without it (#46)") {
      val premiumUuid = UUID.randomUUID()

      val converted = convertLibreLoginUser(libreUser(premiumUuid = premiumUuid), name, password)

      converted.shouldNotBeNull()
      converted.user.isPremium shouldBe true
      converted.user.mojangUuid shouldBe MojangId(premiumUuid)
      converted.user.credentialsRequired shouldBe false
      converted.credentials.shouldBeNull()
    }

    test("premium player with 2FA keeps only the 2FA secret") {
      val lUser = libreUser(premiumUuid = UUID.randomUUID(), secret = secret)

      val converted = convertLibreLoginUser(lUser, name, password)

      converted.shouldNotBeNull()
      converted.user.credentialsRequired shouldBe true
      val credentials = converted.credentials.shouldNotBeNull()
      credentials.hashedPassword.shouldBeNull()
      credentials.totpSecret shouldBe TOTPSecret(secret)
    }

    test("premium player keeps the LibreLogin uuid when it differs from the Mojang uuid") {
      val libreUuid = UUID.randomUUID() // LibreLogin's RANDOM uuid creator
      val premiumUuid = UUID.randomUUID()

      val converted =
        convertLibreLoginUser(libreUser(uuid = libreUuid, premiumUuid = premiumUuid), name, null)

      converted.shouldNotBeNull()
      converted.user.uuid shouldBe UserUuid(libreUuid)
      converted.user.mojangUuid shouldBe MojangId(premiumUuid)
    }

    test("non-premium player keeps password and 2FA") {
      val converted = convertLibreLoginUser(libreUser(secret = secret), name, password)

      converted.shouldNotBeNull()
      converted.user.isPremium shouldBe false
      converted.user.credentialsRequired shouldBe true
      val credentials = converted.credentials.shouldNotBeNull()
      credentials.hashedPassword shouldBe password
      credentials.totpSecret shouldBe TOTPSecret(secret)
    }

    test("non-premium player with only 2FA is imported") {
      val converted =
        convertLibreLoginUser(libreUser(hasPassword = false, secret = secret), name, null)

      converted.shouldNotBeNull()
      converted.credentials.shouldNotBeNull().hashedPassword.shouldBeNull()
    }

    test("non-premium player without password and 2FA is skipped") {
      convertLibreLoginUser(libreUser(hasPassword = false), name, null).shouldBeNull()
    }
  })
