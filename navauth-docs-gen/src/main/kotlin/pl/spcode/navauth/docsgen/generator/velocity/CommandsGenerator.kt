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

package pl.spcode.navauth.docsgen.generator.velocity

import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.steppschuh.markdowngenerator.table.Table
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.docsgen.generator.Generator
import pl.spcode.navauth.velocity.command.CommandsRegistry
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

class CommandsGenerator: Generator {

  override fun generate(): String {
    val commands = mutableListOf<CommandInfo>()

    CommandsRegistry.commandsClasses.forEach { kClass ->
      val baseCommandAnnotation = kClass.findAnnotation<Command>()
      val classPermissionAnnotation = kClass.findAnnotation<Permission>()

      val basePermission = classPermissionAnnotation?.value?.joinToString(" ") ?: "no perm"
      val baseCommandName = baseCommandAnnotation?.name?.trimStart('/') ?: ""

      kClass.functions.forEach { function ->
        val executeAnnotation = function.findAnnotation<Execute>() ?: return@forEach
        val functionPermissionAnnotation = function.findAnnotation<Permission>()

        val fullCommandName = if (executeAnnotation.name.startsWith(baseCommandName)) {
          executeAnnotation.name.trimStart('/')
        } else {
          "$baseCommandName ${executeAnnotation.name.trimStart('/')}".trim()
        }

        val permission = functionPermissionAnnotation?.value?.joinToString(" ")
            ?: basePermission

        val descriptionAnnotation = function.findAnnotation<Description>()
        val description = descriptionAnnotation?.value?.joinToString("\n") ?: "No description"

        commands.add(CommandInfo(fullCommandName, permission, description))
      }
    }

    val md: StringBuilder = StringBuilder()

    val tableBuilder = Table.Builder()
        .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
        .addRow("Command name", "Permission", "Description")
    commands.forEach {
      tableBuilder.addRow(it.name, it.perm, it.desc)
    }
    md.appendLine(tableBuilder.build())

    commands.forEach {
      md.appendLine("## /${it.name}")
      md.appendLine(it.desc)
      md.appendLine("\n**PERM**: ${it.perm}")
    }

    return md.toString()
  }

  override fun getFileName(): String {
    return "velocity-commands.md"
  }

  private class CommandInfo(val name: String, val perm: String, val desc: String)
}