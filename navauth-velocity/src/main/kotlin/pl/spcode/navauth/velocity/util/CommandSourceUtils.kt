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

package pl.spcode.navauth.velocity.util

import com.velocitypowered.api.proxy.ConsoleCommandSource
import me.uniodex.velocityrcon.commandsource.IRconCommandSource

object CommandSourceUtils {
  fun isConsoleOrRcon(sender: Any): Boolean {
    if (sender is ConsoleCommandSource) return true
    return try {
      sender is IRconCommandSource
    } catch (_: ClassNotFoundException) {
      false
    } catch (_: NoClassDefFoundError) {
      false
    }
  }
}
