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

package pl.spcode.navauth.common.infra.mojang

import com.google.gson.Gson
import com.google.inject.Inject
import com.google.inject.Singleton
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import pl.spcode.navauth.common.application.mojang.ProfileService
import pl.spcode.navauth.common.config.MojangAPIConfig
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.shared.utils.UuidUtils

@Singleton
class MineToolsProfileService
@Inject
constructor(val httpClient: HttpClient, val gson: Gson, val config: MojangAPIConfig) :
  ProfileService {

  private data class MineToolsProfileDto(val id: String?, val name: String?, val status: String?)

  override fun fetchProfileInfo(usernameCaseIgnored: Username): MojangProfile? {
    val requestUri = URI.create("https://api.minetools.eu/uuid/${usernameCaseIgnored.value}")
    val request = HttpRequest.newBuilder(requestUri).timeout(config.apiTimeout).GET().build()
    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

    val dto = gson.fromJson(response.body(), MineToolsProfileDto::class.java)

    if (dto.status != "OK" || dto.id == null || dto.name == null) {
      return null
    }

    return MojangProfile(MojangId(UuidUtils.from32(dto.id)), Username(dto.name))
  }
}
