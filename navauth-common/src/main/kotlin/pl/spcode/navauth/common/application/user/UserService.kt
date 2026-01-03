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
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.domain.common.TransactionService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword

@Singleton
class UserService
@Inject
constructor(
  val userRepository: UserRepository,
  val userCredentialsService: UserCredentialsService,
  val profileService: MojangProfileService,
  val txService: TransactionService,
) {

  fun findUserByExactUsername(username: String): User? {
    return userRepository.findByExactUsername(username)
  }

  fun findUserByUsernameIgnoreCase(username: String): User? {
    return userRepository.findByUsernameIgnoreCase(username)
  }

  fun findUserByUuid(uuid: UserUuid): User? {
    return userRepository.findByUserUuid(uuid)
  }

  fun findUserByMojangUuid(uuid: MojangId): User? {
    return userRepository.findByMojangUuid(uuid)
  }

  fun storeUserWithCredentials(user: User, password: HashedPassword) {
    require(user.credentialsRequired) { "cannot store user without credentials required property" }

    txService.inTransaction {
      userRepository.save(user)
      userCredentialsService.storeUserCredentials(user, UserCredentials.create(user, password))
    }
  }

  fun storePremiumUser(user: User) {
    require(user.isPremium) { "cannot store non-premium user" }

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
      val premiumUser = User.premium(user.uuid, user.username, mojangId, requireCredentials)

      userRepository.save(premiumUser)
      if (requireCredentials) {
        val newCredentials = credentials.withoutPassword()
        userCredentialsService.storeUserCredentials(premiumUser, newCredentials)
      } else {
        userCredentialsService.deleteUserCredentials(user)
      }

      return@inTransaction premiumUser
    }
  }

  /**
   * Migrates a premium user to a non-premium user with updated credentials. This operation ensures
   * the user has the required credentials and updates the user's information in a transactional
   * context.
   *
   * @param user The user to be migrated, which must be a premium user.
   * @param newPassword The new hashed password to set for the user.
   * @return The updated user with non-premium status and required credentials.
   * @throws IllegalArgumentException if the user is already a premium user.
   */
  fun migrateToNonPremium(user: User, newPassword: HashedPassword): User {
    require(user.isPremium) { "cannot migrate non-premium user to non-premium" }

    return txService.inTransaction {
      // make sure the user has credentials required
      val nonPremiumUser = user.toNonPremium()
      userRepository.save(nonPremiumUser)

      val newCredentials =
        userCredentialsService.findCredentials(nonPremiumUser)?.withNewPassword(newPassword)
          ?: UserCredentials.create(nonPremiumUser, newPassword)
      userCredentialsService.storeUserCredentials(nonPremiumUser, newCredentials)

      return@inTransaction nonPremiumUser
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
      return@inTransaction migrateUsernameNoTx(user, newUsername)
    }
  }

  /**
   * Migrates a non-premium user's data to a new username. This operation is performed within a
   * transactional context and ensures that the new username is not associated with a premium user
   * and is available for use.
   *
   * @param user The non-premium user whose data will be migrated.
   * @param newUsername The new username to assign to the user.
   * @return The updated user with the new username.
   * @throws UsernameAlreadyTakenException if another user already takes the new username.
   * @throws IllegalArgumentException If the user is a premium user, or the new username belongs to
   *   a premium profile.
   */
  fun migrateData(user: User, newUsername: Username): User {
    require(!user.isPremium) { "cannot migrate premium user data" }

    profileService.fetchProfileInfo(newUsername)?.let {
      throw IllegalArgumentException("cannot migrate to premium username")
    }

    return txService.inTransaction {
      return@inTransaction migrateUsernameNoTx(user, newUsername)
    }
  }

  /**
   * Updates the username of the given user to a new username without a transactional context. This
   * method checks if the new username conflicts with other existing usernames and ensures it is
   * different from the user's current username.
   *
   * @param user The user whose username is being updated.
   * @param newUsername The new username to be assigned to the user.
   * @return The updated user with the new username.
   * @throws UsernameAlreadyTakenException if another user already takes the new username.
   * @throws IllegalArgumentException if the new username is the same as the existing username.
   */
  private fun migrateUsernameNoTx(user: User, newUsername: Username): User {
    if (user.username == newUsername) throw IllegalArgumentException("username cannot be the same")

    val conflictingUser = userRepository.findByUsernameIgnoreCase(newUsername.value)
    if (conflictingUser != null) throw UsernameAlreadyTakenException("username already taken")

    val newUser = user.withNewUsername(newUsername)
    userRepository.save(newUser)
    return newUser
  }
}
