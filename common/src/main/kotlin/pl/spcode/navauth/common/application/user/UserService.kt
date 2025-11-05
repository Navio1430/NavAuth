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
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.infra.crypto.HashedPassword

@Singleton
class UserService
@Inject
constructor(
  val userRepository: UserRepository,
  val userCredentialsService: UserCredentialsService,
) {

  fun findUserByUsername(username: String, ignoreCase: Boolean): User? {
    // todo impl ignore case option
    return userRepository.findByUsername(username)
  }

  fun storeUserWithCredentials(user: User, password: HashedPassword) {
    // todo in transaction
    userRepository.save(user)
    userCredentialsService.storeUserCredentials(UserCredentials.create(user, password))
  }

  fun storePremiumUser(user: User) {
    assert(user.isPremium)

    userRepository.save(user)
  }

  fun migrateToPremium(user: User) {
    val premiumUser = User.create(user.uuid!!, user.username, true)
    // todo in transaction
    userCredentialsService.deleteUserCredentials(user)
    userRepository.save(premiumUser)
  }
}
