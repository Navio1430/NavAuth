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

package pl.spcode.navauth.velocity.command

import com.google.inject.Injector
import pl.spcode.navauth.velocity.command.admin.ForceChangePasswordAdminCommand
import pl.spcode.navauth.velocity.command.admin.ForceUnregisterAdminCommand
import pl.spcode.navauth.velocity.command.admin.MigrateUserDataAdminCommand
import pl.spcode.navauth.velocity.command.root.MigrationRootCommand
import pl.spcode.navauth.velocity.command.user.ChangePasswordCommand
import pl.spcode.navauth.velocity.command.user.LoginCommand
import pl.spcode.navauth.velocity.command.user.PremiumAccountCommand
import pl.spcode.navauth.velocity.command.user.RegisterCommand
import pl.spcode.navauth.velocity.command.user.UnregisterCommand

class CommandsRegistry {

  companion object {
    val commandsClasses =
      listOf(
        // user
        LoginCommand::class,
        RegisterCommand::class,
        UnregisterCommand::class,
        ChangePasswordCommand::class,
        PremiumAccountCommand::class,
        // admin
        ForceUnregisterAdminCommand::class,
        ForceChangePasswordAdminCommand::class,
        MigrateUserDataAdminCommand::class,
        // root (console only)
        MigrationRootCommand::class,
      )

    /** @return list of commands from registry each instantiated with injection */
    fun getWithInjection(injector: Injector): List<Any> {
      return commandsClasses.map { injector.getInstance(it.java) }
    }
  }
}
