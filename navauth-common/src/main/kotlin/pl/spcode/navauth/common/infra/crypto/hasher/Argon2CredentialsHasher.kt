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

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.infra.crypto.CryptoUtils
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash

class Argon2CredentialsHasher : CredentialsHasher {

  companion object {
    private val random = SecureRandom()
    private const val SALT_LENGTH = 16
    private const val HASH_LENGTH = 16
    private const val MEMORY_KB = 65536 // 64MiB
    private const val ITERATIONS = 3
    private const val PARALLELISM = 4

    private fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH).apply { random.nextBytes(this) }
  }

  override fun hash(password: String): HashedPassword {
    val salt = generateSalt()
    val params =
      Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
        .withSalt(salt)
        .withIterations(ITERATIONS)
        .withMemoryAsKB(MEMORY_KB)
        .withParallelism(PARALLELISM)
        .build()

    val generator = Argon2BytesGenerator()
    generator.init(params)

    val passwordBytes = password.toByteArray(StandardCharsets.UTF_8)
    val hash = ByteArray(HASH_LENGTH)
    generator.generateBytes(passwordBytes, hash)

    val saltB64 = CryptoUtils.base64EncodeToString(salt)
    val hashB64 = CryptoUtils.base64EncodeToString(hash)

    // PHC-style: $argon2id$v=19$m=65536,t=3,p=4$<salt_b64>$<hash_b64>
    val encoded = buildString {
      append($$"$argon2id")
      append($$"$v=19")
      append($$"$m=").append(MEMORY_KB)
      append(",t=").append(ITERATIONS)
      append(",p=").append(PARALLELISM)
      append("$").append(saltB64)
      append("$").append(hashB64)
    }

    return HashedPassword(PasswordHash(encoded), HashingAlgorithm.ARGON2)
  }

  override fun verify(password: String, passwordHash: PasswordHash): Boolean {
    val parsed = parsePhcString(passwordHash.value)

    val params =
      Argon2Parameters.Builder(parsed.type)
        .withVersion(parsed.version)
        .withMemoryAsKB(parsed.memoryKb)
        .withIterations(parsed.iterations)
        .withParallelism(parsed.parallelism)
        .withSalt(parsed.salt)
        .build()

    val generator = Argon2BytesGenerator()
    generator.init(params)

    val pwdBytes = password.toByteArray(Charsets.UTF_8)
    val testHash = ByteArray(parsed.hash.size)
    generator.generateBytes(pwdBytes, testHash)

    return MessageDigest.isEqual(parsed.hash, testHash)
  }

  @Suppress("ArrayInDataClass")
  data class ParsedArgon2(
    val type: Int,
    val version: Int,
    val memoryKb: Int,
    val iterations: Int,
    val parallelism: Int,
    val salt: ByteArray,
    val hash: ByteArray,
  )

  private fun parsePhcString(encoded: String): ParsedArgon2 {
    // Example: $argon2id$v=19$m=65536,t=3,p=4$<salt>$<hash>
    val parts = encoded.split("$")
    require(parts.size == 6) { "Invalid Argon2 format" }

    val type =
      when (parts[1]) {
        "argon2id" -> Argon2Parameters.ARGON2_id
        "argon2i" -> Argon2Parameters.ARGON2_i
        "argon2d" -> Argon2Parameters.ARGON2_d
        else -> error("Unsupported Argon2 type: ${parts[1]}")
      }

    val version = parts[2].removePrefix("v=").toInt()

    val paramMap =
      parts[3].split(",").associate {
        val kv = it.split("=")
        kv[0] to kv[1].toInt()
      }

    val memoryKb = paramMap["m"] ?: error("Missing m")
    val iterations = paramMap["t"] ?: error("Missing t")
    val parallelism = paramMap["p"] ?: error("Missing p")

    val salt = CryptoUtils.base64DecodeFromString(parts[4])
    val hash = CryptoUtils.base64DecodeFromString(parts[5])

    return ParsedArgon2(type, version, memoryKb, iterations, parallelism, salt, hash)
  }
}
