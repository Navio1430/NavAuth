package pl.spcode.navauth.common.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import pl.spcode.navauth.common.command.configurer.Command
import pl.spcode.navauth.common.command.configurer.SubCommand

class CommandConfig : OkaeriConfig() {

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
      "#           - \"<new_sub_command_permission>\""
  )
  var commands: MutableMap<String, Command> = mutableMapOf(
      "navauth" to Command(
          name = "navauth",
          aliases = mutableListOf("na"),
          permissions = mutableListOf("navauth.command.navauth"),
          enabled = true,
          subcommands = mutableMapOf(
              "reload" to SubCommand(
                  name = "reload",
                  enabled = true,
                  aliases = mutableListOf("rl"),
                  permissions = mutableListOf("navauth.command.reload")
              )
          )
      ),
      "login" to Command(
          name = "login",
          aliases = mutableListOf("log"),
          permissions = mutableListOf("navauth.command.login"),
          enabled = true
      ),
      "register" to Command(
          name = "register",
          aliases = mutableListOf("reg"),
          permissions = mutableListOf("navauth.command.register"),
          enabled = true
      ),
      "changepassword" to Command(
          name = "changepassword",
          aliases = mutableListOf("cpw", "changepw"),
          permissions = mutableListOf("navauth.command.changepassword"),
          enabled = true
      )
  )
}