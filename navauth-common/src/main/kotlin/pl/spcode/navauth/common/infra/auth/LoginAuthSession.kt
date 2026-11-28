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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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

  // a configured value below 1 still allows one attempt
  private val attemptsLeft = AtomicInteger(maxLoginAttempts.coerceAtLeast(1))

  /** Set while one authentication attempt of this session is being verified. */
  private val attemptInProgress = AtomicBoolean(false)

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
   * @param password the raw (not hashed) password to authenticate the user, can be null if not
   *   required
   * @param twoFactorCode the two-factor authentication code, can be null if 2FA is not enabled
   * @return CompletableFuture with AuthTaskResult
   * @throws IllegalArgumentException if `password` or `twoFactorCode` is required by the user
   *   credentials but not provided
   * @throws EncryptionTaskAlreadyQueuedException if an encryption task is already queued or another
   *   attempt of this session is still being verified
   *
   * An attempt is not verified when the session is already authenticated (returns `Success`), was
   * invalidated or has no attempts left (returns `FailedTooManyAttempts`).
   */
  fun enqueueAuthTask(
    password: String?,
    twoFactorCode: String?,
  ): CompletableFuture<AuthTaskResult> {
    // only one attempt of this session is verified at a time, whatever the command thread
    if (!attemptInProgress.compareAndSet(false, true)) {
      throw EncryptionTaskAlreadyQueuedException()
    }

    val verification: CompletableFuture<AuthTaskResult>
    try {
      if (isAuthenticated) {
        attemptInProgress.set(false)
        return CompletableFuture.completedFuture(AuthTaskResult.Success)
      }
      if (isInvalidated || attemptsLeft.get() <= 0) {
        attemptInProgress.set(false)
        return CompletableFuture.completedFuture(AuthTaskResult.FailedTooManyAttempts)
      }
      verification = tryAuth(password, twoFactorCode)
    } catch (throwable: Throwable) {
      attemptInProgress.set(false)
      throw throwable
    }

    val resultFuture = CompletableFuture<AuthTaskResult>()
    verification.whenComplete { result, throwable ->
      val handled =
        if (throwable != null) Result.failure(throwable) else runCatching { handleResult(result) }
      // released only after the result is applied, so a following attempt sees the new state
      attemptInProgress.set(false)
      handled.fold(
        onSuccess = { resultFuture.complete(it) },
        onFailure = { resultFuture.completeExceptionally(it) },
      )
    }
    return resultFuture
  }

  private fun handleResult(result: AuthTaskResult): AuthTaskResult {
    when (result) {
      AuthTaskResult.WrongCredentials -> {
        val left = attemptsLeft.decrementAndGet()
        if (left <= 0) {
          if (left == 0) {
            onTooManyLoginAttempts()
          }
          return AuthTaskResult.FailedTooManyAttempts
        }
      }
      AuthTaskResult.Success -> {
        authenticate()
      }
      else -> {}
    }
    return result
  }

  /**
   * Validates credentials and enqueues password verification if required.
   *
   * @param password the raw password, can be null if not required
   * @param twoFactorCode the 2FA code, can be null if not required
   * @return CompletableFuture with AuthTaskResult
   * @throws IllegalArgumentException if `password` or `twoFactorCode` is required but not provided
   * @throws EncryptionTaskAlreadyQueuedException if an encryption task is already queued
   */
  private fun tryAuth(
    password: String?,
    twoFactorCode: String?,
  ): CompletableFuture<AuthTaskResult> {
    // the code is used up only when the whole login succeeds, so a wrong password does not burn it
    val codeStep: Long? =
      if (userCredentials.isTwoFactorEnabled) {
        require(twoFactorCode != null) { "twoFactorCode parameter is required by user credentials" }
        userCredentialsService.findUnusedCodeStep(userCredentials, twoFactorCode)
          ?: return CompletableFuture.completedFuture(AuthTaskResult.WrongCredentials)
      } else {
        null
      }

    if (userCredentials.isPasswordRequired) {
      val future = CompletableFuture<AuthTaskResult>()
      require(password != null) { "password parameter is required by user credentials" }
      userCredentialsService
        .enqueueVerifyPassword(userCredentials, password, playerAdapter.identifier)
        .whenComplete { isCorrect, throwable ->
          if (throwable != null) {
            future.completeExceptionally(throwable)
            return@whenComplete
          }
          future.complete(if (isCorrect) useCode(codeStep) else AuthTaskResult.WrongCredentials)
        }

      return future
    }

    // 2fa was the only one required and it passed
    return CompletableFuture.completedFuture(useCode(codeStep))
  }

  private fun useCode(codeStep: Long?): AuthTaskResult =
    if (codeStep == null || userCredentialsService.useCodeStep(userCredentials, codeStep)) {
      AuthTaskResult.Success
    } else {
      AuthTaskResult.WrongCredentials
    }

  sealed class AuthTaskResult {
    object WrongCredentials : AuthTaskResult()

    object Success : AuthTaskResult()

    object FailedTooManyAttempts : AuthTaskResult()
  }
}
