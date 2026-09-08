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
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import java.util.concurrent.CompletableFuture
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.credentials.queue.EncryptionTaskAlreadyQueuedException
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.multification.VelocityMultification

@Command(name = "changepassword")
@Permission(Permissions.USER_CHANGE_PASSWORD)
class ChangePasswordCommand
@Inject
constructor(
  val userService: UserService,
  val userCredentialsService: UserCredentialsService,
  val multification: VelocityMultification,
) {

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
  }

  @Async
  @Execute
  @Description("Changes account password to new one. Requires current password.")
  fun changePassword(
    @Context sender: Player,
    @Arg(value = "current_password") currentPassword: String,
    @Arg(value = "new_password") newPassword: String,
  ) {
    val user = userService.findUserByExactUsername(sender.username)!!

    if (user.isPremium) {
      multification.send(sender) { it.multification.accountNotNonPremiumError }
      return
    }

    val credentials = userCredentialsService.findCredentials(user)!!

    try {
      userCredentialsService
        .enqueueVerifyPassword(credentials, currentPassword, sender.uniqueId)
        .thenCompose { isCorrect ->
          if (!isCorrect) {
            multification.send(sender) { it.multification.wrongCredentialsError }
            CompletableFuture.completedFuture(false)
          } else {
            userCredentialsService.updatePassword(user, newPassword).thenApply { true }
          }
        }
        .thenAccept { updated ->
          if (updated) {
            multification.send(sender) { it.multification.newPasswordSetSuccess }
          }
        }
        .exceptionally { ex ->
          logger.error("Unexpected error occurred while trying to change user password", ex)
          multification.send(sender) { it.multification.unexpectedErrorOccurred }
          null
        }
    } catch (_: EncryptionTaskAlreadyQueuedException) {
      multification.send(sender) { it.multification.processAlreadyInProgressError }
    }
  }
}
