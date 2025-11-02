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

package pl.spcode.navauth.velocity.infra.auth

import java.net.InetSocketAddress
import pl.spcode.navauth.common.domain.auth.UniqueSessionId

/**
 * Unique id based on nickname and socket port. Structure: "username:port"
 *
 * NOTE: Do not use this as an identifier outside single host. This ID loses its uniqueness in a
 * distributed system.
 */
class VelocityUniqueSessionId : UniqueSessionId {

  override val id: String

  private constructor(id: String) {
    this.id = id
  }

  constructor(username: String, socket: InetSocketAddress) : this("$username:${socket.port}")
}
