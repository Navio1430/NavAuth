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

package pl.spcode.navauth.velocity.command.user

import com.google.inject.Inject
import com.velocitypowered.api.proxy.Player
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.component.TextColors

@Command(name = "premium")
@Permission(Permissions.USER_CHANGE_TO_PREMIUM_ACCOUNT)
class PremiumAccountCommand @Inject constructor(val userService: UserService, val mojangProfileService: MojangProfileService) {

  @Async
  @Execute
  fun changeToPremiumAccount(@Context sender: Player) {
    val user = userService.findUserByUsername(sender.username, false)!!
    if (user.isPremium) {
      sender.sendMessage(Component.text("Account is already set as a premium one.", TextColors.RED))
      return;
    }

    val mojangProfile = mojangProfileService.fetchProfileInfo(sender.username)
    if (mojangProfile == null) {
      sender.sendMessage(Component.text("Can't set this account as premium because there's no premium account with username '${sender.username}'.", TextColors.RED))
      return;
    }

    userService.migrateToPremium(user)
    sender.sendMessage(Component.text("Account migrated successfully!", TextColors.GREEN))
  }
}
