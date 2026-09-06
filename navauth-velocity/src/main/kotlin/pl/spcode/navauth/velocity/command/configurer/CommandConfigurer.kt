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
import pl.spcode.navauth.common.config.CommandsConfig

class CommandConfigurer @Inject constructor(private val commandConfiguration: CommandsConfig) :
  Editor<CommandSource> {

  override fun edit(context: CommandBuilder<CommandSource>): CommandBuilder<CommandSource> {
    val command = commandConfiguration.commands[context.name()] ?: return context

    var newContext = context

    if (!command.name.isNullOrEmpty()) {
      newContext = context.name(command.name)
    }

    val aliasesFiltered = command.aliases.filter { it.isNotEmpty() }

    return newContext.aliases(aliasesFiltered).enabled(command.enabled)
  }
}
