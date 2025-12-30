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

package pl.spcode.navauth.common.application.auth.handshake

import com.google.inject.Inject
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.auth.handshake.PreAuthUsernameState
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.Username

class PreAuthenticatedUserService
@Inject
constructor(val userService: UserService, val profileService: MojangProfileService) {

//  /** @param connectionUsername initial connection username */
//  fun validateUsername(connectionUsername: String): Boolean {
//    // todo check regex
//    return true
//  }
//
//  fun getPreAuthUserState(connUsername: Username): PreAuthUserState {
//    val user = userService.findUserByUsernameIgnoreCase(connUsername.value)
//
//    var existingUser = userService.findUserByUsernameIgnoreCase(connUsername.value)
//    val userExists = existingUser != null
//    val correspondingPremiumProfile = profileService.fetchProfileInfo(connUsername)
//    val isPremiumNickname = correspondingPremiumProfile != null
//
//    val state: PreAuthUsernameState = return PreAuthUserState(getUsernameState())
//  }
//
//  private fun getUsernameState(connUsername: String, existingUser: User?): PreAuthUsernameState {
//    if (existingUser != null) {
//      if (existingUser.isPremium) {
//        // user could change 1 letter to be uppercased/lowercased in their nickname
//        if (connUsername != existingUser.username.value) {
//          // let them through and make data migration later after auth (NOT BEFORE)
//          return PreAuthUsernameState.POSSIBLE_PREMIUM_USERNAME_CHANGE
//        }
//      }
//      // non premium user
//      else {
//        if (isPremiumNickname) {
//          if (correspondingPremiumProfile.name == existingUser.username.value) {
//            session.usernameState = PreAuthUsernameState.USERNAME_POTENTIAL_CONFLICT
//            if (connUsername != correspondingPremiumProfile.name) {
//              event.result =
//                usernameRequiredDeniedResult(connUsername, correspondingPremiumProfile.name)
//              return
//            }
//          } else {
//            // todo refactor conflicts
//            session.usernameState = PreAuthUsernameState.USERNAME_CONFLICT
//            event.result = usernameConflictDeniedResult(correspondingPremiumProfile.name)
//            return
//          }
//        }
//        // not a premium nickname
//        else {
//          if (connUsername != existingUser.username.value) {
//            event.result = usernameRequiredDeniedResult(connUsername, existingUser.username.value)
//            return
//          }
//        }
//      }
//    }
//    // user doesn't exist yet
//    else {
//      if (isPremiumNickname) {
//        if (connUsername != correspondingPremiumProfile.name) {
//          event.result =
//            premiumUsernameRequiredDeniedResult(connUsername, correspondingPremiumProfile.name)
//          return
//        }
//      }
//    }
//  }
}
