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
import pl.spcode.navauth.common.infra.crypto.HashedPassword

@Singleton
class UserService
@Inject
constructor(
  val userRepository: UserRepository,
  val userCredentialsService: UserCredentialsService,
  val txService: TransactionService,
) {

  fun findUserByUsername(username: String): User? {
    return userRepository.findByUsername(username)
  }

  fun findUserByUsernameLowercase(username: String): User? {
    return userRepository.findByUsernameLowercase(username.lowercase())
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

  fun migrateToPremium(user: User, mojangId: MojangId) {
    txService.inTransaction {
      val credentials = userCredentialsService.findCredentials(user)!!
      // require credentials only if there's 2FA enabled
      val requireCredentials = credentials.isTwoFactorEnabled
      val premiumUser = User.premium(user.id, user.username, mojangId, requireCredentials)

      // do not delete credentials in case a revert was requested
      userRepository.save(premiumUser)
    }
  }

  fun findUserByMojangUuid(uuid: MojangId): User? {
    return userRepository.findByMojangUuid(uuid)
  }
}
