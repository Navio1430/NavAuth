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

package pl.spcode.navauth.common.application.credentials.queue

import java.util.UUID

interface EncryptionQueueService {

  /**
   * Submits an encryption task for the given player.
   *
   * The player's slot in the active-task registry is held from the moment this method returns until
   * [operation] invokes the `finishTask` callback it is given (or, as a fallback, until the task
   * finishes running). Calling `finishTask` early — e.g. right before completing a future with the
   * result — frees the slot for a new [submitTask] call for the same [playerId] while this task's
   * continuations are still unwinding. Calling `finishTask` more than once is safe and has no
   * effect after the first call.
   *
   * @param playerId the player this task is associated with; only one task per player may be queued
   *   or running at a time
   * @param operation the work to perform, given a `finishTask` callback that must be invoked just
   *   before committing the operation's result (e.g. right before completing a future), so the
   *   player's slot is released before any reentrant work triggered by that commit (such as a
   *   completion callback) runs
   * @param onCancelled called when the task is dequeued before execution
   * @throws EncryptionTaskAlreadyQueuedException if player has existing task queued
   */
  fun submitTask(
    playerId: UUID,
    operation: (finishTask: () -> Unit) -> Unit,
    onCancelled: () -> Unit,
  )

  fun dequeueTask(playerId: UUID): Boolean

  fun isTaskQueued(playerId: UUID): Boolean
}
