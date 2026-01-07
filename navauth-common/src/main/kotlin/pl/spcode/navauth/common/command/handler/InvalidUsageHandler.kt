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

package pl.spcode.navauth.common.command.handler

import dev.rollczi.litecommands.handler.result.ResultHandlerChain
import dev.rollczi.litecommands.invalidusage.InvalidUsage
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler
import dev.rollczi.litecommands.invocation.Invocation
import pl.spcode.navauth.common.component.AudienceProvider
import pl.spcode.navauth.common.component.MultificationProvider
import pl.spcode.navauth.common.config.MessagesConfig

abstract class InvalidUsageHandler<SENDER>(
  val audienceProvider: AudienceProvider,
  val multificationProvider: MultificationProvider,
  val messagesConfig: MessagesConfig,
) : InvalidUsageHandler<SENDER> {
  override fun handle(
    invocation: Invocation<SENDER>,
    result: InvalidUsage<SENDER>,
    chain: ResultHandlerChain<SENDER>,
  ) {
    val sender = audienceProvider.getAudience(invocation)
    val multification = multificationProvider.provide(invocation)

    val schematic = result.schematic

    multification.notice(messagesConfig.multification.invalidUsageError).send()

    for (scheme in schematic.all()) {
      sender.sendMessage(
        messagesConfig.multification.invalidUsageLine
          .withPlaceholders()
          .placeholder("SCHEME", scheme)
          .toComponent()
      )
    }
  }
}
