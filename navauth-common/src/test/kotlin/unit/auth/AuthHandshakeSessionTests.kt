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
import java.util.UUID
import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username

class AuthHandshakeSessionTests :
  FunSpec({
    val owner = MojangId(UUID.randomUUID())
    val premiumUser = User.premium(UserUuid(UUID.randomUUID()), Username("Owner"), owner)

    fun session(user: User?, encryption: EncryptionType) =
      AuthHandshakeSession(user, "Owner", encryption, PostUsernameResolutionState.NO_CHANGE)

    test("stored owner authenticated by Mojang passes") {
      val session = session(premiumUser, EncryptionType.ENFORCE_PREMIUM)

      session.verifyPremiumIdentity(true, owner.value) shouldBe true
      session.premiumIdentityMismatch shouldBe false
    }

    test("another Mojang account is refused and remembered") {
      val session = session(premiumUser, EncryptionType.ENFORCE_PREMIUM)

      session.verifyPremiumIdentity(true, UUID.randomUUID()) shouldBe false
      session.premiumIdentityMismatch shouldBe true
    }

    test("new premium account authenticated by Mojang has nothing to compare") {
      val session = session(null, EncryptionType.ENFORCE_PREMIUM)

      session.verifyPremiumIdentity(true, UUID.randomUUID()) shouldBe true
      session.premiumIdentityMismatch shouldBe false
    }

    test("offline handshake is not checked") {
      val cracked = User.nonPremium(UserUuid(UUID.randomUUID()), Username("Owner"))

      session(cracked, EncryptionType.NONE).verifyPremiumIdentity(false, UUID.randomUUID()) shouldBe
        true
      session(null, EncryptionType.NONE).verifyPremiumIdentity(false, UUID.randomUUID()) shouldBe
        true
    }

    test("non-premium account reached through online mode is refused") {
      val cracked = User.nonPremium(UserUuid(UUID.randomUUID()), Username("Owner"))
      val session = session(cracked, EncryptionType.ENFORCE_PREMIUM)

      session.verifyPremiumIdentity(true, UUID.randomUUID()) shouldBe false
      session.premiumIdentityMismatch shouldBe true
    }

    test("premium handshake of an existing account over an offline connection is refused") {
      val session = session(premiumUser, EncryptionType.ENFORCE_PREMIUM)

      // even the stored Mojang UUID proves nothing without Mojang authentication
      session.verifyPremiumIdentity(false, owner.value) shouldBe false
      session.premiumIdentityMismatch shouldBe true
    }

    test("premium handshake of a new name over an offline connection is refused") {
      val session = session(null, EncryptionType.ENFORCE_PREMIUM)

      session.verifyPremiumIdentity(false, UUID.randomUUID()) shouldBe false
      session.premiumIdentityMismatch shouldBe true
    }
  })
