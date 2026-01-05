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

package pl.spcode.navauth.velocity.command.user

import com.google.inject.Inject
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.async.Async
import dev.rollczi.litecommands.annotations.command.RootCommand
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import java.util.Optional
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.config.TwoFactorAuthConfig
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.crypto.TOTP2FA
import pl.spcode.navauth.common.shared.qr.QRCodeGenerator
import pl.spcode.navauth.velocity.application.totp.TotpSetupSessionFactory
import pl.spcode.navauth.velocity.application.totp.TotpSetupSessionService
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

// we use inverted permission in this command
// todo generate command aliases in docs
@RootCommand
class SetupTwoFactorCommand
@Inject
constructor(
  val userService: UserService,
  val userCredentialsService: UserCredentialsService,
  val totpSetupSessionFactory: TotpSetupSessionFactory,
  val totpSetupSessionService: TotpSetupSessionService,
  val twoFactorAuthConfig: TwoFactorAuthConfig,
) {

  @Async
  @Execute(name = "setup2fa", aliases = ["enable2fa"])
  // todo description
  fun setupTwoFactor(
    @Context sender: Player,
    @Arg(value = "current_password") currentPassword: Optional<String>,
  ) {
    // if permission is set explicitly to FALSE
    if (sender.getPermissionValue(Permissions.USER_SETUP_TWO_FACTOR) == Tristate.FALSE) {
      // todo unify missing permission handler
      sender.sendMessage(
        Component.text("You don't have permission to use this command.", TextColors.RED)
      )
      return
    }

    val user = userService.findUserByUuid(UserUuid(sender.uniqueId))!!
    val credentials = userCredentialsService.findCredentials(user)
    if (credentials?.isTwoFactorEnabled == true) {
      sender.sendMessage(
        Component.text(
          "Can't execute this command right now: your account has 2FA enabled already.",
          TextColors.RED,
        )
      )
      return
    }

    if (credentials?.isPasswordRequired == true) {
      if (currentPassword.isEmpty) {
        sender.sendMessage(Component.text("Please provide your current password.", TextColors.RED))
        return
      }

      val isCorrectPassword =
        userCredentialsService.verifyPassword(credentials, currentPassword.get())
      if (!isCorrectPassword) {
        sender.sendMessage(Component.text("Wrong password!", TextColors.RED))
        return
      }
    }

    val secret = TOTP2FA().generateSecret()
    val session = totpSetupSessionFactory.createSession(VelocityPlayerAdapter(sender), secret)
    totpSetupSessionService.registerSession(UserUuid(sender.uniqueId), session)

    val remainingSeconds = TotpSetupSessionService.SESSION_LIFETIME_SECONDS
    sender.sendMessage(
      MiniMessage.miniMessage()
        .deserialize(
          """
      <#00ff88><bold>🔐 2FA Setup Started</bold>

      <white><bold>ℹ First add your secret to authenticator app:</bold>
      <gray>• Google Authenticator, Authy, Microsoft Auth, Aegis, etc.
      <gray>1. Tap the '<bold><white>+</white></bold>' button → "Enter secret manually"
      <gray>2. Add label such as your username and server name
      <gray>3. Complete the setup with → /complete2fa <code>

      <white><bold>YOUR SECRET:</bold>
      <bold><red>⚠ NEVER share this - even with admins!</red></bold>
      <yellow>${secret.value}</yellow>
      
      <click:run_command:'/generate2faqr'><aqua><b>CLICK HERE TO GENERATE QR CODE</b></click>

      <gray><i>⏱ Time left: ${remainingSeconds}s<gray></i>
"""
            .trimIndent()
        )
    )
    return
  }

  @Async
  @Execute(name = "complete2fa")
  // todo description
  fun completeTwoFactorSetup(@Context sender: Player, @Arg(value = "2fa_code") code: String) {
    val user = userService.findUserByUuid(UserUuid(sender.uniqueId))!!

    if (userCredentialsService.findCredentials(user)?.isTwoFactorEnabled == true) {
      sender.sendMessage(Component.text("2FA is already enabled for this account.", TextColors.RED))
      return
    }

    val session = totpSetupSessionService.findSession(UserUuid(sender.uniqueId))
    if (session == null) {
      sender.sendMessage(
        Component.text(
          "2FA setup session not found. Please try again using /setup2fa command first.",
          TextColors.RED,
        )
      )
      return
    }

    if (!TOTP2FA().verifyTOTP(session.secret, code)) {
      sender.sendMessage(Component.text("Invalid 2FA code!", TextColors.RED))
      return
    }

    userService.enableTwoFactorAuth(user, session.secret)
    totpSetupSessionService.closeSession(UserUuid(sender.uniqueId))
    sender.sendMessage(Component.text("2FA successfully enabled!", TextColors.GREEN))
  }

  @Execute(name = "generate2faqr")
  @Description(
    "Command used to generate QR code with otp totp data.",
    "Available only if 2FA setup session is found.",
  )
  fun generateQrCode(@Context sender: Player) {
    val session = totpSetupSessionService.findSession(UserUuid(sender.uniqueId))
    if (session == null) {
      sender.sendMessage(
        Component.text(
          "2FA setup session not found. Please try again using /setup2fa command first.",
          TextColors.RED,
        )
      )
      return
    }

    val issuer = twoFactorAuthConfig.issuer
    val label = sender.username
    val qrData = "otpauth://totp/${label}?secret=${session.secret.value}&issuer=${issuer}"

    val generator = QRCodeGenerator()
    val matrix = generator.generateQRMatrix(qrData)
    val grid = generator.bitMatrixToGrid(matrix)
    val unicodeGrid = generator.gridToMinecraftUnicodeWithMiniMessage(grid)

    val message = MiniMessage.miniMessage().deserialize(unicodeGrid.joinToString("\n"))
    sender.sendMessage(message)
  }
}
