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

import com.google.inject.Inject
import com.google.inject.Singleton
import pl.spcode.navauth.common.application.mojang.ProfileApiFetchException
import pl.spcode.navauth.common.application.mojang.ProfileService
import pl.spcode.navauth.common.config.MojangAPIConfig
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.mojang.MojangProfileApi
import pl.spcode.navauth.common.domain.user.Username

@Singleton
class CompositeProfileService
@Inject
constructor(
  private val config: MojangAPIConfig,
  private val mojangProfileService: MojangProfileServiceImpl,
  private val mineToolsProfileService: MineToolsProfileService,
  private val profileCache: ProfileCache,
) : ProfileService {

  override fun fetchProfileInfo(
    usernameCaseIgnored: Username,
    useNotFoundCache: Boolean,
  ): MojangProfile? {
    val username = Username(usernameCaseIgnored.value.lowercase())
    when (val cached = profileCache.get(username)) {
      is CachedProfile.Found -> return cached.profile
      is CachedProfile.NotFound ->
        if (useNotFoundCache) {
          return null
        }
      null -> {}
    }

    val exceptions: MutableList<Exception> = mutableListOf()
    for (api in config.apiOrder) {
      try {
        val profile =
          when (api) {
            MojangProfileApi.MINETOOLS ->
              mineToolsProfileService.fetchProfileInfo(usernameCaseIgnored)
            MojangProfileApi.MOJANG -> mojangProfileService.fetchProfileInfo(usernameCaseIgnored)
          }
        if (profile != null) {
          profileCache.putFound(username, profile)
          return profile
        }
      } catch (ex: Exception) {
        exceptions.add(ex)
      }
    }

    if (exceptions.isNotEmpty()) {
      throw ProfileApiFetchException(usernameCaseIgnored, exceptions)
    }
    if (useNotFoundCache) {
      profileCache.putNotFound(username)
    }
    return null
  }
}
