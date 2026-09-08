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

package pl.spcode.navauth.common.infra.auth

import java.util.concurrent.CompletableFuture
import pl.spcode.navauth.api.domain.auth.AuthSessionType
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.credentials.queue.EncryptionTaskAlreadyQueuedException
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.player.DisconnectReason
import pl.spcode.navauth.common.domain.player.PlayerAdapter

open class LoginAuthSession<T : PlayerAdapter>(
  playerAdapter: T,
  val userCredentials: UserCredentials,
  private val userCredentialsService: UserCredentialsService,
  maxLoginAttempts: Int,
  eventBus: NavAuthEventBus,
) : AuthSession<T>(playerAdapter, eventBus) {

  private var attemptsLeft = maxLoginAttempts

  override fun getSessionType(): AuthSessionType {
    return AuthSessionType.LOGIN
  }

  override fun onInvalidate() {}

  open fun onTooManyLoginAttempts() {
    playerAdapter.disconnect(DisconnectReason.TOO_MANY_LOGIN_ATTEMPTS)
  }

  /**
   * Authenticates the session owner using a combination of password and two-factor authentication
   * code if required.
   *
   * @param password the raw (not hashed) password to authenticate the user
   * @param twoFactorCode the two-factor authentication code, can be null if 2FA is not enabled
   * @return CompletableFuture with AuthTaskResult
   * @throws IllegalArgumentException if `password` is null (always required)
   * @throws EncryptionTaskAlreadyQueuedException if an encryption task is already queued
   */
  fun enqueueAuthTask(
    password: String?,
    twoFactorCode: String?,
  ): CompletableFuture<AuthTaskResult> {
    return tryAuth(password, twoFactorCode).thenApply { result ->
      when (result) {
        AuthTaskResult.WrongCredentials -> {
          attemptsLeft -= 1
          if (attemptsLeft <= 0) {
            onTooManyLoginAttempts()
            return@thenApply AuthTaskResult.FailedTooManyAttempts
          }
        }
        AuthTaskResult.Success -> {
          authenticate()
        }
        else -> {}
      }
      return@thenApply result
    }
  }

  /**
   * Validates credentials and enqueues password verification.
   *
   * @param password the raw password (always required)
   * @param twoFactorCode the 2FA code, can be null if not enabled
   * @return CompletableFuture with AuthTaskResult
   * @throws IllegalArgumentException if `password` is null
   * @throws EncryptionTaskAlreadyQueuedException if an encryption task is already queued
   */
  private fun tryAuth(
    password: String?,
    twoFactorCode: String?,
  ): CompletableFuture<AuthTaskResult> {
    if (userCredentials.isTwoFactorEnabled) {
      require(twoFactorCode != null) { "twoFactorCode parameter is required by user credentials" }
      if (!userCredentialsService.verifyCode(userCredentials, twoFactorCode)) {
        return CompletableFuture.completedFuture(AuthTaskResult.WrongCredentials)
      }
    }

    // Password is always required
    val future = CompletableFuture<AuthTaskResult>()
    require(password != null) { "password parameter is required" }
    userCredentialsService
      .enqueueVerifyPassword(userCredentials, password, playerAdapter.identifier)
      .whenComplete { isCorrect, throwable ->
        if (throwable != null) {
          future.completeExceptionally(throwable)
          return@whenComplete
        }
        if (isCorrect) {
          future.complete(AuthTaskResult.Success)
        } else {
          future.complete(AuthTaskResult.WrongCredentials)
        }
      }

    return future
  }

  sealed class AuthTaskResult {
    object WrongCredentials : AuthTaskResult()

    object Success : AuthTaskResult()

    object FailedTooManyAttempts : AuthTaskResult()
  }
}
