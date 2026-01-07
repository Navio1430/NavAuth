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

package pl.spcode.navauth.velocity.infra.component

import com.google.inject.Inject
import com.velocitypowered.api.proxy.ProxyServer
import java.util.UUID
import net.kyori.adventure.audience.Audience
import pl.spcode.navauth.common.component.AudienceProvider

class VelocityAudienceProvider @Inject constructor(val proxyServer: ProxyServer) :
  AudienceProvider() {

  override fun getConsole(): Audience {
    return proxyServer.consoleCommandSource
  }

  override fun getPlayer(identifier: UUID): Audience {
    return proxyServer.getPlayer(identifier).orElseThrow()
  }
}
