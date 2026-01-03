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
import pl.spcode.navauth.common.infra.crypto.hasher.Argon2CredentialsHasher
import pl.spcode.navauth.common.infra.crypto.hasher.BCryptCredentialsHasher
import pl.spcode.navauth.common.infra.crypto.hasher.LibreLoginSHACredentialsHasher
import pl.spcode.navauth.common.infra.crypto.hasher.SHACredentialsHasher

@Singleton
class UserCredentialsService
@Inject
constructor(val credentialsRepository: UserCredentialsRepository) {

  fun findCredentials(user: User): UserCredentials? {
    return credentialsRepository.findByUser(user)
  }

  fun storeUserCredentials(user: User, userCredentials: UserCredentials) {
    require(userCredentials.userUuid == user.uuid) {
      "provided credentials do not belong to the provided user"
    }
    require(user.credentialsRequired) {
      "cannot store credentials for a user without credentials required property"
    }

    credentialsRepository.save(userCredentials)
  }

  fun deleteUserCredentials(user: User) {
    credentialsRepository.deleteByUser(user)
  }

  /** @param password the raw (not hashed) password */
  fun verifyPassword(credentials: UserCredentials, password: String): Boolean {

    val verified =
      when (credentials.hashingAlgo) {
        HashingAlgorithm.BCRYPT -> {
          BCryptCredentialsHasher().verify(password, credentials.passwordHash)
        }
        HashingAlgorithm.ARGON2 -> {
          Argon2CredentialsHasher().verify(password, credentials.passwordHash)
        }
        HashingAlgorithm.SHA256,
        HashingAlgorithm.SHA512 -> {
          SHACredentialsHasher().verify(password, credentials.passwordHash)
        }
        HashingAlgorithm.LIBRELOGIN_SHA256,
        HashingAlgorithm.LIBRELOGIN_SHA512 -> {
          LibreLoginSHACredentialsHasher().verify(password, credentials.passwordHash)
        }
      }

    return verified
  }
}
