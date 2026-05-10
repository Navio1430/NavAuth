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

package pl.spcode.navauth.velocity.command.admin

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.velocity.command.Permissions
import pl.spcode.navauth.velocity.multification.VelocityMultification

@Command(name = "navauth")
class ReloadConfigAdminCommand
@Inject
constructor(val config: MessagesConfig, val multification: VelocityMultification) {

  @Execute(name = "reload")
  @Permission(Permissions.ADMIN_RELOAD)
  fun reload(@Context sender: CommandSource) {
    multification
      .create(sender) { it.multification.adminCmdReloadingConfig }
      .placeholder("%NAME%", "messages")
      .send()
    try {
      config.load()
    } catch (e: Exception) {
      multification
        .create(sender) { it.multification.adminCmdConfigReloadError }
        .placeholder("%NAME%", "messages")
        .placeholder("%CAUSE%", e.message)
        .send()
    }
    multification
      .create(sender) { it.multification.adminCmdConfigReloadSuccess }
      .placeholder("%NAME%", "messages")
      .send()
  }
}
