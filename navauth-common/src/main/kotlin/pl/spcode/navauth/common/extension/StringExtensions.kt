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

package pl.spcode.navauth.common.extension

import java.util.regex.Pattern

class StringExtensions {

  companion object {
    /**
     * This function uses %KEY% formatted placeholders by default.
     *
     * @param parameters map of keys to replace with their corresponding values.
     * @return formatted string with replaced placeholders
     */
    fun String.applyPlaceholders(parameters: Map<String, Any?>): String {
      val newTemplate = StringBuilder(this)
      val valueList = mutableListOf<Any?>()

      val matcher = Pattern.compile("%(\\w+)%").matcher(this) // Match %key%

      while (matcher.find()) {
        val key = matcher.group(1) ?: continue
        val paramName = "%$key%" // Correct: %key%

        val index = newTemplate.indexOf(paramName)
        if (index != -1) {
          newTemplate.replace(index, index + paramName.length, "%s")
          valueList.add(parameters[key])
        }
      }

      return String.format(newTemplate.toString(), *valueList.toTypedArray())
    }
  }
}
