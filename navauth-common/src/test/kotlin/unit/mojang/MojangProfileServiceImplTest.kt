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

import com.google.gson.Gson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.util.UUID
import pl.spcode.navauth.common.config.MojangAPIConfig
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.mojang.MojangProfileServiceImpl

class MojangProfileServiceImplTest :
  FunSpec({
    lateinit var httpClient: HttpClient
    lateinit var response: HttpResponse<String>
    lateinit var service: MojangProfileServiceImpl

    beforeTest {
      httpClient = mockk()
      response = mockk()
      service = MojangProfileServiceImpl(httpClient, Gson(), MojangAPIConfig())
    }

    test("returns profile on successful response") {
      every { response.statusCode() } returns 200
      every { response.body() } returns
        """{"id":"853c80ef3c3749fdaa49938b674adae6","name":"Notch"}"""
      every { httpClient.send(any<HttpRequest>(), any<BodyHandler<String>>()) } returns response

      val profile = service.fetchProfileInfo(Username("notch"))

      profile.shouldNotBeNull()
      profile!!.uuid shouldBe MojangId(UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"))
      profile.name shouldBe Username("Notch")
    }

    test("returns null on 404") {
      every { response.statusCode() } returns 404
      every { httpClient.send(any<HttpRequest>(), any<BodyHandler<String>>()) } returns response

      val profile = service.fetchProfileInfo(Username("nonexistent"))

      profile.shouldBeNull()
    }

    test("throws on network error") {
      every { httpClient.send(any<HttpRequest>(), any<BodyHandler<String>>()) } throws
        RuntimeException("connection refused")

      shouldThrow<RuntimeException> { service.fetchProfileInfo(Username("notch")) }
    }
  })
