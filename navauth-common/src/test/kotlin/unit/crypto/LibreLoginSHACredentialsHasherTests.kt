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

package unit.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.crypto.hasher.LibreLoginSHACredentialsHasher

class LibreLoginSHACredentialsHasherTests {

  val hasher = LibreLoginSHACredentialsHasher()

  @Test
  fun `verify LibreLogin SHA512 hash succeeds`() {
    val algoRaw = "sha512"
    val hashRaw =
      "8ee118450f375a778d7db5d75ede9e0f278c3d13f39a885a613f247155c8feacaca533a10db20528ca52386288629a70f3ab61be29db8e48d190a9e3bae51850"
    val saltRaw = "e690eaed7726e456d3dd494c4cb51117"
    val passwordHash = PasswordHash("$algoRaw$$saltRaw$$hashRaw")
    val result = hasher.verify("dadupa123", passwordHash)

    assertTrue(result)
  }

  @Test
  fun `hash and verify should succeed for correct password`() {
    val password = "SecretPassword123!"

    val hashed = hasher.hash(password)

    assertTrue(hasher.verify(password, hashed.passwordHash), "Password should verify correctly")
  }

  @Test
  fun `verify should fail for wrong password`() {
    val password = "SecretPassword123!"
    val wrongPassword = "SecretPassword123?"
    val hashed = hasher.hash(password)

    val result = hasher.verify(wrongPassword, hashed.passwordHash)

    assertFalse(result, "Wrong password must not verify")
  }

  @Test
  fun `hash format should be parseable by LibreLoginPasswordHash`() {
    val password = "AnotherPassword!"

    val hashed = hasher.hash(password)
    val libre =
      LibreLoginSHACredentialsHasher.LibreLoginPasswordHash.fromPasswordHash(hashed.passwordHash)

    assertEquals(HashingAlgorithm.LIBRELOGIN_SHA512, hashed.algo)
    assertEquals(HashingAlgorithm.SHA512, libre.baseAlgorithm)
    assertTrue(libre.hashHex.matches(Regex("^[0-9a-f]+$")), "Hash must be hex")
    assertTrue(libre.saltHex.matches(Regex("^[0-9a-f]+$")), "Salt must be hex")
  }

  @Test
  fun `toPasswordHash should preserve data`() {
    val hashHex = "a".repeat(128) // 512-bit hash as hex
    val saltHex = "b".repeat(16)
    val libre =
      LibreLoginSHACredentialsHasher.LibreLoginPasswordHash.fromRaw(
        hashHex = hashHex,
        saltHex = saltHex,
        baseAlgorithm = HashingAlgorithm.SHA512,
      )

    val passwordHash: PasswordHash = libre.toPasswordHash()
    val parsed =
      LibreLoginSHACredentialsHasher.LibreLoginPasswordHash.fromPasswordHash(passwordHash)

    assertEquals(hashHex, parsed.hashHex)
    assertEquals(saltHex, parsed.saltHex)
    assertEquals(HashingAlgorithm.SHA512, parsed.baseAlgorithm)
  }

  @Test
  fun `fromPasswordHash should reject invalid format`() {
    // too few parts
    val invalid1 = PasswordHash($$"SHA512$onlyTwoParts")
    // too many parts
    val invalid2 = PasswordHash($$"SHA512$foo$bar$baz")

    assertThrows<IllegalArgumentException> {
      LibreLoginSHACredentialsHasher.LibreLoginPasswordHash.fromPasswordHash(invalid1)
    }
    assertThrows<IllegalArgumentException> {
      LibreLoginSHACredentialsHasher.LibreLoginPasswordHash.fromPasswordHash(invalid2)
    }
  }

  @Test
  fun `LibreLoginPasswordHash should reject unsupported algorithms`() {
    // given an enum that is not SHA256 or SHA512
    // Assuming your HashingAlgorithm has some other value like PLAINTEXT or LIBRELOGIN_SHA512
    assertThrows<IllegalArgumentException> {
      LibreLoginSHACredentialsHasher.LibreLoginPasswordHash.fromRaw(
        hashHex = "a".repeat(64),
        saltHex = "b".repeat(16),
        baseAlgorithm =
          HashingAlgorithm.LIBRELOGIN_SHA512, // intentionally invalid for baseAlgorithm
      )
    }
  }

  @Test
  fun `hash should use different salts for different calls`() {
    val password = "SamePassword"

    val hash1 = hasher.hash(password)
    val hash2 = hasher.hash(password)

    assertNotEquals(
      hash1.passwordHash,
      hash2.passwordHash,
      "Hashes for same password should differ due to random salt",
    )
    assertTrue(hasher.verify(password, hash1.passwordHash))
    assertTrue(hasher.verify(password, hash2.passwordHash))
  }
}
