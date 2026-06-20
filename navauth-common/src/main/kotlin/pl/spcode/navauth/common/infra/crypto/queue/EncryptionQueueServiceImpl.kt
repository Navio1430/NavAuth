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

package pl.spcode.navauth.common.infra.crypto.queue

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.credentials.queue.EncryptionQueueService
import pl.spcode.navauth.common.application.credentials.queue.EncryptionTask
import pl.spcode.navauth.common.application.credentials.queue.EncryptionTaskAlreadyQueuedException
import pl.spcode.navauth.common.config.EncryptionQueueConfig

@Singleton
class EncryptionQueueServiceImpl @Inject constructor(private val config: EncryptionQueueConfig) :
  EncryptionQueueService {

  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  private val workQueue = LinkedBlockingQueue<Runnable>()
  private val activeTasks = ConcurrentHashMap<UUID, EncryptionTask>()

  private val executor: ThreadPoolExecutor =
    ThreadPoolExecutor(
      config.encryptionQueueMinThreads,
      config.encryptionQueueMaxThreads,
      60,
      TimeUnit.SECONDS,
      workQueue,
      Executors.defaultThreadFactory(),
    ) { task, _ ->
      task as EncryptionTask
      try {
        workQueue.put(task)
      } catch (e: InterruptedException) {
        activeTasks.remove(task.playerId)
        Thread.currentThread().interrupt()
        throw RejectedExecutionException("Interrupted while waiting for queue slot", e)
      }
    }

  init {
    executor.allowCoreThreadTimeOut(true)
  }

  /** @throws EncryptionTaskAlreadyQueuedException if task is already queued */
  override fun submitTask(playerId: UUID, operation: () -> Unit) {
    val task = EncryptionTask(playerId, operation)

    if (activeTasks.containsKey(playerId)) {
      throw EncryptionTaskAlreadyQueuedException()
    }
    activeTasks[playerId] = task

    executor.execute {
      val result = runCatching { task.run() }
      if (result.isFailure) {
        logger.warn("Player id='${playerId}' EncryptionTask failed", result.exceptionOrNull())
      }
      activeTasks.remove(playerId)
    }
  }

  override fun isTaskQueued(playerId: UUID): Boolean {
    return activeTasks.containsKey(playerId)
  }

  override fun dequeueTask(playerId: UUID): Boolean {
    workQueue.removeIf { it is EncryptionTask && it.playerId == playerId }

    val task = activeTasks.remove(playerId)
    if (task != null) {
      task.cancelled = true
      return true
    }

    return false
  }
}
