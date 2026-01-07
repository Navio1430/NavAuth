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
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Optional
import net.kyori.adventure.text.minimessage.MiniMessage
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserActivitySessionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.UserArgumentResolver
import pl.spcode.navauth.common.command.UsernameOrUuidRaw
import pl.spcode.navauth.velocity.command.Permissions

@Command(name = "lookup")
@Permission(Permissions.ADMIN_PLAYER_LOOKUP)
class PlayerLookupAdminCommand
@Inject
constructor(
  val userService: UserService,
  val userActivitySessionService: UserActivitySessionService,
  val credentialsService: UserCredentialsService,
  val userArgumentResolver: UserArgumentResolver,
) {

  val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

  @Async
  @Execute(name = "profile")
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
    val lastIp = userActivitySessionService.findLatestSession(user)?.ip

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
• <white>Last IP:</white> <gray><click:copy_to_clipboard:$lastIp><hover:show_text:${lastIp ?: "-"}><b>HOVER TO REVEAL</b></hover> <hover:show_text:"<dark_gray>Copy"><blue><u>[copy]</u></blue></hover></click>

"""
        .trimIndent()

    sender.sendMessage(MiniMessage.miniMessage().deserialize(template))
  }

  @Async
  @Execute(name = "sessions", aliases = ["session"])
  fun lookupSession(
    @Context sender: Player,
    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
    @Arg pageNumber: Optional<Long>,
  ) {
    val user = userArgumentResolver.resolve(usernameOrUuidRaw)

    val pageSize = 8L
    val paginator = userActivitySessionService.getSessionPaginatorByUuid(user.uuid, pageSize)
    val pageCount = paginator.getPagesCount()
    val pageNumber = pageNumber.orElseGet { 1L }
    val sessions = paginator.paginate(pageNumber)

    val sessionLines =
      sessions.joinToString("\n") { session ->
        val joinTime = dateFormat.format(session.joinedAt)
        val duration = Duration.between(session.joinedAt.toInstant(), session.leftAt.toInstant())
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()
        val durationStr =
          when {
            hours > 0 ->
              "${hours}h ${if (minutes > 0) "${minutes}min" else ""}${if (minutes == 0 && seconds > 0) " ${seconds}s" else ""}"
                .trim()
            minutes > 0 -> "${minutes}min${if (seconds > 0) " ${seconds}s" else ""}"
            else -> "${seconds}s"
          }
        val ipText = "<blue><hover:show_text:${session.ip}>[Show IP]</hover></blue>"

        "<gray> • ($joinTime) <white>${durationStr.padEnd(15)} $ipText"
      }

    val previousPageButton =
      if (pageNumber > 1)
        "<blue><b><click:run_command:${lookupSessionPageCommand(user.username.value, pageNumber - 1)}><<<</click></blue>"
      else "<gray><<<</gray>"
    val nextPageButton =
      if (pageNumber < pageCount)
        "<blue><b><click:run_command:${lookupSessionPageCommand(user.username.value, pageNumber + 1)}>>>></click></blue>"
      else "<gray><<<</gray>"

    val template =
      """

👤 Player <blue><b>N4vio</blue> sessions
<white><b><st>━━━━━━━━━━━━━━━━━━━</st></b></white>

<dark_gray>   Join date               Session duration
$sessionLines

  $previousPageButton page $pageNumber/$pageCount $nextPageButton

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

  private fun lookupSessionPageCommand(username: String, currentPageNumber: Long): String {
    return "/lookup sessions $username $currentPageNumber"
  }
}
