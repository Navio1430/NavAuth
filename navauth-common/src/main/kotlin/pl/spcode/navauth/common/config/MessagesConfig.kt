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

package pl.spcode.navauth.common.config

import com.eternalcode.multification.notice.Notice
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.Header
import eu.okaeri.configs.annotation.Variable
import pl.spcode.navauth.common.component.TextComponent

@Header(
  "NavAuth messages configuration",
  "This file contains all configurable messages and " + "notifications that are used in NavAuth.",
)
open class MessagesConfig : OkaeriConfig() {

  var supportFooter =
    TextComponent("<br><br><gray>For support please join our discord: https://dc.spcode.pl/")

  var usernameRequiredError =
    TextComponent(
      "<red>You're trying to join with the username: '%USERNAME%', but we expect it to be '%EXPECTED%'. " +
        "<br>Please change your username and try again."
    )

  var premiumUsernameRequiredError =
    TextComponent(
      "<red>You're trying to join with a premium username. Your current username is '%USERNAME%', but it must be '%EXPECTED%'. " +
        "<br>Please change your username and try again."
    )

  var usernameConflictError = TextComponent("<red>Premium and non-premium username CONFLICT!</red>")

  var usernameAlreadyTakenConflictError =
    TextComponent(
      "<red>Username '%USERNAME%' is already taken! Administrator needs to resolve the conflict.</red>"
    )

  var loginTimeExceededError =
    TextComponent("<red>You've exceeded login time, please try again</red>")

  var registerTimeExceededError =
    TextComponent("<red>You've exceeded register time, please try again</red>")

  var adminCopyPasswordText =
    "<aqua><bold><click:copy_to_clipboard:%PASSWORD%>CLICK HERE TO COPY</click>"

  var yourAccountDataHasBeenMigrated =
    TextComponent("<green>Your account data has been migrated to '%USERNAME%'.")

  @Comment(
    "Notifications which use multification library.",
    "Here you can use chat messages, action bars, sounds etc. combined.",
    "To learn more about multification please read https://navio1430.github.io/NavAuth/docs/configuration/multification.html",
  )
  var multification = NoticesConfig()

  class NoticesConfig : OkaeriConfig() {

    val passwordRequiredError: Notice = Notice.chat("<red>Please provide your current password.")
    val twoFactorAlreadyEnabledError: Notice =
      Notice.chat("<red>Your account has 2FA enabled already!")

    var missingPermissionError: Notice =
      Notice.chat("<red>You don't have permission to execute this command.")
    var invalidUsageError: Notice = Notice.chat("<red>Invalid command usage!")
    @Comment("Invalid usage scheme line (single text component only).")
    var invalidUsageLine: TextComponent = TextComponent("<gray> • %SCHEME%")

    var cantUseThisCommandNowError: Notice = Notice.chat("<red>Can't use this command right now.")
    val commandPasswordNotSetForAccountError: Notice =
      Notice.chat("<red>Can't execute this command right now: your account has no password set.")
    val commandNoPremiumAccountWithUsername: Notice =
      Notice.chat(
        "<red>Can't set this account as premium because there's no premium account with username '%USERNAME%'."
      )
    val accountAlreadyPremiumError: Notice =
      Notice.chat("<red>Account is already set as a premium one.")

    val registerPasswordInvalidError: Notice =
      Notice.chat(
        "<red>The password is invalid. It must be at least 5 characters long and contain at least one uppercase letter and one digit."
      )
    val registerPasswordsMustMatchError: Notice = Notice.chat("<red>Both passwords must match.")

    var loginPasswordOnlyInstruction: Notice =
      Notice.chat("<green>Please login using \"/login <password>\" command.</green>")
    var loginTwoFactorOnlyInstruction: Notice =
      Notice.chat("<green>Please login using \"/2fa <code>\" command.</green>")
    var loginPasswordAndTwoFactorInstruction: Notice =
      Notice.chat("<green>Please login using \"/login <password> <2fa_code>\" command.</green>")

    var registerInstruction: Notice =
      Notice.chat("<green>Please register using /register command.</green>")

