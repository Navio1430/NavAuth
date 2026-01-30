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
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.exception.MissingPermissionException
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.multification.VelocityMultification

// inverted permission
@Command(name = "premium")
class PremiumAccountCommand
@Inject
constructor(
  val userService: UserService,
  val mojangProfileService: MojangProfileService,
  val multification: VelocityMultification,
) {

  @Async
  @Execute
  @Description(
    "Migrates account mode to premium account.",
    "Applicable for non-premium players only.",
    "Enables auto-login and migrates to premium.",
    "This command will remove bound **password** and will leave **2FA** secret if enabled.",
  )
  fun changeToPremiumAccount(@Context sender: Player) {
    // if permission is set explicitly to FALSE
    if (sender.getPermissionValue(Permissions.USER_CHANGE_TO_PREMIUM_ACCOUNT) == Tristate.FALSE) {
      throw MissingPermissionException(Permissions.USER_CHANGE_TO_PREMIUM_ACCOUNT)
    }

    val user = userService.findUserByExactUsername(sender.username)!!
    if (user.isPremium) {
      multification.send(sender) { it.multification.accountAlreadyPremiumError }
      return
    }

    val mojangProfile = mojangProfileService.fetchProfileInfo(Username(sender.username))
    if (mojangProfile == null) {
      multification
        .create(sender) { it.multification.commandNoPremiumAccountWithUsername }
        .placeholder("%USERNAME%", sender.username)
        .send()
      return
    }

    userService.migrateToPremium(user, mojangProfile.uuid)
    multification.create(sender) { it.multification.accountMigrationSuccess }
  }
}
