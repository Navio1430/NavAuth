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

package utils

import com.google.inject.Inject
import com.google.inject.Injector

class GuiceUtils {
  companion object {
    fun injectToDeclaredFields(instance: Any, injector: Injector) {
      instance::class.java.declaredFields.forEach { field ->
        field.getAnnotation(Inject::class.java)?.let {
          field.isAccessible = true
          try {
            field.set(instance, injector.getInstance(field.type))
          } catch (e: Exception) {
            throw IllegalStateException(
              "Failed to inject field '${field.name}' of type ${field.type.name}",
              e,
            )
          }
        }
      }
    }
  }
}
