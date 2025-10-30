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

package pl.spcode.navauth.common.application.credentials

import com.google.inject.Inject
import com.google.inject.Singleton
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.crypto.BCryptCredentialsHasher

@Singleton
class CredentialsService @Inject constructor(val credentialsRepository: UserCredentialsRepository) {

  fun findCredentials(user: User): UserCredentials? {
    return credentialsRepository.findById(user.uuid!!)
  }

  /** @param password the raw (not hashed) password */
  fun verifyPassword(credentials: UserCredentials, password: String): Boolean {

    val verified =
      when (credentials.algo) {
        HashingAlgorithm.BCRYPT -> {
          BCryptCredentialsHasher().verify(password, credentials.passwordHash)
        }
      }

    return verified
  }
}
