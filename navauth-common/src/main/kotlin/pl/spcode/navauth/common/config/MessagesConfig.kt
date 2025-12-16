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
    "This file contains all configurable messages and " +
    "notifications that are used in NavAuth."
)
open class MessagesConfig : OkaeriConfig() {

  // todo create support information message for each disconnect reason

  var usernameRequiredError =
    TextComponent(
      "<red>You're trying to join with the username: '%USERNAME%', but we expect it to be '%EXPECTED%'. " +
        "\n\nPlease change your username and try again."
    )

  var premiumUsernameRequiredError =
    TextComponent(
      "<red>You're trying to join with a premium username. Your current username is '%USERNAME%', but it must be '%EXPECTED%'. " +
        "\n\nPlease change your username and try again."
    )

  var usernameConflictError = TextComponent("<red>Premium and non-premium username CONFLICT!</red>")

  var loginTimeExceededError =
    TextComponent("<red>You've exceeded login time, please try again</red>")

  var registerTimeExceededError =
    TextComponent("<red>You've exceeded register time, please try again</red>")

  @Comment("Notices that use multification library.",
      "Here you can use chat messages, action bars, sounds etc. combined.",
      "To learn more about multification please read https://navio1430.github.io/NavAuth/multification.html.")
  var multification = NoticesConfig()

  class NoticesConfig: OkaeriConfig() {
    var loginInstruction: Notice = Notice.chat("<green>Please login using /login command.</green>")

    var registerInstruction: Notice =
        Notice.chat("<green>Please register using /register command.</green>")

    var loginSuccess: Notice =
        Notice.chat("<green>You have been authenticated successfully.</green>")

    var registerSuccess: Notice = Notice.chat("<green>Successfully registered</green>")

    var premiumAuthSuccess: Notice = Notice.chat("<green>Auto-logged in</green>")
  }

  @Variable("CONFIG_VERSION")
  @Comment("Config version. DO NOT CHANGE this property!")
  var configVersion: Int = 0
    protected set
}
