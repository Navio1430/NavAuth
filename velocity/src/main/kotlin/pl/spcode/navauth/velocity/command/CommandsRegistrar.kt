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

import pl.spcode.navauth.velocity.command.admin.ForceChangePasswordAdminCommand
import pl.spcode.navauth.velocity.command.admin.ForceUnregisterAdminCommand
import pl.spcode.navauth.velocity.command.user.ChangePasswordCommand
import pl.spcode.navauth.velocity.command.user.LoginCommand
import pl.spcode.navauth.velocity.command.user.PremiumAccountCommand
import pl.spcode.navauth.velocity.command.user.RegisterCommand
import pl.spcode.navauth.velocity.command.user.UnregisterCommand

class CommandsRegistrar {

  companion object {
    val commands = listOf(
        // user
        LoginCommand(),
        RegisterCommand(),
        UnregisterCommand(),
        ChangePasswordCommand(),
        PremiumAccountCommand(),
        // admin
        ForceUnregisterAdminCommand(),
        ForceChangePasswordAdminCommand()
    )
  }
}