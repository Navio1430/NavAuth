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

import com.eternalcode.multification.notice.NoticeBroadcast
import dev.rollczi.litecommands.handler.result.ResultHandlerChain
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.permission.MissingPermissions
import dev.rollczi.litecommands.permission.MissingPermissionsHandler
import pl.spcode.navauth.common.component.MultificationProvider
import pl.spcode.navauth.common.config.MessagesConfig

abstract class MissingPermissionHandler<SENDER>(
  val multificationProvider: MultificationProvider,
  val messagesConfig: MessagesConfig,
) : MissingPermissionsHandler<SENDER> {
  override fun handle(
    invocation: Invocation<SENDER>,
    missingPermissions: MissingPermissions,
    chain: ResultHandlerChain<SENDER>,
  ) {
    sendNotification(
      multificationProvider.provide(invocation),
      missingPermissions.permissions.first(),
    )
  }

  fun handle(invocation: Invocation<SENDER>, missingPermission: String) {
    sendNotification(multificationProvider.provide(invocation), missingPermission)
  }

  private fun sendNotification(multification: NoticeBroadcast<*, *, *>, missingPermission: String) {
    multification
      .notice(messagesConfig.multification.missingPermissionError)
      .placeholder("%PERM%", missingPermission)
      .send()
  }
}
