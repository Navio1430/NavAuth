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

package pl.spcode.navauth.common.infra.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.util.encoders.Base32
import pl.spcode.navauth.common.domain.credentials.TOTPSecret

class TOTP2FA {

  companion object {
    private const val CODE_DIGITS = 6
    private const val TIME_STEP_SECONDS = 30L
    private const val SECRET_SIZE_BYTES = 20 // 160 bits (RFC 4226)
    private const val MOD_DIVISOR = 1_000_000

    private val TIMESTAMP_SECONDS: Long = System.currentTimeMillis() / 1000

    private val secureRandom = SecureRandom()
  }

  fun generateSecret(): TOTPSecret {
    val secret = ByteArray(SECRET_SIZE_BYTES)
    secureRandom.nextBytes(secret)
    return TOTPSecret(String(Base32.encode(secret), StandardCharsets.US_ASCII))
  }

  fun generateTOTP(
    secret: TOTPSecret,
    timestampSeconds: Long = System.currentTimeMillis() / 1000,
  ): String {
    val secretBytes =
      Base32.decode(secret.value.trim().uppercase().toByteArray(StandardCharsets.US_ASCII))

    val timeStep = timestampSeconds / TIME_STEP_SECONDS
    val timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array()

    val hmac: Mac = HMac(SHA1Digest())
    hmac.init(KeyParameter(secretBytes))
    hmac.update(timeBytes, 0, timeBytes.size)

    val hash = ByteArray(hmac.macSize)
    hmac.doFinal(hash, 0)

    val offset = hash.last().toInt() and 0x0F

    val binaryCode =
      ((hash[offset].toInt() and 0x7F) shl 24) or
        ((hash[offset + 1].toInt() and 0xFF) shl 16) or
        ((hash[offset + 2].toInt() and 0xFF) shl 8) or
        (hash[offset + 3].toInt() and 0xFF)

    val otp = binaryCode % MOD_DIVISOR
    return otp.toString().padStart(CODE_DIGITS, '0')
  }

  fun verifyTOTP(secret: TOTPSecret, code: String, window: Int = 1): Boolean {
    val currentStep = TIMESTAMP_SECONDS / TIME_STEP_SECONDS

    for (i in -window..window) {
      val stepTime = (currentStep + i) * TIME_STEP_SECONDS
      if (generateTOTP(secret, stepTime) == code) {
        return true
      }
    }
    return false
  }
}
