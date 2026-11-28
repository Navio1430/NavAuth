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

package pl.spcode.navauth.common.application.credentials

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.credentials.queue.EncryptionQueueService
import pl.spcode.navauth.common.domain.common.TransactionService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.TOTP2FA

@Singleton
class UserCredentialsService
@Inject
constructor(
  val credentialsRepository: UserCredentialsRepository,
  val transactionService: TransactionService,
  val credentialsHasherFactory: CredentialsHasherFactory,
  val encryptionQueueService: EncryptionQueueService,
  @param:Named("db") val dbExecutor: ThreadPoolExecutor,
) {

  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  /** The last TOTP time step accepted per account (see [verifyCode]). */
  private val lastAcceptedTotpSteps = ConcurrentHashMap<UserUuid, Long>()

  fun findCredentials(user: User): UserCredentials? {
    return credentialsRepository.findByUser(user)
  }

  /**
   * Stores the user credentials after validation.
   *
   * @param user the user to whom the provided credentials belong
   * @param userCredentials the credentials to be stored for the user
   * @throws IllegalArgumentException if the provided credentials do not match the user or if the
   *   user does not require credentials
   */
  fun storeUserCredentials(user: User, userCredentials: UserCredentials) {
    require(userCredentials.userUuid == user.uuid) {
      "provided credentials do not belong to the provided user"
    }
    require(user.credentialsRequired) {
      "cannot store credentials for a user without credentials required property"
    }

    credentialsRepository.save(userCredentials)
  }

  /**
   * Stores the first credentials of a user that was just inserted in the same transaction. A
   * credentials row already under that uuid belongs to no account (the user insert would have
   * failed otherwise), e.g. after a user row was deleted by hand, so it is replaced.
   *
   * @throws IllegalArgumentException if the provided credentials do not match the user or if the
   *   user does not require credentials
   */
  fun storeNewUserCredentials(user: User, userCredentials: UserCredentials) {
    require(userCredentials.userUuid == user.uuid) {
      "provided credentials do not belong to the provided user"
    }
    require(user.credentialsRequired) {
      "cannot store credentials for a user without credentials required property"
    }

    credentialsRepository.deleteByUser(user)
    credentialsRepository.insert(userCredentials)
  }

  /**
   * Deletes the credentials associated with the given user.
   *
   * @param user the user whose credentials are to be deleted
   * @throws IllegalArgumentException if the user has the `credentialsRequired` property set to true
   */
  fun deleteUserCredentials(user: User) {
    require(!user.credentialsRequired) {
      "cannot delete credentials for a user with credentials required property"
    }
    credentialsRepository.deleteByUser(user)
  }

  // TODO: add full link qualifier for exception (requires new formatting plugin)
  /**
   * @param password the raw (not hashed) password
   * @throws
   *   pl.spcode.navauth.common.application.credentials.queue.EncryptionTaskAlreadyQueuedException
   *   if task is already queued
   */
  fun enqueueVerifyPassword(
    credentials: UserCredentials,
    password: String,
    playerId: UUID,
  ): CompletableFuture<Boolean> {
    require(credentials.hashedPassword != null) { "credentials must have password hash" }

    val passwordHash = credentials.hashedPassword.passwordHash
    val hasher = credentialsHasherFactory.createHasher(credentials.hashedPassword.algo)

    val future = CompletableFuture<Boolean>()
    encryptionQueueService.submitTask(
      playerId = playerId,
      operation = { finishTask ->
        try {
          val result = hasher.verify(password, passwordHash)
          finishTask()
          future.complete(result)
        } catch (ex: Exception) {
          logger.error(
            "Unexpected error occurred while trying to verify user id='${playerId}' password",
            ex,
          )
          future.completeExceptionally(ex)
        }
      },
      onCancelled = {
        future.completeExceptionally(
          CancellationException("Password verification task was cancelled")
        )
      },
    )
    return future
  }

  // TODO: add full link qualifier for exception (requires new formatting plugin)
  /**
   * @param password the raw (not hashed) password
   * @param playerId the id of the player for queue tracking
   * @throws EncryptionTaskAlreadyQueuedException if task is already queued
   */
  fun enqueueHashPassword(password: String, playerId: UUID): CompletableFuture<HashedPassword> {
    val hasher = credentialsHasherFactory.createDefaultHasher()
    val future = CompletableFuture<HashedPassword>()
    encryptionQueueService.submitTask(
      playerId = playerId,
      operation = { finishTask ->
        try {
          val result = hasher.hash(password)
          finishTask()
          future.complete(result)
        } catch (ex: Exception) {
          logger.error(
            "Unexpected error occurred while trying to hash password for player id='${playerId}'",
            ex,
          )
          future.completeExceptionally(ex)
        }
      },
      onCancelled = {
        future.completeExceptionally(CancellationException("Password hash task was cancelled"))
      },
    )
    return future
  }

  /**
   * The time step of [code] when it is a valid 2FA code that this account has not used yet, else
   * null. Does not use the code up: call [useCodeStep] once the whole login succeeded.
   */
  fun findUnusedCodeStep(credentials: UserCredentials, code: String): Long? {
    require(credentials.totpSecret != null) { "credentials do not have a TOTP secret" }
    val step = TOTP2FA().findMatchingStep(credentials.totpSecret, code) ?: return null
    val lastStep = lastAcceptedTotpSteps[credentials.userUuid]
    return if (lastStep == null || step > lastStep) step else null
  }

  /**
   * Marks the code of [step] as used for this account. False when that step or a later one was used
   * in the meantime; the code must then be refused.
   */
  fun useCodeStep(credentials: UserCredentials, step: Long): Boolean {
    var accepted = false
    lastAcceptedTotpSteps.compute(credentials.userUuid) { _, lastStep ->
      if (lastStep == null || step > lastStep) {
        accepted = true
        step
      } else {
        lastStep
      }
    }
    return accepted
  }

  /** Checks a 2FA code and uses it up: each code is accepted once per account. */
  fun verifyCode(credentials: UserCredentials, code: String): Boolean {
    val step = findUnusedCodeStep(credentials, code) ?: return false
    return useCodeStep(credentials, step)
  }

  /**
   * Updates the password for a given user to a new hashed password, keeping a 2FA secret. A
   * non-premium user without a credentials row (left without one by an older release) gets a new
   * row with the password; a premium user must already have credentials.
   *
   * @param user the user whose password needs to be updated
   * @param newPassword the new raw password to be hashed and stored
   * @throws EncryptionTaskAlreadyQueuedException if task is already queued
   */
  fun updatePassword(user: User, newPassword: String): CompletableFuture<Unit> {
    return enqueueHashPassword(newPassword, user.uuid.value)
      .thenApplyAsync(
        { hashedPassword ->
          transactionService.inTransaction {
            val credentials = findCredentials(user)
            val newCredentials =
              when {
                credentials != null -> credentials.withNewPassword(hashedPassword)
                !user.isPremium -> UserCredentials.create(user, hashedPassword, null)
                else -> throw IllegalArgumentException("user does not have credentials")
              }
            storeUserCredentials(user, newCredentials)
          }
        },
        dbExecutor,
      )
  }
}
