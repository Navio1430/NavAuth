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

package pl.spcode.navauth.velocity.command.configurer

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import dev.rollczi.litecommands.command.builder.CommandBuilder
import dev.rollczi.litecommands.editor.Editor
import dev.rollczi.litecommands.meta.Meta
import dev.rollczi.litecommands.permission.PermissionSet
import pl.spcode.navauth.common.config.CommandConfig
import java.util.function.UnaryOperator

class CommandConfigurer @Inject constructor(
    private val commandConfiguration: CommandConfig
) : Editor<CommandSource> {


  override fun edit(context: CommandBuilder<CommandSource>): CommandBuilder<CommandSource> {
    val command = commandConfiguration.commands[context.name()] ?: return context

    var currentContext = context
    for ((childName, subCommand) in command.subcommands) {
      currentContext = currentContext.editChild(childName) { editor ->
        editor.name(subCommand.name)
            .aliases(subCommand.aliases)
            .applyMeta(editPermissions(subCommand.permissions))
            .enabled(subCommand.enabled)
      }
    }

    return currentContext
        .name(command.name)
        .aliases(command.aliases)
        .applyMeta(editPermissions(command.permissions))
        .enabled(command.enabled)
  }

  private fun editPermissions(permissions: List<String>): UnaryOperator<Meta> {
    return UnaryOperator { meta ->
      meta.listEditor(Meta.PERMISSIONS)
          .clear()
          .add(PermissionSet(permissions))
          .apply()
    }
  }
}