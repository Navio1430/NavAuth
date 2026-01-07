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

import com.eternalcode.multification.Multification
import com.eternalcode.multification.notice.NoticeBroadcast
import dev.rollczi.litecommands.identifier.Identifier
import dev.rollczi.litecommands.invocation.Invocation
import java.util.UUID

abstract class MultificationProvider(private val multification: Multification<*, *>) {

  fun <SENDER> provide(invocation: Invocation<SENDER>): NoticeBroadcast<*, *, *> {
    if (invocation.platformSender() == null) throw IllegalArgumentException()
    val identifier = invocation.platformSender().identifier
    if (identifier == Identifier.CONSOLE) {
      return multification.create().console()
    } else {
      val uuid = identifier.getIdentifier(UUID::class.java).orElseThrow()
      return multification.create().player(uuid)
    }
  }
}
