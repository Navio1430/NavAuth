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
import pl.spcode.navauth.common.infra.mojang.ProfileCache

class ProfileCacheTest :
  FunSpec({
    lateinit var cache: ProfileCache
    val username = Username("test")
    val profile = MojangProfile(MojangId(UUID.randomUUID()), username)

    beforeTest { cache = ProfileCache(MojangAPIConfig()) }

    test("get returns null for uncached username") { cache.get(username).shouldBeNull() }

    test("get returns profile after put") {
      cache.put(username, profile)
      cache.get(username) shouldBe profile
    }

    test("put overwrites existing entry") {
      val newProfile = MojangProfile(MojangId(UUID.randomUUID()), username)
      cache.put(username, profile)
      cache.put(username, newProfile)
      cache.get(username) shouldBe newProfile
    }

    test("invalidate removes entry") {
      cache.put(username, profile)
      cache.invalidate(username)
      cache.get(username).shouldBeNull()
    }

    test("invalidate does not throw for missing key") { cache.invalidate(Username("nonexistent")) }

    test("get returns null for different usernames") {
      val other = Username("other")
      cache.put(username, profile)
      cache.get(other).shouldBeNull()
    }

    test("get returns cached entry when TTL has not expired") {
      val config = MojangAPIConfig()
      config.profileCacheTTL = java.time.Duration.ofHours(1)
      val longTtlCache = ProfileCache(config)

      longTtlCache.put(username, profile)
      longTtlCache.get(username) shouldBe profile
    }
  })
