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

package pl.spcode.navauth.velocity.command.root

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.ProxyServer
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import me.uniodex.velocityrcon.commandsource.IRconCommandSource
import pl.spcode.navauth.common.annotation.Description
import pl.spcode.navauth.common.migrate.MigrationManager

@Command(name = "migration")
@Permission("navauth.root")
class MigrationRootCommand
@Inject
constructor(private val migrationManager: MigrationManager, private val proxyServer: ProxyServer) {

  @Execute(name = "start")
  @Description("")
  fun startMigration(@Context sender: CommandSource) {

    if (sender !is ConsoleCommandSource && sender !is IRconCommandSource) {
      sender.sendPlainMessage("This command can only be executed from console.")
      return
    }

    if (migrationManager.isMigrating) {
      sender.sendPlainMessage("Migration is already in progress.")
      return
    }

    try {
      sender.sendPlainMessage("Migration started...")
      migrationManager.startMigration()
    } catch (e: Exception) {
      sender.sendPlainMessage("Migration failed: ${e.message}")
      return
    }

    sender.sendPlainMessage("Migrated successfully!")
  }
}
