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
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.component.TextColors

@Command(name = "forcepremium")
@Permission(Permissions.ADMIN_FORCE_PREMIUM)
class ForcePremiumAdminCommand
@Inject
constructor(val userService: UserService, val profileService: MojangProfileService) {

  @Execute
  @Async
  fun forcePremiumMode(@Context sender: Player, @Arg(value = "username|uuid") user: User) {

    if (user.isPremium) {
      sender.sendMessage(Component.text("User is already premium.", TextColors.RED))
      return
    }

    val profile = profileService.fetchProfileInfo(user.username)
    if (profile == null) {
      sender.sendMessage(
        Component.text(
          "Can't find '${user.username}' user in Mojang database. This player can't be migrated to premium mode.",
          TextColors.RED,
        )
      )
      return
    }

    userService.migrateToPremium(user, profile.uuid)
    sender.sendMessage(
      Component.text(
        "User '${user.username}' successfully migrated to premium mode.",
        TextColors.GREEN,
      )
    )
  }
}
