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
import com.velocitypowered.api.proxy.ConsoleCommandSource
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import java.util.Optional
import me.uniodex.velocityrcon.commandsource.IRconCommandSource
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.user.UserArgumentResolver
import pl.spcode.navauth.common.command.user.UsernameOrUuidRaw
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.shared.utils.StringUtils.Companion.generateRandomString
import pl.spcode.navauth.velocity.command.Permissions

@Command(name = "forcecracked")
@Permission(Permissions.ADMIN_FORCE_CRACKED)
class ForceCrackedAdminCommand
@Inject
constructor(
  val userService: UserService,
  val userArgumentResolver: UserArgumentResolver,
  val userCredentialsService: UserCredentialsService,
) {

  @Async
  @Execute
  @Description(
    "Forces a premium user account into non-premium (cracked) mode.",
    "Generates or assigns a new password and updates the user’s authentication data accordingly.",
  )
  fun forceCrackedMode(
    @Context sender: CommandSource,
    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
    @Arg(value = "newPassword") newPasswordOpt: Optional<String>,
  ) {
    val user = userArgumentResolver.resolve(usernameOrUuidRaw)

    if (!user.isPremium) {
      sender.sendMessage(
        Component.text("User '${user.username}' is already non-premium account.", TextColors.RED)
      )
      return
    }

    val newPassword = newPasswordOpt.orElseGet { generateRandomString(8) }

    val hashedPassword = userCredentialsService.hashPassword(newPassword)
    userService.migrateToNonPremium(user, hashedPassword)

    val passwordText =
      if (sender is ConsoleCommandSource || sender is IRconCommandSource) {
        "$newPassword"
      } else {
        "<aqua><bold><click:copy_to_clipboard:${newPassword}>CLICK HERE TO COPY</click>"
      }
    sender.sendMessage(
      MiniMessage.miniMessage()
        .deserialize(
          "<${TextColors.GREEN.asHexString()}>User '${user.username}' has been successfully migrated to non-premium mode. Their new password is: $passwordText"
        )
    )
  }
}
