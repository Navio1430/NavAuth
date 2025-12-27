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

package pl.spcode.navauth.common.infra.crypto.hasher

import java.math.BigInteger
import java.security.MessageDigest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash

class LibreLoginSHACredentialsHasher : CredentialsHasher {

  private val logger: Logger = LoggerFactory.getLogger(LibreLoginSHACredentialsHasher::class.java)

  companion object {
    private val random = java.security.SecureRandom()
  }

  override fun hash(password: String): HashedPassword {
    val baseAlgo = HashingAlgorithm.SHA512
    val salt = randomSalt()
    val plain = plainHash(password, baseAlgo)
    val hash = plainHash(plain + salt, baseAlgo)
    val libreLoginHash = LibreLoginPasswordHash.fromRaw(hash, salt, baseAlgo)
    logger.warn(
      "Generated {} hash which is not considered secure and reliable.",
      HashingAlgorithm.LIBRELOGIN_SHA512.name,
    )
    return HashedPassword(libreLoginHash.toPasswordHash(), HashingAlgorithm.LIBRELOGIN_SHA512)
  }

  override fun verify(password: String, passwordHash: PasswordHash): Boolean {
    val libreLoginHash = LibreLoginPasswordHash.fromPasswordHash(passwordHash)
    val hashedInput =
      plainHash(
        plainHash(password, libreLoginHash.baseAlgorithm) + libreLoginHash.saltHex,
        libreLoginHash.baseAlgorithm,
      )
    return hashedInput == libreLoginHash.hashHex
  }

  private fun randomSalt(): String {
    val bytes = ByteArray(16)
    random.nextBytes(bytes)
    return String.format("%016x", BigInteger(1, bytes))
  }

  private fun plainHash(input: String, baseAlgorithm: HashingAlgorithm): String {
    val inputBytes = input.toByteArray()

    val digest =
      when (baseAlgorithm) {
        HashingAlgorithm.SHA256 -> MessageDigest.getInstance("SHA-256")
        HashingAlgorithm.SHA512 -> MessageDigest.getInstance("SHA-512")
        else -> throw IllegalArgumentException("Unsupported base SHA algorithm: $baseAlgorithm")
      }

    val hashedBytes: ByteArray = digest.digest(inputBytes)
    return String.format("%064x", BigInteger(1, hashedBytes))
  }

  class LibreLoginPasswordHash(
    val hashHex: String,
    val saltHex: String,
    val baseAlgorithm: HashingAlgorithm,
  ) {

    init {
      require(baseAlgorithm == HashingAlgorithm.SHA256 || baseAlgorithm == HashingAlgorithm.SHA512)
    }

    companion object {
      fun fromRaw(
        hashHex: String,
        saltHex: String,
        baseAlgorithm: HashingAlgorithm,
      ): LibreLoginPasswordHash {
        return LibreLoginPasswordHash(hashHex, saltHex, baseAlgorithm)
      }

      fun fromPasswordHash(passwordHash: PasswordHash): LibreLoginPasswordHash {
        val parts = passwordHash.value.split("$")
        require(parts.size == 3) { "Invalid LibreLogin hash format" }

        return fromRaw(parts[2], parts[1], HashingAlgorithm.valueOf(parts[0].uppercase()))
      }
    }

    fun toPasswordHash(): PasswordHash {
      return PasswordHash("${baseAlgorithm.name}$$saltHex$$hashHex")
    }
  }
}
