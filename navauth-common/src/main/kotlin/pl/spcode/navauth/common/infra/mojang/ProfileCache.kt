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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import pl.spcode.navauth.common.config.MojangAPIConfig
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.Username

sealed interface CachedProfile {
  data class Found(val profile: MojangProfile) : CachedProfile

  data object NotFound : CachedProfile
}

@Singleton
class ProfileCache @Inject constructor(config: MojangAPIConfig) {

  private val cache: Cache<String, CachedProfile> =
    CacheBuilder.newBuilder().expireAfterWrite(config.profileCacheTTL).build()

  fun get(username: Username): CachedProfile? {
    return cache.getIfPresent(username.value)
  }

  fun putFound(username: Username, profile: MojangProfile) {
    cache.put(username.value, CachedProfile.Found(profile))
  }

  fun putNotFound(username: Username) {
    cache.put(username.value, CachedProfile.NotFound)
  }

  fun invalidate(username: Username) {
    cache.invalidate(username.value)
  }
}
