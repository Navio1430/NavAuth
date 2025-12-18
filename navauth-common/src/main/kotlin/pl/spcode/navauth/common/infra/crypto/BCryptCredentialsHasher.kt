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

package pl.spcode.navauth.common.infra.crypto

import at.favre.lib.crypto.bcrypt.BCrypt
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm

class BCryptCredentialsHasher : CredentialsHasher {

  companion object {
    private val hasher: BCrypt.Hasher = BCrypt.withDefaults()
    private val verifier: BCrypt.Verifyer = BCrypt.verifyer()
  }

  override fun hash(password: String): HashedPassword {
    val hash: String = hasher.hashToString(10, password.toCharArray())
    return HashedPassword(PasswordHash(hash), HashingAlgorithm.BCRYPT)
  }

  override fun verify(password: String, passwordHash: PasswordHash): Boolean {
    return verifier.verify(password.toByteArray(), passwordHash.value.toByteArray()).verified
  }
}
