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

package unit.auth

import com.google.common.base.Ticker
import com.google.inject.Guice
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.domain.auth.UniqueSessionId
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType

/** A clock that moves only when a test advances it. */
class FakeTicker : Ticker() {
  private val nanos = AtomicLong()

  override fun read(): Long = nanos.get()

  fun advance(duration: Duration) {
    nanos.addAndGet(duration.toNanos())
  }
}

/** L26: a handshake session (PreLogin until PostLogin) lives 15 seconds. */
class AuthHandshakeSessionServiceTests :
  FunSpec({
    val sessionId =
      object : UniqueSessionId() {
        override val id = "Player:192.0.2.1:54321"
      }
    val premium =
      UsernameResResult.Success(
        EncryptionType.ENFORCE_PREMIUM,
        PostUsernameResolutionState.NO_CHANGE,
      )

    test("a session lives 15 seconds after it was created") {
      AuthHandshakeSessionService.SESSION_TTL shouldBe Duration.ofSeconds(15)
      val ticker = FakeTicker()
      val service = AuthHandshakeSessionService(ticker)
      service.createSession(sessionId, null, "Player", premium)

      ticker.advance(Duration.ofSeconds(15).minusMillis(1))
      service.findSession(sessionId).shouldNotBeNull()

      ticker.advance(Duration.ofMillis(1))
      service.findSession(sessionId).shouldBeNull()
    }

    test("a new session for the same id starts a new 15 seconds") {
      val ticker = FakeTicker()
      val service = AuthHandshakeSessionService(ticker)
      service.createSession(sessionId, null, "Player", premium)

      ticker.advance(Duration.ofSeconds(10))
      service.createSession(sessionId, null, "Player", premium)
      ticker.advance(Duration.ofSeconds(10))

      service.findSession(sessionId).shouldNotBeNull()
    }

    test("Guice creates the service with the system clock") {
      val service = Guice.createInjector().getInstance(AuthHandshakeSessionService::class.java)
      service.createSession(sessionId, null, "Player", premium)

      service.findSession(sessionId).shouldNotBeNull()
    }
  })
