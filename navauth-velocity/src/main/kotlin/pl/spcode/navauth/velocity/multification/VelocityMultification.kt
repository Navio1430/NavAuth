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

package pl.spcode.navauth.velocity.multification

import com.eternalcode.multification.Multification
import com.eternalcode.multification.adventure.AudienceConverter
import com.eternalcode.multification.translation.TranslationProvider
import com.eternalcode.multification.viewer.ViewerProvider
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.ComponentSerializer
import pl.spcode.navauth.common.config.MessagesConfig

open class VelocityMultification(
  private val config: MessagesConfig,
  private val provider: VelocityViewerProvider,
) : Multification<CommandSource, MessagesConfig>() {

  override fun viewerProvider(): ViewerProvider<CommandSource> {
    return provider
  }

  override fun translationProvider(): TranslationProvider<MessagesConfig> = TranslationProvider {
    config
  }

  override fun serializer(): ComponentSerializer<Component, Component, String> {
    return MiniMessage.miniMessage()
  }

  override fun audienceConverter(): AudienceConverter<CommandSource> {
    return AudienceConverter { commandSource ->
      if (commandSource is Player) {
        provider.player(commandSource.uniqueId)
      } else {
        provider.console()
      }
    }
  }
}
