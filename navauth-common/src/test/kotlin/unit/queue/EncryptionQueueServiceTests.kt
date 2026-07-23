package unit.queue

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import pl.spcode.navauth.common.application.credentials.queue.EncryptionTaskAlreadyQueuedException
import pl.spcode.navauth.common.config.EncryptionQueueConfig
import pl.spcode.navauth.common.infra.crypto.queue.EncryptionQueueServiceImpl

class EncryptionQueueServiceTests :
  FunSpec({
    val config =
      EncryptionQueueConfig().apply {
        encryptionQueueMinThreads = 2
        encryptionQueueMaxThreads = 4
      }

    fun createService(): EncryptionQueueServiceImpl = EncryptionQueueServiceImpl(config)

    test("submitTask executes the operation") {
      val service = createService()
      val playerId = UUID.randomUUID()
      var executed = false
      val latch = CountDownLatch(1)

      service.submitTask(
        playerId = playerId,
        operation = {
          executed = true
          latch.countDown()
        },
        onCancelled = mockk(relaxed = true),
      )

      latch.await(5, TimeUnit.SECONDS) shouldBe true
      executed shouldBe true
    }

    test("submitTask throws EncryptionTaskAlreadyQueuedException for duplicate playerId") {
      val service = createService()
      val playerId = UUID.randomUUID()
      val blocker = CountDownLatch(1)

      service.submitTask(
        playerId = playerId,
        operation = { blocker.await() },
        onCancelled = mockk(relaxed = true),
      )

      shouldThrow<EncryptionTaskAlreadyQueuedException> {
        service.submitTask(playerId = playerId, operation = {}, onCancelled = mockk(relaxed = true))
      }

      blocker.countDown()
    }

    test("submitTask allows different playerIds") {
      val service = createService()
      val latch1 = CountDownLatch(1)
      val latch2 = CountDownLatch(1)

      service.submitTask(
        playerId = UUID.randomUUID(),
        operation = { latch1.countDown() },
        onCancelled = mockk(relaxed = true),
      )
      service.submitTask(
        playerId = UUID.randomUUID(),
        operation = { latch2.countDown() },
        onCancelled = mockk(relaxed = true),
      )

      latch1.await(5, TimeUnit.SECONDS) shouldBe true
      latch2.await(5, TimeUnit.SECONDS) shouldBe true
    }

    test("dequeueTask returns false when no task exists") {
      val service = createService()
      service.dequeueTask(UUID.randomUUID()) shouldBe false
    }

    test("dequeueTask removes queued task and invokes onCancelled") {
      val config =
        EncryptionQueueConfig().apply {
          encryptionQueueMinThreads = 1
          encryptionQueueMaxThreads = 1
        }
      val service = EncryptionQueueServiceImpl(config)
      val blockerStarted = CountDownLatch(1)
      val blockerRelease = CountDownLatch(1)
      val blockerId = UUID.randomUUID()

      service.submitTask(
        playerId = blockerId,
        operation = {
          blockerStarted.countDown()
          blockerRelease.await()
        },
        onCancelled = mockk(relaxed = true),
      )

      blockerStarted.await(5, TimeUnit.SECONDS) shouldBe true

      val targetId = UUID.randomUUID()
      val opLatch = CountDownLatch(1)
      val onCancelled = mockk<() -> Unit>(relaxed = true)

      service.submitTask(
        playerId = targetId,
        operation = { opLatch.countDown() },
        onCancelled = onCancelled,
      )

      service.dequeueTask(targetId) shouldBe true
      verify { onCancelled.invoke() }

      opLatch.await(1, TimeUnit.SECONDS) shouldBe false
      service.isTaskQueued(targetId) shouldBe false

      blockerRelease.countDown()
    }

    test("dequeueTask returns true and sets cancelled for executing task") {
      val service = createService()
      val opStarted = CountDownLatch(1)
      val opRelease = CountDownLatch(1)
      val playerId = UUID.randomUUID()

      service.submitTask(
        playerId = playerId,
        operation = {
          opStarted.countDown()
          opRelease.await()
        },
        onCancelled = mockk(relaxed = true),
      )

      opStarted.await(5, TimeUnit.SECONDS) shouldBe true

      service.dequeueTask(playerId) shouldBe true
      service.isTaskQueued(playerId) shouldBe false

      opRelease.countDown()
    }

    test("onCancelled is not called when task executes normally") {
      val service = createService()
      val playerId = UUID.randomUUID()
      val latch = CountDownLatch(1)
      val onCancelled = mockk<() -> Unit>(relaxed = true)

      service.submitTask(
        playerId = playerId,
        operation = { latch.countDown() },
        onCancelled = onCancelled,
      )

      latch.await(5, TimeUnit.SECONDS) shouldBe true
      verify(inverse = true) { onCancelled.invoke() }
    }

    test("isTaskQueued reflects current state") {
      val service = createService()
      val playerId = UUID.randomUUID()
      val blocker = CountDownLatch(1)
      val done = CountDownLatch(1)

      service.isTaskQueued(playerId) shouldBe false

      service.submitTask(
        playerId = playerId,
        operation = {
          blocker.await()
          done.countDown()
        },
        onCancelled = mockk(relaxed = true),
      )

      service.isTaskQueued(playerId) shouldBe true

      blocker.countDown()
      done.await(5, TimeUnit.SECONDS) shouldBe true

      Thread.sleep(100)
      service.isTaskQueued(playerId) shouldBe false
    }

    test("EncryptionTaskAlreadyQueuedException is thrown and caught correctly") {
      val service = createService()
      val playerId = UUID.randomUUID()
      val blocker = CountDownLatch(1)

      service.submitTask(
        playerId = playerId,
        operation = { blocker.await() },
        onCancelled = mockk(relaxed = true),
      )

      shouldThrow<EncryptionTaskAlreadyQueuedException> {
        service.submitTask(playerId = playerId, operation = {}, onCancelled = mockk(relaxed = true))
      }

      blocker.countDown()
    }

    test("dequeueTask after task already completed returns false") {
      val service = createService()
      val playerId = UUID.randomUUID()
      val latch = CountDownLatch(1)

      service.submitTask(
        playerId = playerId,
        operation = { latch.countDown() },
        onCancelled = mockk(relaxed = true),
      )

      latch.await(5, TimeUnit.SECONDS) shouldBe true

      Thread.sleep(200)
      service.dequeueTask(playerId) shouldBe false
    }
  })
