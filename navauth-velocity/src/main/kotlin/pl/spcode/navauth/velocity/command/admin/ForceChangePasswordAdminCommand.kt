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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.credentials.queue.EncryptionTaskAlreadyQueuedException
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.user.UserArgumentResolver
import pl.spcode.navauth.common.command.user.UsernameOrUuidRaw
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.multification.VelocityMultification

@Command(name = "forcesetpassword")
@Permission(Permissions.ADMIN_FORCE_SET_PASSWORD)
class ForceChangePasswordAdminCommand
@Inject
constructor(
  val userService: UserService,
  val userCredentialsService: UserCredentialsService,
  val userArgumentResolver: UserArgumentResolver,
  val multification: VelocityMultification,
) {

  private val logger: Logger = LoggerFactory.getLogger(ForceChangePasswordAdminCommand::class.java)

  @Async
  @Execute
  @Description(
    "Force set password for specified user. Works like register command, but doesn't require password."
  )
  fun forceSetPassword(
    @Context sender: CommandSource,
    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
    @Arg(value = "password") password: String,
  ) {
    val user = userArgumentResolver.resolve(usernameOrUuidRaw)

    multification.create(sender) { it.multification.adminCmdPasswordSetUpdating }.send()

    try {
      userCredentialsService.updatePassword(user, password).whenComplete { _, throwable ->
        if (throwable != null) {
          logger.error("Failed to update password for user '${user.username.value}'", throwable)
          multification.send(sender) { it.multification.unexpectedErrorOccurred }
        } else {
          multification
            .create(sender) { it.multification.adminCmdPasswordSetSuccess }
            .placeholder("%USERNAME%", user.username.value)
            .send()
        }
      }
    } catch (_: EncryptionTaskAlreadyQueuedException) {
      multification.send(sender) { it.multification.processAlreadyInProgressError }
    }
  }
}
