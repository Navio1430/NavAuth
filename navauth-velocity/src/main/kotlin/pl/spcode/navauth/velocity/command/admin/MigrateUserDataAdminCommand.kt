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

package pl.spcode.navauth.velocity.command.admin

import com.google.inject.Inject
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.user.UsernameAlreadyTakenException
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.command.user.UserArgumentResolver
import pl.spcode.navauth.common.command.user.UsernameOrUuidRaw
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.multification.VelocityMultification

@Command(name = "migrateuser")
@Permission(Permissions.ADMIN_MIGRATE_USER_DATA)
class MigrateUserDataAdminCommand
@Inject
constructor(
  val proxyServer: ProxyServer,
  val userService: UserService,
  val profileService: MojangProfileService,
  val userArgumentResolver: UserArgumentResolver,
  val usernameValidator: UsernameValidator,
  val multification: VelocityMultification,
) {

  @Async
  @Execute
  @Description(
    "Migrates user data from an existing cracked account to a new username.",
    "The command validates usernames, checks for conflicts or premium accounts,",
    "and safely transfers all stored data to the specified new account.",
    "If you want to migrate premium user, then use /forcecracked command first.",
  )
  fun migrateUserData(
    @Context sender: Player,
    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
    @Arg(value = "newAccountUsername") newUsername: String,
  ) {
    val user = userArgumentResolver.resolve(usernameOrUuidRaw)

    if (user.isPremium) {
      multification
        .create(sender) { it.multification.adminCmdAccountIsPremiumError }
        .placeholder("%USERNAME%", user.username.value)
        .send()
      multification.send(sender) { it.multification.adminCmdUseForceCrackedFirst }
      return
    }

    if (!usernameValidator.isValid(newUsername)) {
      multification
        .create(sender) { it.multification.adminCmdUsernameIsInvalid }
        .placeholder("%USERNAME%", newUsername)
        .send()
      return
    }

    val premiumMojangProfile = profileService.fetchProfileInfo(Username(newUsername))
    if (premiumMojangProfile != null) {
      multification
        .create(sender) { it.multification.adminCmdCantMigrateToExistingPremiumAccount }
        .placeholder("%USERNAME%", user.username.value)
        .send()
      return
    }

    try {
      userService.migrateData(user, Username(newUsername))
    } catch (e: UsernameAlreadyTakenException) {
      multification
        .create(sender) { it.multification.adminCmdUsernameAlreadyTakenError }
        .placeholder("%USERNAME%", newUsername)
        .send()
      return
    }

    proxyServer.getPlayer(user.username.value).ifPresent {
      val disconnectReason =
        multification.config.yourAccountDataHasBeenMigrated
          .withPlaceholders()
          .placeholder("USERNAME", newUsername)
          .toComponent()
      it.disconnect(disconnectReason)
    }

    multification
      .create(sender) { it.multification.adminCmdUserDataMigratedSuccess }
      .placeholder("%OLD_USERNAME%", user.username.value)
      .placeholder("%NEW_USERNAME%", newUsername)
      .send()
  }
}
