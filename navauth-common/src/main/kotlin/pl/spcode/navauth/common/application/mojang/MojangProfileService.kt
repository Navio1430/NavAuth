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

package pl.spcode.navauth.common.application.mojang

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.inject.Inject
import com.google.inject.Singleton
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.shared.http.HttpStatusCodes
import pl.spcode.navauth.common.shared.utils.UuidUtils

@Singleton
class MojangProfileService @Inject constructor(val httpClient: HttpClient) {

  val gson: Gson = Gson()

  data class MojangProfileDto(
    @SerializedName(value = "id") val uuidWithoutDashes: String,
    val name: String,
  ) {

    fun toMojangProfile(): MojangProfile {
      return MojangProfile(MojangId(UuidUtils.from32(uuidWithoutDashes)), name)
    }
  }

  fun fetchProfileInfo(usernameCaseIgnored: Username): MojangProfile? {
    val requestUri =
      URI.create(
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/$usernameCaseIgnored"
      )
    val request = HttpRequest.newBuilder(requestUri).GET().build()

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

    if (response.statusCode() == HttpStatusCodes.NOT_FOUND) {
      return null
    }

    val profileDto = gson.fromJson(response.body(), MojangProfileDto::class.java)
    return profileDto.toMojangProfile()
  }
}
