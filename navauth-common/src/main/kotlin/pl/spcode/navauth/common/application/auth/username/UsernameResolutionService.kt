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

package pl.spcode.navauth.common.application.auth.username

import com.google.inject.Inject
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.user.UsernameAlreadyTakenException
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.Username

class UsernameResolutionService
@Inject
constructor(
  private val userService: UserService,
  private val profileService: MojangProfileService,
) {

  fun resolveUsernameConflicts(
    connUsername: Username,
    existingUserIgnoreCase: User?,
  ): UsernameResResult {
    val correspondingPremiumProfile = profileService.fetchProfileInfo(connUsername)
    val isPremiumNickname = correspondingPremiumProfile != null

    // check if the user changed their nickname (does not check letter cases)
    if (existingUserIgnoreCase == null && isPremiumNickname) {
      val userByMojangUuid = userService.findUserByMojangUuid(correspondingPremiumProfile.uuid)
      if (userByMojangUuid != null) {
        // user with the same mojang uuid exists, but with different nickname
        try {
          userService.migrateUsername(userByMojangUuid, Username(correspondingPremiumProfile.name))
        } catch (e: UsernameAlreadyTakenException) {
          return failure(
            UsernameResFailureReason.UsernameMigrationFailedUsernameAlreadyTaken(
              correspondingPremiumProfile.name
            )
          )
        }
        if (connUsername.value != correspondingPremiumProfile.name) {
          return failure(
            UsernameResFailureReason.PremiumUsernameNotIdentical(correspondingPremiumProfile.name)
          )
        }
        return success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
        )
      }
    }

    if (existingUserIgnoreCase != null && existingUserIgnoreCase.isPremium && isPremiumNickname) {
      // check if the letter case changed
      if (
        correspondingPremiumProfile.name != existingUserIgnoreCase.username.value &&
          correspondingPremiumProfile.name.equals(existingUserIgnoreCase.username.value, true)
      ) {
        userService.migrateUsername(
          existingUserIgnoreCase,
          Username(correspondingPremiumProfile.name),
        )
        return success(
          EncryptionType.ENFORCE_PREMIUM,
          PostUsernameResolutionState.PREMIUM_USERNAME_MIGRATED,
        )
      }
    }

    if (existingUserIgnoreCase != null && !existingUserIgnoreCase.isPremium) {
      if (isPremiumNickname) {
        return failure(
          UsernameResFailureReason.NonPremiumWithPremiumConflict(correspondingPremiumProfile.name)
        )
        //        if (correspondingPremiumProfile.name == connUsername) {
        //          // possible premium user tries to join on the current nonpremium account
        //        }
        //        // conflict or possible premium migration
        //        if (correspondingPremiumProfile.name != existingUser.username.value) {
        //          // existing mojang account but with a different name case
        //        } else {
        //
        //        }
      } else {
        if (connUsername.value != existingUserIgnoreCase.username.value) {
          return failure(
            UsernameResFailureReason.NonPremiumUsernameNotIdentical(
              existingUserIgnoreCase.username.value
            )
          )
        }
        return success(EncryptionType.NONE, PostUsernameResolutionState.NO_CHANGE)
      }
    }

    if (existingUserIgnoreCase != null && existingUserIgnoreCase.isPremium) {
      if (connUsername.value != existingUserIgnoreCase.username.value) {
        return failure(
          UsernameResFailureReason.PremiumUsernameNotIdentical(
            existingUserIgnoreCase.username.value
          )
        )
      }
      return success(EncryptionType.ENFORCE_PREMIUM, PostUsernameResolutionState.NO_CHANGE)
    }

    if (existingUserIgnoreCase == null) {
      if (isPremiumNickname) {
        if (connUsername.value != correspondingPremiumProfile.name) {
          return failure(
            UsernameResFailureReason.PremiumUsernameNotIdentical(correspondingPremiumProfile.name)
          )
        }
        return success(EncryptionType.ENFORCE_PREMIUM, PostUsernameResolutionState.NEW_ACCOUNT)
      } else {
        return success(EncryptionType.NONE, PostUsernameResolutionState.NEW_ACCOUNT)
      }
    }

    throw IllegalStateException(
      "Reached code that should not be reached. Please report this directly to NavAuth developers."
    )
  }

  private fun success(
    encryptionType: EncryptionType,
    postUsernameResolutionState: PostUsernameResolutionState,
  ): UsernameResResult.Success {
    return UsernameResResult.Success(encryptionType, postUsernameResolutionState)
  }

  private fun failure(reason: UsernameResFailureReason): UsernameResResult.Failure {
    return UsernameResResult.Failure(reason)
  }
}
