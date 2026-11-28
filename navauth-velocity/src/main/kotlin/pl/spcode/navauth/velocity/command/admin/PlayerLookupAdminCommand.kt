/*
 * NavAuth
 * Copyright © 2026 Oliwier Fijas (Navio1430)
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
import net.kyori.adventure.text.minimessage.MiniMessage
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.user.UserArgumentResolver
import pl.spcode.navauth.common.command.user.UsernameOrUuidRaw
import pl.spcode.navauth.velocity.command.Permissions

@Command(name = "lookup")
@Permission(Permissions.ADMIN_PLAYER_LOOKUP_BASE)
class PlayerLookupAdminCommand
@Inject
constructor(
  val userService: UserService,
  val credentialsService: UserCredentialsService,
  val userArgumentResolver: UserArgumentResolver,
) {

  @Async
  @Permission(Permissions.ADMIN_PLAYER_LOOKUP_PROFILE)
  @Execute(name = "profile")
  @Description("Displays information about specified user.")
  fun lookupProfile(
    @Context sender: Player,
    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
  ) {
    val user = userArgumentResolver.resolve(usernameOrUuidRaw)

    val isPremium = user.isPremium
    val credentials = credentialsService.findCredentials(user)
    val isTwoFactorEnabled = credentials?.isTwoFactorEnabled ?: false
    val isPasswordEnabled = credentials?.isPasswordRequired ?: false
    val username = user.username
    val uuid = user.uuid
    val mojangUuid = user.mojangUuid?.value

    val mojangLine =
      mojangUuid?.let {
        "• <white>Mojang UUID:</white> <dark_gray>$it</dark_gray> <click:copy_to_clipboard:$it><hover:show_text:\"<dark_gray>Copy\"><blue><u>[copy]</u></blue></hover></click>"
      } ?: "• <white>Mojang UUID:</white> <dark_gray>-</dark_gray>"

    val template =
      """
<gradient:#1e3a8a:#0f172a><bold><white>👤 Player Lookup</white></bold></gradient>
<white><b><st>━━━━━━━━━━━━━━━━━━━</st></b></white>

• <white>Username:</white> <blue><bold>$username</bold></blue>
• <white>Premium:</white> <blue>${checkedIcon(isPremium)}</blue>
• <white>UUID:</white> <dark_gray>$uuid</dark_gray> <click:copy_to_clipboard:$uuid><hover:show_text:"<dark_gray>Copy"><blue><u>[copy]</u></blue></hover></click>
$mojangLine
• <white>Password:</white> <blue>${checkedIcon(isPasswordEnabled)}</blue>
• <white>2FA:</white> <blue>${checkedIcon(isTwoFactorEnabled)}</blue>

"""
        .trimIndent()

    sender.sendMessage(MiniMessage.miniMessage().deserialize(template))
  }

  private fun checkedIcon(checked: Boolean): String {
    return if (checked) {
      "✔"
    } else {
      "❌"
    }
  }
}
