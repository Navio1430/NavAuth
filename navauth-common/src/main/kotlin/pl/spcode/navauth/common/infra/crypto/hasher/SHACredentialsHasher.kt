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

import java.security.MessageDigest
import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.infra.crypto.CryptoUtils
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash

class SHACredentialsHasher : CredentialsHasher {

  private companion object {
    const val ITERATIONS = 100_000
    const val SALT_LENGTH = 16
    const val HASH_LENGTH_256 = 32
    const val HASH_LENGTH_512 = 64
    const val PBKDF2_SHA256 = "pbkdf2-sha256"
    const val PBKDF2_SHA512 = "pbkdf2-sha512"
  }

  /**
   * Generates a salted and hashed representation of the given password using the PBKDF2 algorithm.
   *
   * @param password The plain text password to be hashed.
   * @return A `HashedPassword` object containing the hashed password, salt and the hashing
   *   algorithm identifier.
   */
  override fun hash(password: String): HashedPassword {
    val saltBytes = CryptoUtils.generateBytes(SALT_LENGTH)
    val defaultAlgo = HashingAlgorithm.SHA512

    val hashBytes = generatePBKDF2Hash(password.toCharArray(), saltBytes, defaultAlgo)

    val encoded = encodeSHAHash(SHAHash(defaultAlgo, saltBytes, hashBytes))
    return HashedPassword(PasswordHash(encoded), defaultAlgo)
  }

  private fun getAlgoIdentifier(algo: HashingAlgorithm): String =
    when (algo) {
      HashingAlgorithm.SHA256 -> PBKDF2_SHA256
      HashingAlgorithm.SHA512 -> PBKDF2_SHA512
      else -> throw IllegalArgumentException("unsupported SHA algorithm: $algo")
    }

  /**
   * @param password the plain text password to verify
   * @param passwordHash the hashed password to compare against, required format:
   *   {identifier}${b64salt}${b64hash}
   * @return true if the password matches the hash, false otherwise
   */
  override fun verify(password: String, passwordHash: PasswordHash): Boolean {
    val shaHash = decodeSHAHash(passwordHash)

    val recomputedHash =
      generatePBKDF2Hash(password.toCharArray(), shaHash.saltBytes, shaHash.algorithm)

    return MessageDigest.isEqual(recomputedHash, shaHash.hashBytes)
  }

  private fun generatePBKDF2Hash(
    password: CharArray,
    salt: ByteArray,
    algo: HashingAlgorithm,
  ): ByteArray {
    val (targetLength, digest) =
      when (algo) {
        HashingAlgorithm.SHA256 -> Pair(HASH_LENGTH_256, SHA256Digest())
        HashingAlgorithm.SHA512 -> Pair(HASH_LENGTH_512, SHA512Digest())
        else -> throw IllegalArgumentException("Unsupported SHA algorithm: $algo")
      }

    val generator = PKCS5S2ParametersGenerator(digest)
    generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(password), salt, ITERATIONS)

    val keyParam = generator.generateDerivedParameters(targetLength * 8) as KeyParameter
    return keyParam.key
  }

  private fun encodeSHAHash(hash: SHAHash): String {
    val encodedSalt = CryptoUtils.base64EncodeToString(hash.saltBytes)
    val encodedHash = CryptoUtils.base64EncodeToString(hash.hashBytes)
    val identifier = getAlgoIdentifier(hash.algorithm)

    return $$"$$identifier$$$encodedSalt$$$encodedHash"
  }

  fun decodeSHAHash(passwordHash: PasswordHash): SHAHash {
    val parts = passwordHash.value.split("$", limit = 3)
    require(parts.size == 3) { "Invalid hash format" }

    val algoRaw = parts[0]
    val salt = CryptoUtils.base64DecodeFromString(parts[1])
    val hash = CryptoUtils.base64DecodeFromString(parts[2])

    val algo =
      when (algoRaw) {
        PBKDF2_SHA256 -> HashingAlgorithm.SHA256
        PBKDF2_SHA512 -> HashingAlgorithm.SHA512
        else -> throw IllegalArgumentException("Unsupported SHA algorithm: $algoRaw")
      }

    return SHAHash(algo, salt, hash)
  }

  @Suppress("ArrayInDataClass")
  data class SHAHash(
    val algorithm: HashingAlgorithm,
    val saltBytes: ByteArray,
    val hashBytes: ByteArray,
  ) {
    init {
      require(algorithm == HashingAlgorithm.SHA256 || algorithm == HashingAlgorithm.SHA512) {
        "Unsupported SHA algorithm: $algorithm"
      }
    }
  }
}
