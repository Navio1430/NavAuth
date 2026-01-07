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

package pl.spcode.navauth.velocity.module

import com.google.inject.AbstractModule
import pl.spcode.navauth.common.command.handler.InvalidUsageHandler
import pl.spcode.navauth.common.command.handler.MissingPermissionExceptionHandler
import pl.spcode.navauth.common.command.handler.MissingPermissionHandler
import pl.spcode.navauth.velocity.infra.command.VelocityInvalidUsageHandler
import pl.spcode.navauth.velocity.infra.command.VelocityMissingPermissionExceptionHandler
import pl.spcode.navauth.velocity.infra.command.VelocityMissingPermissionHandler

class VelocityCommandsModule : AbstractModule() {

  override fun configure() {
    bind(MissingPermissionHandler::class.java).to(VelocityMissingPermissionHandler::class.java)
    bind(InvalidUsageHandler::class.java).to(VelocityInvalidUsageHandler::class.java)

    bind(MissingPermissionExceptionHandler::class.java)
      .to(VelocityMissingPermissionExceptionHandler::class.java)
  }
}