    var loginSuccess: Notice =
      Notice.chat("<green>You have been authenticated successfully.</green>")
    var registerSuccess: Notice = Notice.chat("<green>Successfully registered</green>")
    var premiumAuthSuccess: Notice = Notice.chat("<green>Auto-logged in</green>")

    val accountMigrationSuccess: Notice = Notice.chat("<green>Account migrated successfully!")
    val newPasswordSetSuccess: Notice = Notice.chat("<green>Success! New password set.")

    val wrongCredentialsError: Notice = Notice.chat("<red>Wrong credentials provided!")

    val twoFactorDisabledSuccess: Notice = Notice.chat("<green>2FA is now disabled!")
    val twoFactorEnabledSuccess: Notice = Notice.chat("<green>2FA is now enabled!")
    val twoFactorCodeRequiredError: Notice =
      Notice.chat("<red>Please provide two-factor authentication code.")
    val twoFactorSessionNotFound: Notice =
      Notice.chat(
        "<red>2FA setup session not found. Please try again using /setup2fa command first."
      )
    val twoFactorWrongCodeError: Notice = Notice.chat("<red>Wrong 2FA code!")
    val twoFactorAlreadyDisabledError: Notice =
      Notice.chat("<red>Your account has 2FA disabled already.")

    var twoFactorSetupInstruction: Notice =
      Notice.chat(
        """
        <#00ff88><bold>🔐 2FA Setup Started</bold>

        <white><bold>ℹ First add your secret to authenticator app:</bold>
        <gray>• Google Authenticator, Authy, Microsoft Auth, Aegis, etc.
        <gray>1. Tap the '<bold><white>+</white></bold>' button → "Enter secret manually"
        <gray>2. Add label such as your username and server name
        <gray>3. Complete the setup with → /verify2fa <code>

        <white><bold>YOUR SECRET:</bold>
        <bold><red>⚠ NEVER share this - even with admins!</red></bold>
        <yellow>%SECRET%</yellow>
        
        <click:run_command:'/generate2faqr'><aqua><b>CLICK HERE TO GENERATE QR CODE</b></click>

        <gray><i>⏱ Time left: %REMAINING_SECONDS%s<gray></i>
        """
          .trimIndent()
      )

    val adminCmdUsernameIsInvalid: Notice =
      Notice.chat("<red>Provided username '%USERNAME%' is invalid.")
    val adminCmdAccountIsPremiumError: Notice =
      Notice.chat("<red>Can't execute the command! Account '%USERNAME%' is set to premium mode.")
    val adminCmdAccountIsAlreadyNonPremiumError: Notice =
      Notice.chat("<red>Account '%USERNAME%' is already a non-premium account.")
    val adminCmdUseForceCrackedFirst: Notice = Notice.chat("<red>Use /forcecracked command first.")
    val adminCmdCantMigrateToExistingPremiumAccount: Notice =
      Notice.chat(
        "<red>Provided username '%USERNAME%' is found as Mojang premium profile. Can't migrate to premium account."
      )
    val adminCmdUsernameAlreadyTakenError: Notice =
      Notice.chat(
        "<red>Username '%USERNAME%' is already taken. Please try again with a different username."
      )
    val adminCmdUsernameNotPremiumError: Notice =
      Notice.chat(
        "<red>Can't find '%USERNAME%' user in Mojang database. This player can't be migrated to premium mode."
      )

    val adminCmdPasswordSetSuccess: Notice =
      Notice.chat("<green>Success! User '%USERNAME%' password was set.")
    val adminCmdAccountMigratedToNonPremiumSuccess: Notice =
      Notice.chat(
        "<green>User '%USERNAME%' has been successfully migrated to non-premium mode. Their new password is: %PASSWORD_TEXT%"
      )
    val adminCmdUserDataMigratedSuccess: Notice =
      Notice.chat(
        "<green>Success! User '%OLD_USERNAME%' data has been migrated to '%NEW_USERNAME%'."
      )
    val adminCmdUserPremiumMigrationSuccess: Notice =
      Notice.chat("<green>User '%USERNAME%' successfully migrated to premium mode.")
  }

  @Variable("CONFIG_VERSION")
  @Comment("Config version. DO NOT CHANGE this property!")
  var configVersion: Int = 0
    protected set
}
