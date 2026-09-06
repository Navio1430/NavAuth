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

package pl.spcode.navauth.common.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import pl.spcode.navauth.common.command.configurer.CommandConfig

class CommandsConfig : OkaeriConfig() {

  @Comment(
    "This property allows you to configure commands definitions.",
    "You can toggle the command and update its name or aliases.",
    "",
    "You can find command names in docs:",
    "https://navio1430.github.io/NavAuth/docs/general/commands.html#available-commands",
    "Remember to use command names without the leading slash '/'.",
  )
  var commands: MutableMap<String, CommandConfig> =
    mutableMapOf(
      "navauth" to CommandConfig(name = "navauth", aliases = mutableListOf("na"), enabled = true),
      "login" to CommandConfig(name = "login", aliases = mutableListOf("l"), enabled = true),
      "register" to
        CommandConfig(name = "register", aliases = mutableListOf("reg"), enabled = true),
    )
}
