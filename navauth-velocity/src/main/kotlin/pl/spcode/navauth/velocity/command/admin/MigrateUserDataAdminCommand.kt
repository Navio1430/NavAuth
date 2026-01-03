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
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.user.UsernameAlreadyTakenException
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.command.UserArgumentResolver
import pl.spcode.navauth.common.command.UsernameOrUuidRaw
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.command.Permissions

@Command(name = "navauth user")
@Permission(Permissions.ADMIN_MIGRATE_USER_DATA)
class MigrateUserDataAdminCommand
@Inject
constructor(
  val proxyServer: ProxyServer,
  val userService: UserService,
  val profileService: MojangProfileService,
  val userArgumentResolver: UserArgumentResolver,
  val usernameValidator: UsernameValidator,
) {

  @Async
  @Execute(name = "migrate")
  @Description()
  fun migrateUserData(
    @Context sender: Player,
    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
    @Arg(value = "newAccountUsername") newUsername: String,
  ) {
    val user = userArgumentResolver.resolve(usernameOrUuidRaw)

    if (user.isPremium) {
      sender.sendMessage(
        Component.text(
          "Can't execute the command! Account '${user.username}' is set to premium mode. Use /navauth forcecracked command first.",
          TextColors.RED,
        )
      )
      return
    }

    if (!usernameValidator.isValid(newUsername)) {
      sender.sendMessage(
        Component.text("Provided username '${newUsername}' is invalid.", TextColors.RED)
      )
      return
    }

    val premiumMojangProfile = profileService.fetchProfileInfo(Username(newUsername))
    if (premiumMojangProfile != null) {
      sender.sendMessage(
        Component.text(
          "Provided username '${user.username}' is found as Mojang premium profile. Can't migrate to premium account.",
          TextColors.RED,
        )
      )
      return
    }

    try {
      userService.migrateData(user, Username(newUsername))
    } catch (e: UsernameAlreadyTakenException) {
      sender.sendMessage(
        Component.text(
          "Username '${newUsername}' is already taken. Please try again with different username.",
          TextColors.RED,
        )
      )
      return
    }

    // todo send api event

    proxyServer.getPlayer(user.username.value).ifPresent {
      it.disconnect(
        Component.text("Your account data has been migrated to '${newUsername}'.", TextColors.GREEN)
      )
    }

    sender.sendMessage(
      Component.text(
        "Success! User '${user.username}' data has been migrated to '${newUsername}'.",
        TextColors.GREEN,
      )
    )
  }
}
