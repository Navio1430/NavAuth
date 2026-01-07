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

package pl.spcode.navauth.common.application.credentials

import com.google.inject.Inject
import pl.spcode.navauth.common.config.PasswordsConfig
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.infra.crypto.hasher.Argon2CredentialsHasher
import pl.spcode.navauth.common.infra.crypto.hasher.BCryptCredentialsHasher
import pl.spcode.navauth.common.infra.crypto.hasher.CredentialsHasher
import pl.spcode.navauth.common.infra.crypto.hasher.LibreLoginSHACredentialsHasher
import pl.spcode.navauth.common.infra.crypto.hasher.SHACredentialsHasher

class CredentialsHasherFactory @Inject constructor(val passwordsConfig: PasswordsConfig) {

  fun createDefaultHasher(): CredentialsHasher {
    return when (passwordsConfig.hashingAlgorithm) {
      HashingAlgorithm.BCRYPT -> BCryptCredentialsHasher()
      HashingAlgorithm.ARGON2 -> Argon2CredentialsHasher()
      else -> throw IllegalArgumentException()
    }
  }

  fun createHasher(algo: HashingAlgorithm): CredentialsHasher {
    return when (algo) {
      HashingAlgorithm.BCRYPT -> BCryptCredentialsHasher()
      HashingAlgorithm.ARGON2 -> Argon2CredentialsHasher()
      HashingAlgorithm.SHA256,
      HashingAlgorithm.SHA512 -> SHACredentialsHasher()
      HashingAlgorithm.LIBRELOGIN_SHA256,
      HashingAlgorithm.LIBRELOGIN_SHA512 -> LibreLoginSHACredentialsHasher()
    }
  }
}
