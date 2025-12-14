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

package pl.spcode.navauth.common.component

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import pl.spcode.navauth.common.extension.StringExtensions.Companion.applyPlaceholders

class TextComponent(val plainText: String) {

  companion object {
    private val miniMessage = MiniMessage.miniMessage()
  }

  fun withPlaceholders(): TextComponentPlaceholderBuilder {
    return TextComponentPlaceholderBuilder(this)
  }

  fun toComponent(): Component {
    return miniMessage.deserialize(plainText)
  }

  class TextComponentPlaceholderBuilder(val parent: TextComponent) {

    val parameters = mutableMapOf<String, Any>()

    fun placeholder(placeholder: String, value: Any): TextComponentPlaceholderBuilder {
      parameters[placeholder] = value
      return this
    }

    fun toComponent(): Component {
      val text = parent.plainText.applyPlaceholders(parameters)

      return miniMessage.deserialize(text)
    }
  }
}
