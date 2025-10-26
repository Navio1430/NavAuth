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
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.crypto.BCryptCredentialsHasher

class CredentialsService @Inject constructor(val credentialsRepository: UserCredentialsRepository) {

  /**
   * @param user user who should be verified
   * @param password raw, not hashed password
   */
  fun verifyCredentials(user: User, password: String): Boolean {

    val credentials =
      credentialsRepository.findById(user.uuid!!)
        ?: // todo: throw exception, credentials not found but tried to verify
        return false

    val verified =
      when (credentials.algo) {
        HashingAlgorithm.BCRYPT -> {
          BCryptCredentialsHasher().verify(password, credentials.passwordHash)
        }
      }

    return verified
  }
}
