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

package pl.spcode.navauth.common.config.serdes

import eu.okaeri.configs.schema.GenericsDeclaration
import eu.okaeri.configs.serdes.DeserializationData
import eu.okaeri.configs.serdes.ObjectSerializer
import eu.okaeri.configs.serdes.SerializationData
import pl.spcode.navauth.common.component.TextComponent

class TextComponentSerializer : ObjectSerializer<TextComponent> {

  override fun supports(type: Class<*>): Boolean {
    return TextComponent::class.java.isAssignableFrom(type)
  }

  override fun serialize(
    obj: TextComponent,
    data: SerializationData,
    generics: GenericsDeclaration,
  ) {
    data.setValue(obj.plainText)
  }

  override fun deserialize(
    data: DeserializationData,
    generics: GenericsDeclaration,
  ): TextComponent {
    return TextComponent(data.getValue(String::class.java))
  }
}
