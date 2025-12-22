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

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.infra.crypto.CryptoUtils
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.crypto.hasher.SHACredentialsHasher

class SHACredentialsHasherTests {

  private val hasher = SHACredentialsHasher()

  @Test
  fun `hash generates correct SHA512 format`() {
    val password = "password123"
    val hashed = hasher.hash(password)

    assertEquals(HashingAlgorithm.SHA512, hashed.algo)
    val shaHash = hasher.decodeSHAHash(hashed.passwordHash)
    assertEquals(HashingAlgorithm.SHA512, shaHash.algorithm)
    assertEquals(16, shaHash.saltBytes.size)
    assertEquals(64, shaHash.hashBytes.size)

    // Verify format: pbkdf2-sha512${salt}${hash}
    val parts = hashed.passwordHash.value.split("$", limit = 3)
    assertEquals(3, parts.size)
    assertEquals("pbkdf2-sha512", parts[0])
    assertTrue(parts[1].isNotEmpty())
    assertTrue(parts[2].isNotEmpty())
  }

  @Test
  fun `verify correct password SHA512 succeeds`() {
    val password = "password123!"
    val hashed = hasher.hash(password)

    val result = hasher.verify(password, hashed.passwordHash)
    assertTrue(result)
  }

  @Test
  fun `verify incorrect password fails`() {
    val password = "correctPassword"
    val wrongPassword = "wrongPassword"
    val hashed = hasher.hash(password)

    val result = hasher.verify(wrongPassword, hashed.passwordHash)
    assertFalse(result)
  }

  @Test
  fun `decodeSHAHash parses valid SHA512 hash`() {
    val expectedSalt = "deadbeefdeadbeef".toByteArray()
    val expectedHash = ByteArray(64) { 0x42 }
    val encodedSalt = CryptoUtils.base64EncodeToString(expectedSalt)
    val encodedHash = CryptoUtils.base64EncodeToString(expectedHash)
    val testHash = "pbkdf2-sha512$$encodedSalt$$encodedHash"

    val shaHash = hasher.decodeSHAHash(PasswordHash(testHash))

    assertEquals(HashingAlgorithm.SHA512, shaHash.algorithm)
    assertTrue(MessageDigest.isEqual(expectedSalt, shaHash.saltBytes))
    assertTrue(MessageDigest.isEqual(expectedHash, shaHash.hashBytes))
  }

  @Test
  fun `decodeSHAHash parses valid SHA256 hash`() {
    val expectedSalt = "deadbeefdeadbeef".toByteArray()
    val expectedHash = ByteArray(32) { 0x42 }
    val encodedSalt = CryptoUtils.base64EncodeToString(expectedSalt)
    val encodedHash = CryptoUtils.base64EncodeToString(expectedHash)
    val testHash = "pbkdf2-sha256$$encodedSalt$$encodedHash"

    val shaHash = hasher.decodeSHAHash(PasswordHash(testHash))

    assertEquals(HashingAlgorithm.SHA256, shaHash.algorithm)
    assertTrue(MessageDigest.isEqual(expectedSalt, shaHash.saltBytes))
    assertTrue(MessageDigest.isEqual(expectedHash, shaHash.hashBytes))
  }

  @Test
  fun `decodeSHAHash rejects invalid format too few parts`() {
    assertThrows<IllegalArgumentException> {
      hasher.decodeSHAHash(PasswordHash($$"pbkdf2-sha512$saltsalt"))
    }
  }

  @Test
  fun `decodeSHAHash rejects invalid format too many parts`() {
    assertThrows<IllegalArgumentException> {
      hasher.decodeSHAHash(PasswordHash($$"pbkdf2-sha512$salt$hash$extra"))
    }
  }

  @Test
  fun `decodeSHAHash rejects invalid base64`() {
    assertThrows<IllegalArgumentException> {
      hasher.decodeSHAHash(PasswordHash($$"pbkdf2-sha512$invalid64$something"))
    }
  }

  @Test
  fun `decodeSHAHash rejects unknown algorithm`() {
    val salt = CryptoUtils.base64EncodeToString(CryptoUtils.generateBytes(16))
    val hash = CryptoUtils.base64EncodeToString(CryptoUtils.generateBytes(16))

    assertThrows<IllegalArgumentException> {
      hasher.decodeSHAHash(PasswordHash("unknown-algo$$salt$$hash"))
    }
  }

  @Test
  fun `SHAHash constructor rejects unsupported algorithm`() {
    assertThrows<IllegalArgumentException> {
      SHACredentialsHasher.SHAHash(
        algorithm = HashingAlgorithm.ARGON2,
        ByteArray(16),
        ByteArray(64),
      )
    }
  }

  @Test
  fun `multiple hashes same password different salts`() {
    val password = "password123!"
    val hash1 = hasher.hash(password)
    val hash2 = hasher.hash(password)

    assertTrue(hash1.passwordHash.value != hash2.passwordHash.value)

    assertTrue(hasher.verify(password, hash1.passwordHash))
    assertTrue(hasher.verify(password, hash2.passwordHash))
  }

  @Test
  fun `empty password throws in hash`() {
    assertThrows<IllegalArgumentException> { hasher.hash("") }
  }
}
