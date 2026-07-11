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

package pl.spcode.navauth.integration.ajqueue.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import pl.spcode.navauth.common.component.TextComponent

class GeneralConfig : OkaeriConfig() {

  @Comment("Name of the ajQueue queue to send authenticated players to")
  var queueName: String = "lobby"

  @Comment("Message sent when adding player to queue fails")
  var queueAddFailMessage: TextComponent = TextComponent("<red>Failed to add you to the queue!")

  @Comment("Whether to kick the player if adding them to the queue fails")
  var kickOnQueueAddFail: Boolean = false
}
