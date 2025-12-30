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

package pl.spcode.navauth.common.application.user

import com.google.inject.Inject
import com.google.inject.Singleton
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.domain.common.TransactionService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword

@Singleton
class UserService
@Inject
constructor(
  val userRepository: UserRepository,
  val userCredentialsService: UserCredentialsService,
  val txService: TransactionService,
) {

  fun findUserByExactUsername(username: String): User? {
    return userRepository.findByExactUsername(username)
  }

  fun findUserByUsernameIgnoreCase(username: String): User? {
    return userRepository.findByUsernameIgnoreCase(username)
  }

  fun storeUserWithCredentials(user: User, password: HashedPassword) {
    txService.inTransaction {
      userRepository.save(user)
      userCredentialsService.storeUserCredentials(UserCredentials.create(user, password))
    }
  }

  fun storePremiumUser(user: User) {
    assert(user.isPremium)

    userRepository.save(user)
  }

  /**
   * Migrates a non-premium user account to a premium account using the provided Mojang ID. This
   * updates the user's data and ensures proper handling of authentication credentials.
   *
   * @param user The non-premium user to be migrated to a premium account.
   * @param mojangId The Mojang ID associated with the premium account to link to the user.
   * @return The updated user with premium account status.
   */
  fun migrateToPremium(user: User, mojangId: MojangId): User {
    return txService.inTransaction {
      val credentials = userCredentialsService.findCredentials(user)!!
      // require credentials only if there's 2FA enabled
      val requireCredentials = credentials.isTwoFactorEnabled
      val premiumUser = User.premium(user.id, user.username, mojangId, requireCredentials)

      // do not delete credentials in case a revert was requested
      userRepository.save(premiumUser)
      return@inTransaction premiumUser
    }
  }

  /**
   * Updates the username of a given user to a new username, ensuring the change is performed within
   * a transactional context and verifying that the new username is not already in use.
   *
   * @param user The user whose username is being updated.
   * @param newUsername The new username to be assigned to the user.
   * @return The updated user with the new username.
   * @throws UsernameAlreadyTakenException if another user already takes the new username.
   * @throws IllegalArgumentException if the new username is the same as the existing username.
   */
  fun migrateUsername(user: User, newUsername: Username): User {
    return txService.inTransaction {
      if (user.username == newUsername)
        throw IllegalArgumentException("username cannot be the same")

      val conflictingUser = userRepository.findByUsernameIgnoreCase(newUsername.value)
      if (conflictingUser != null) throw UsernameAlreadyTakenException("username already taken")

      val newUser = user.withNewUsername(newUsername)
      userRepository.save(newUser)
      return@inTransaction newUser
    }
  }

  fun findUserByMojangUuid(uuid: MojangId): User? {
    return userRepository.findByMojangUuid(uuid)
  }
}
