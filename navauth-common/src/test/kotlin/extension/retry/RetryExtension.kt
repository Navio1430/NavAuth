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

/** Extension originally made by: Guillaume Laforge (glaforge.dev) */
package extension.retry

import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler

class RetryExtension : TestExecutionExceptionHandler {
  private val counter = AtomicInteger(1)

  private fun printError(e: Throwable) {
    System.err.println(
      "Attempt test execution #" +
        counter.get() +
        " failed (" +
        e.javaClass.getName() +
        "thrown):  " +
        e.message
    )
  }

  @Throws(Throwable::class)
  override fun handleTestExecutionException(
    extensionContext: ExtensionContext,
    throwable: Throwable,
  ) {
    printError(throwable)

    extensionContext.testMethod.ifPresent(
      Consumer { method: Method? ->
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val maxExecutions =
          if (method!!.getAnnotation(Retry::class.java) != null)
            method.getAnnotation(Retry::class.java).value
          else 1
        while (counter.incrementAndGet() <= maxExecutions) {
          try {
            extensionContext.executableInvoker.invoke(method, extensionContext.requiredTestInstance)
            return@Consumer
          } catch (t: Throwable) {
            printError(t)

            if (counter.get() >= maxExecutions) {
              throw t
            }
          }
        }
      }
    )
  }
}
