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

package pl.spcode.navauth.velocity.infra.command

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import pl.spcode.navauth.common.command.handler.InvalidUsageHandler
import pl.spcode.navauth.common.component.AudienceProvider
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.velocity.multification.VelocityMultificationProvider

class VelocityInvalidUsageHandler
@Inject
constructor(
  audienceProvider: AudienceProvider,
  multificationProvider: VelocityMultificationProvider,
  messagesConfig: MessagesConfig,
) : InvalidUsageHandler<CommandSource>(audienceProvider, multificationProvider, messagesConfig) {}
