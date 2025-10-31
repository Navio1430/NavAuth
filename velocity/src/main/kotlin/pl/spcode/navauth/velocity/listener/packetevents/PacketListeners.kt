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

package pl.spcode.navauth.velocity.listener.packetevents

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PacketListeners : PacketListener {

  val logger: Logger = LoggerFactory.getLogger(PacketListeners::class.java)

  override fun onPacketReceive(event: PacketReceiveEvent) {
    if (event.packetType == PacketType.Login.Client.ENCRYPTION_RESPONSE) {
      val encryptionResponse = WrapperLoginClientEncryptionResponse(event)
      logger.debug("got encryption response: {}", encryptionResponse.toString())
    }
  }

  override fun onPacketSend(event: PacketSendEvent) {
    if (event.packetType == PacketType.Login.Server.ENCRYPTION_REQUEST) {
      val encryptionRequest = WrapperLoginServerEncryptionRequest(event)
      logger.debug("sent encryption request: {}", encryptionRequest.toString())
    }
  }
}
