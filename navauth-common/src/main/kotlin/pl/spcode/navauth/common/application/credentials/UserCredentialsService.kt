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
import pl.spcode.navauth.common.domain.common.TransactionService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.infra.crypto.TOTP2FA

@Singleton
class UserCredentialsService
@Inject
constructor(
  val credentialsRepository: UserCredentialsRepository,
  val transactionService: TransactionService,
  val credentialsHasherFactory: CredentialsHasherFactory,
) {

  fun findCredentials(user: User): UserCredentials? {
    return credentialsRepository.findByUser(user)
  }

  /**
   * Stores the user credentials after validation.
   *
   * @param user the user to whom the provided credentials belong
   * @param userCredentials the credentials to be stored for the user
   * @throws IllegalArgumentException if the provided credentials do not match the user or if the
   *   user does not require credentials
   */
  fun storeUserCredentials(user: User, userCredentials: UserCredentials) {
    require(userCredentials.userUuid == user.uuid) {
      "provided credentials do not belong to the provided user"
    }
    require(user.credentialsRequired) {
      "cannot store credentials for a user without credentials required property"
    }

    credentialsRepository.save(userCredentials)
  }

  /**
   * Deletes the credentials associated with the given user.
   *
   * @param user the user whose credentials are to be deleted
   * @throws IllegalArgumentException if the user has the `credentialsRequired` property set to true
   */
  fun deleteUserCredentials(user: User) {
    require(!user.credentialsRequired) {
      "cannot delete credentials for a user with credentials required property"
    }
    credentialsRepository.deleteByUser(user)
  }

  /** @param password the raw (not hashed) password */
  fun verifyPassword(credentials: UserCredentials, password: String): Boolean {
    require(credentials.hashedPassword != null) { "credentials do not have a password hash" }

    val passwordHash = credentials.hashedPassword.passwordHash
    val hasher = credentialsHasherFactory.createHasher(credentials.hashedPassword.algo)

    return hasher.verify(password, passwordHash)
  }

  fun verifyCode(credentials: UserCredentials, code: String): Boolean {
    require(credentials.totpSecret != null) { "credentials do not have a TOTP secret" }
    return TOTP2FA().verifyTOTP(credentials.totpSecret, code)
  }

  /**
   * Updates the password for a given user to a new hashed password. Validates that the user has
   * existing credentials before updating.
   *
   * @param user the user whose password needs to be updated
   * @param newPassword the new raw password to be hashed and stored
   */
  fun updatePassword(user: User, newPassword: String) {
    transactionService.inTransaction {
      val credentials = findCredentials(user)
      require(credentials != null) { "user does not have credentials" }

      val hasher = credentialsHasherFactory.createDefaultHasher()
      val hashedPassword = hasher.hash(newPassword)
      val newCredentials = credentials.withNewPassword(hashedPassword)
      storeUserCredentials(user, newCredentials)
    }
  }
}
