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

package unit.mojang

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID
import pl.spcode.navauth.common.config.MojangAPIConfig
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.mojang.CachedProfile
import pl.spcode.navauth.common.infra.mojang.ProfileCache

class ProfileCacheTest :
  FunSpec({
    lateinit var cache: ProfileCache
    val username = Username("test")
    val profile = MojangProfile(MojangId(UUID.randomUUID()), username)

    beforeTest { cache = ProfileCache(MojangAPIConfig()) }

    test("get returns null for uncached username") { cache.get(username).shouldBeNull() }

    test("get returns Found after putFound") {
      cache.putFound(username, profile)
      cache.get(username) shouldBe CachedProfile.Found(profile)
    }

    test("get returns NotFound after putNotFound") {
      cache.putNotFound(username)
      cache.get(username) shouldBe CachedProfile.NotFound
    }

    test("putFound overwrites existing entry") {
      val newProfile = MojangProfile(MojangId(UUID.randomUUID()), username)
      cache.putFound(username, profile)
      cache.putFound(username, newProfile)
      cache.get(username) shouldBe CachedProfile.Found(newProfile)
    }

    test("putFound overwrites NotFound entry") {
      cache.putNotFound(username)
      cache.putFound(username, profile)
      cache.get(username) shouldBe CachedProfile.Found(profile)
    }

    test("putNotFound overwrites Found entry") {
      cache.putFound(username, profile)
      cache.putNotFound(username)
      cache.get(username) shouldBe CachedProfile.NotFound
    }

    test("invalidate removes entry") {
      cache.putFound(username, profile)
      cache.invalidate(username)
      cache.get(username).shouldBeNull()
    }

    test("invalidate removes NotFound entry") {
      cache.putNotFound(username)
      cache.invalidate(username)
      cache.get(username).shouldBeNull()
    }

    test("invalidate does not throw for missing key") { cache.invalidate(Username("nonexistent")) }

    test("get returns null for different usernames") {
      val other = Username("other")
      cache.putFound(username, profile)
      cache.get(other).shouldBeNull()
    }

    test("get returns cached entry when TTL has not expired") {
      val config = MojangAPIConfig()
      config.profileCacheTTL = java.time.Duration.ofHours(1)
      val longTtlCache = ProfileCache(config)

      longTtlCache.putFound(username, profile)
      longTtlCache.get(username) shouldBe CachedProfile.Found(profile)
    }
  })
