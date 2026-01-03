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

package pl.spcode.navauth.common.component

import dev.rollczi.litecommands.identifier.Identifier
import dev.rollczi.litecommands.invocation.Invocation
import java.util.UUID
import net.kyori.adventure.audience.Audience

abstract class AudienceProvider {
  abstract fun getConsole(): Audience

  abstract fun getPlayer(identifier: UUID): Audience

  fun <SENDER> getAudience(invocation: Invocation<SENDER>): Audience {
    if (invocation.platformSender() == null) throw IllegalArgumentException()
    val identifier = invocation.platformSender().identifier
    if (identifier == Identifier.CONSOLE) {
      return getConsole()
    } else {
      val uuid = identifier.getIdentifier(UUID::class.java).orElseThrow()
      return getPlayer(uuid)
    }
  }
}
