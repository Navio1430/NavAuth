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

package pl.spcode.navauth.common.module

import com.google.inject.AbstractModule
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.application.auth.username.UsernameResolutionService
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.credentials.queue.EncryptionQueueService
import pl.spcode.navauth.common.application.mojang.ProfileService
import pl.spcode.navauth.common.application.user.UserActivitySessionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.common.TransactionService
import pl.spcode.navauth.common.infra.crypto.queue.EncryptionQueueServiceImpl
import pl.spcode.navauth.common.infra.mojang.CompositeProfileService
import pl.spcode.navauth.common.infra.mojang.MineToolsProfileService
import pl.spcode.navauth.common.infra.mojang.MojangProfileServiceImpl
import pl.spcode.navauth.common.infra.mojang.ProfileCache
import pl.spcode.navauth.common.infra.persistence.ormlite.TransactionServiceImpl

class ServicesModule : AbstractModule() {

  override fun configure() {
    bind(EncryptionQueueService::class.java).to(EncryptionQueueServiceImpl::class.java)

    bind(TransactionService::class.java).to(TransactionServiceImpl::class.java)

    bind(UsernameResolutionService::class.java)

    bind(ProfileCache::class.java)
    bind(MojangProfileServiceImpl::class.java)
    bind(MineToolsProfileService::class.java)
    bind(ProfileService::class.java).to(CompositeProfileService::class.java)

    bind(AuthHandshakeSessionService::class.java)

    bind(UserCredentialsService::class.java)
    bind(UserService::class.java)
    bind(UserActivitySessionService::class.java)

    bind(AuthSessionService::class.java)
  }
}
