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
  "For messages/notification we use Multification library.",
  "Thanks to it, you can use chat messages, action bars, sounds etc. combined.",
  "To learn more about it please read https://navio1430.github.io/NavAuth/multification.html.",
)
open class MessagesConfig : OkaeriConfig() {

  var test: Notice = Notice.chat("<red>Test</red>")

  // todo create support information message for each disconnect reason

  var usernameRequiredMessage =
    TextComponent(
      "You're trying to join with the username: '%USERNAME%', but we expect it to be '%EXPECTED%'. " +
        "\n\nPlease change your username and try again."
    )

  @Variable("CONFIG_VERSION")
  @Comment("Config version. DO NOT CHANGE this property!")
  var configVersion: Int = 0
    protected set
}
