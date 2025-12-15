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

package pl.spcode.navauth.docsgen.generator.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import net.steppschuh.markdowngenerator.table.Table
import pl.spcode.navauth.docsgen.generator.Generator
import pl.spcode.navauth.velocity.listener.VelocityListenersRegistry

class EventsGenerator : Generator {

  override fun generate(): String {
    val tableBuilder =
      Table.Builder()
        .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
        .addRow("Event name", "Priority order")

    VelocityListenersRegistry.Companion.listenersClasses.forEach { kClass ->
      kClass.declaredMemberFunctions.forEach { fn ->
        val firstParam = fn.parameters.getOrNull(1) ?: return@forEach // 0 is "this"
        val paramType = firstParam.type

        // Check if parameter is a Velocity event
        val isVelocityEvent =
          paramType.classifier is KClass<*> &&
            (paramType.classifier as KClass<*>)
              .qualifiedName
              ?.startsWith("com.velocitypowered.api.event") == true

        if (!isVelocityEvent) return@forEach

        val subscribe = fn.findAnnotation<Subscribe>()
        val order = subscribe?.order ?: PostOrder.NORMAL

        val eventName = paramType.toString().substringAfterLast('.')
        tableBuilder.addRow(eventName, order)

        //        println("eventType: ${paramType.toString().substringAfterLast('.')}, order:
        // $order")
      }
    }

    val md: StringBuilder = StringBuilder().append(tableBuilder.build())

    return md.toString()
  }

  override fun getFileName(): String {
    return "events-table.md"
  }
}
