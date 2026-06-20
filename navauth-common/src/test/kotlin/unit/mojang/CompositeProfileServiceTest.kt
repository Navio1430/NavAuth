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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.util.UUID
import pl.spcode.navauth.common.config.MojangAPIConfig
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.mojang.MojangProfileApi
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.mojang.CompositeProfileService
import pl.spcode.navauth.common.infra.mojang.MineToolsProfileService
import pl.spcode.navauth.common.infra.mojang.MojangProfileServiceImpl
import pl.spcode.navauth.common.infra.mojang.ProfileCache

class CompositeProfileServiceTest :
  FunSpec({
    lateinit var config: MojangAPIConfig
    lateinit var mineTools: MineToolsProfileService
    lateinit var mojang: MojangProfileServiceImpl
    lateinit var cache: ProfileCache
    lateinit var service: CompositeProfileService

    val username = Username("test")
    val minetoolsProfile = MojangProfile(MojangId(UUID.randomUUID()), Username("MinetoolsPlayer"))
    val mojangProfile = MojangProfile(MojangId(UUID.randomUUID()), Username("MojangPlayer"))

    beforeTest {
      config = MojangAPIConfig()
      mineTools = mockk()
      mojang = mockk()
      cache = mockk()
      every { cache.put(any(), any()) } just runs
      service = CompositeProfileService(config, mojang, mineTools, cache)
    }

    test("returns cached profile without calling APIs") {
      every { cache.get(username) } returns minetoolsProfile

      val result = service.fetchProfileInfo(username)

      result shouldBe minetoolsProfile
    }

    test("queries minetools first (default order) and returns its result") {
      every { cache.get(username) } returns null
      every { mineTools.fetchProfileInfo(username) } returns minetoolsProfile

      val result = service.fetchProfileInfo(username)

      result shouldBe minetoolsProfile
    }

    test("falls back to mojang when minetools returns null") {
      every { cache.get(username) } returns null
      every { mineTools.fetchProfileInfo(username) } returns null
      every { mojang.fetchProfileInfo(username) } returns mojangProfile

      val result = service.fetchProfileInfo(username)

      result shouldBe mojangProfile
    }

    test("falls back to mojang when minetools throws") {
      every { cache.get(username) } returns null
      every { mineTools.fetchProfileInfo(username) } throws RuntimeException("minetools error")
      every { mojang.fetchProfileInfo(username) } returns mojangProfile

      val result = service.fetchProfileInfo(username)

      result shouldBe mojangProfile
    }

    test("returns null when all APIs return null") {
      every { cache.get(username) } returns null
      every { mineTools.fetchProfileInfo(username) } returns null
      every { mojang.fetchProfileInfo(username) } returns null

      val result = service.fetchProfileInfo(username)

      result.shouldBeNull()
    }

    test("throws when all APIs throw") {
      every { cache.get(username) } returns null
      every { mineTools.fetchProfileInfo(username) } throws RuntimeException("minetools error")
      every { mojang.fetchProfileInfo(username) } throws RuntimeException("mojang error")

      shouldThrow<RuntimeException> { service.fetchProfileInfo(username) }
    }

    test("returns minetools result and caches it") {
      every { cache.get(username) } returns null
      every { mineTools.fetchProfileInfo(username) } returns minetoolsProfile
      every { cache.put(username, minetoolsProfile) } returns Unit

      val result = service.fetchProfileInfo(username)

      result shouldBe minetoolsProfile
    }

    test("falls back when minetools throws, returns mojang result and caches it") {
      every { cache.get(username) } returns null
      every { mineTools.fetchProfileInfo(username) } throws RuntimeException("minetools error")
      every { mojang.fetchProfileInfo(username) } returns mojangProfile
      every { cache.put(username, mojangProfile) } returns Unit

      val result = service.fetchProfileInfo(username)

      result shouldBe mojangProfile
    }

    test("uses configured order: MOJANG first then MINETOOLS") {
      config.apiOrder = listOf(MojangProfileApi.MOJANG, MojangProfileApi.MINETOOLS)
      every { cache.get(username) } returns null
      every { mojang.fetchProfileInfo(username) } returns mojangProfile

      val result = service.fetchProfileInfo(username)

      result shouldBe mojangProfile
    }

    test("returns first result from configured order with single API") {
      config.apiOrder = listOf(MojangProfileApi.MOJANG)
      every { cache.get(username) } returns null
      every { mojang.fetchProfileInfo(username) } returns mojangProfile

      val result = service.fetchProfileInfo(username)

      result shouldBe mojangProfile
    }
  })
