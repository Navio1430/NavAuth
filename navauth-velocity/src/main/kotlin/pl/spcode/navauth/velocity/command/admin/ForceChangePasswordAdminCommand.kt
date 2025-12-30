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
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.infra.crypto.hasher.BCryptCredentialsHasher
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.component.TextColors

@Command(name = "forcesetpassword")
@Permission(Permissions.ADMIN_FORCE_SET_PASSWORD)
class ForceChangePasswordAdminCommand
@Inject
constructor(val userService: UserService, val userCredentialsService: UserCredentialsService) {

  @Async
  @Execute
  @Description(
    "Force set password for specified user. Works like register command, but doesn't require password."
  )
  fun forceSetPassword(
    @Context sender: Player,
    @Arg(value = "playerName") playerName: String,
    @Arg(value = "password") password: String,
  ) {
    val user = userService.findUserByUsernameIgnoreCase(playerName.lowercase())

    if (user == null) {
      sender.sendMessage(Component.text("User '${playerName}' not found.", TextColors.RED))
      return
    }

    if (user.isPremium) {
      sender.sendMessage(
        Component.text(
          "Can't execute the command! Account '${user.username}' is set to premium mode.",
          TextColors.RED,
        )
      )
      return
    }

    val newCredentials = UserCredentials.create(user, BCryptCredentialsHasher().hash(password))
    userCredentialsService.storeUserCredentials(newCredentials)

    sender.sendMessage(
      Component.text("Success! User '${user.username}' credentials set.", TextColors.GREEN)
    )
  }
}
