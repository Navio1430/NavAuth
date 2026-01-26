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
import com.velocitypowered.api.command.CommandSource
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.user.UserArgumentResolver
import pl.spcode.navauth.common.command.user.UsernameOrUuidRaw
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.multification.VelocityMultification

@Command(name = "forcepremium")
@Permission(Permissions.ADMIN_FORCE_PREMIUM)
class ForcePremiumAdminCommand
@Inject
constructor(
  val userService: UserService,
  val profileService: MojangProfileService,
  val userArgumentResolver: UserArgumentResolver,
  val multification: VelocityMultification,
) {

  @Execute
  @Async
  @Description("Does the same thing as `/premium` on specified user, buy forcefully used as admin")
  fun forcePremiumMode(
    @Context sender: CommandSource,
    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
  ) {
    val user = userArgumentResolver.resolve(usernameOrUuidRaw)

    if (user.isPremium) {
      multification
        .create(sender) { it.multification.adminCmdAccountIsPremiumError }
        .placeholder("%USERNAME%", user.username.value)
        .send()
      return
    }

    val profile = profileService.fetchProfileInfo(user.username)
    if (profile == null) {
      multification
        .create(sender) { it.multification.adminCmdUsernameNotPremiumError }
        .placeholder("%USERNAME%", user.username.value)
        .send()
      return
    }

    userService.migrateToPremium(user, profile.uuid)
    multification
      .create(sender) { it.multification.adminCmdUserPremiumMigrationSuccess }
      .placeholder("%USERNAME%", user.username.value)
      .send()
  }
}
