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

package unit.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import pl.spcode.navauth.api.event.EventPriority
import pl.spcode.navauth.api.event.NavAuthEvent
import pl.spcode.navauth.api.event.NavAuthEventListener
import pl.spcode.navauth.api.event.Subscribe
import pl.spcode.navauth.common.infra.NavAuthEventBusInternal

class PriorityTestEvent : NavAuthEvent

/** Records its id in [calls] when it receives a [PriorityTestEvent]. */
abstract class RecordingListener(
  val id: Int,
  val priority: Int,
  private val calls: MutableList<Int>,
) : NavAuthEventListener {
  protected fun record() {
    calls.add(id)
  }
}

class BelowLowestListener(id: Int, calls: MutableList<Int>) : RecordingListener(id, -5, calls) {
  @Subscribe(priority = -5) fun on(event: PriorityTestEvent) = record()
}

class LowestListener(id: Int, calls: MutableList<Int>) :
  RecordingListener(id, EventPriority.LOWEST, calls) {
  @Subscribe(priority = EventPriority.LOWEST) fun on(event: PriorityTestEvent) = record()
}

class LowListener(id: Int, calls: MutableList<Int>) :
  RecordingListener(id, EventPriority.LOW, calls) {
  @Subscribe(priority = EventPriority.LOW) fun on(event: PriorityTestEvent) = record()
}

class DefaultPriorityListener(id: Int, calls: MutableList<Int>) :
  RecordingListener(id, EventPriority.NORMAL, calls) {
  @Subscribe fun on(event: PriorityTestEvent) = record()
}

class HighListener(id: Int, calls: MutableList<Int>) :
  RecordingListener(id, EventPriority.HIGH, calls) {
  @Subscribe(priority = EventPriority.HIGH) fun on(event: PriorityTestEvent) = record()
}

class HighestListener(id: Int, calls: MutableList<Int>) :
  RecordingListener(id, EventPriority.HIGHEST, calls) {
  @Subscribe(priority = EventPriority.HIGHEST) fun on(event: PriorityTestEvent) = record()
}

class AboveHighestListener(id: Int, calls: MutableList<Int>) : RecordingListener(id, 100, calls) {
  @Subscribe(priority = 100) fun on(event: PriorityTestEvent) = record()
}

class ThrowingListener : NavAuthEventListener {
  @Subscribe(priority = EventPriority.HIGHEST)
  fun on(event: PriorityTestEvent): Unit = throw IllegalStateException("listener failure")
}

private val factories: List<(Int, MutableList<Int>) -> RecordingListener> =
  listOf(
    ::BelowLowestListener,
    ::LowestListener,
    ::LowListener,
    ::DefaultPriorityListener,
    ::HighListener,
    ::HighestListener,
    ::AboveHighestListener,
  )

/** Higher priority first; equal priorities in registration (id) order. */
private fun expectedOrder(listeners: List<RecordingListener>): List<Int> =
  listeners
    .sortedWith(compareByDescending<RecordingListener> { it.priority }.thenBy { it.id })
    .map {
      it.id
    }

class EventBusPriorityTests :
  FunSpec({
    test("public priority constants keep their values") {
      listOf(
        EventPriority.LOWEST,
        EventPriority.LOW,
        EventPriority.NORMAL,
        EventPriority.HIGH,
        EventPriority.HIGHEST,
      ) shouldBe listOf(0, 10, 20, 30, 40)
    }

    test("listeners are called in priority order across many registrations") {
      val bus = NavAuthEventBusInternal()
      val calls = CopyOnWriteArrayList<Int>()
      val random = Random(1430)
      val listeners =
        (0 until 300).map { id -> factories[random.nextInt(factories.size)](id, calls) }
      listeners.forEach(bus::register)

      repeat(3) {
        calls.clear()
        bus.post(PriorityTestEvent())
        calls.toList() shouldBe expectedOrder(listeners)
      }
    }

    test("listeners with equal priority are called in registration order") {
      val bus = NavAuthEventBusInternal()
      val calls = CopyOnWriteArrayList<Int>()
      val listeners = (0 until 100).map { id -> HighListener(id, calls) }
      listeners.forEach(bus::register)

      bus.post(PriorityTestEvent())

      calls.toList() shouldBe (0 until 100).toList()
    }

    test("registering in reverse priority order still calls higher priorities first") {
      val bus = NavAuthEventBusInternal()
      val calls = CopyOnWriteArrayList<Int>()
      val listeners = factories.mapIndexed { id, factory -> factory(id, calls) }
      listeners.forEach(bus::register)

      bus.post(PriorityTestEvent())

      calls.toList() shouldBe listOf(6, 5, 4, 3, 2, 1, 0)
    }

    test("unregistered listeners are not called and the rest keep their order") {
      val bus = NavAuthEventBusInternal()
      val calls = CopyOnWriteArrayList<Int>()
      val random = Random(20)
      val listeners =
        (0 until 120).map { id -> factories[random.nextInt(factories.size)](id, calls) }
      listeners.forEach(bus::register)

      val removed = listeners.filter { it.id % 3 == 0 }
      removed.forEach(bus::unregister)
      bus.post(PriorityTestEvent())
      calls.toList() shouldBe expectedOrder(listeners - removed.toSet())

      // registered again, they run after the listeners of the same priority that stayed
      calls.clear()
      removed.forEach(bus::register)
      bus.post(PriorityTestEvent())
      val stayed = expectedOrder(listeners - removed.toSet())
      val reRegistered = removed.map { it.id }.toSet()
      calls.toList() shouldBe
        listeners
          .sortedWith(
            compareByDescending<RecordingListener> { it.priority }
              .thenBy { if (it.id in reRegistered) 1 else 0 }
              .thenBy { it.id }
          )
          .map { it.id }
      calls.filter { it !in reRegistered } shouldBe stayed

      calls.clear()
      listeners.forEach(bus::unregister)
      bus.post(PriorityTestEvent())
      calls.toList() shouldBe emptyList()
    }

    test("a failing listener does not stop the listeners after it") {
      val bus = NavAuthEventBusInternal()
      val calls = CopyOnWriteArrayList<Int>()
      bus.register(ThrowingListener())
      bus.register(LowListener(1, calls))
      bus.register(HighListener(0, calls))

      bus.post(PriorityTestEvent())

      calls.toList() shouldBe listOf(0, 1)
    }
  })
