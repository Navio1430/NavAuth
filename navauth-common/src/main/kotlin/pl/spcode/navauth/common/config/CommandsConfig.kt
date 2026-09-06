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
    "# This file allows you to configure commands.",
    "# You can change command name, aliases and permissions.",
    "# You can edit the commands as follows this template:",
    "# commands:",
    "#   <command_name>:",
    "#     name: \"<new_command_name>\"",
    "#     enabled: true/false",
    "#     aliases:",
    "#       - \"<new_command_aliases>\"",
    "#     permissions:",
    "#       - \"<new_command_permission>\"",
    "#     subcommands:",
    "#       <default_sub_command_name>:",
    "#         name: \"<new_sub_command_name>\"",
    "#         enabled: true/false",
    "#         aliases:",
    "#           - \"<new_sub_command_aliases>\"",
    "#         permissions:",
    "#           - \"<new_sub_command_permission>\"",
  )
  var commands: MutableMap<String, CommandConfig> =
    mutableMapOf(
      "navauth" to
        CommandConfig(
          name = "navauth",
          aliases = mutableListOf("na"),
          permissions = mutableListOf("navauth.command.navauth"),
          enabled = true,
        ),
      "login" to
        CommandConfig(
          name = "login",
          aliases = mutableListOf("l"),
          permissions = mutableListOf("navauth.command.login"),
          enabled = true,
        ),
      "register" to
        CommandConfig(
          name = "register",
          aliases = mutableListOf("reg"),
          permissions = mutableListOf("navauth.command.register"),
          enabled = true,
        ),
    )
}
