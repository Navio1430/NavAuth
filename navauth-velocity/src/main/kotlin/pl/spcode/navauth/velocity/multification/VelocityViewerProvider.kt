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

package pl.spcode.navauth.velocity.multification

import com.eternalcode.multification.viewer.ViewerProvider
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ProxyServer
import java.util.UUID

class VelocityViewerProvider(private val proxyServer: ProxyServer) : ViewerProvider<CommandSource> {

  override fun console(): CommandSource = proxyServer.consoleCommandSource

  override fun player(uuid: UUID): CommandSource? = proxyServer.getPlayer(uuid).orElse(null)

  override fun onlinePlayers(): Collection<CommandSource> = proxyServer.allPlayers.toList()

  override fun onlinePlayers(permission: String): Collection<CommandSource> {
    return proxyServer.allPlayers.filter { it.hasPermission(permission) }
  }

  override fun all(): Collection<CommandSource> {
    val viewers = ArrayList<CommandSource>(onlinePlayers())
    viewers.add(console())
    return viewers.toList()
  }
}
