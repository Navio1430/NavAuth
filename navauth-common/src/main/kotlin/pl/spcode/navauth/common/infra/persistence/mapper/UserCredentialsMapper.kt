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

package pl.spcode.navauth.common.infra.persistence.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.persistence.exposed.CredentialsTable

fun ResultRow.toUserCredentials(): UserCredentials {
  val passwordHash = this[CredentialsTable.passwordHash]
  val algo = this[CredentialsTable.algo]

  return UserCredentials.create(
    userUuid = UserUuid(this[CredentialsTable.uuid]),
    hashedPassword = passwordHash?.let { HashedPassword(PasswordHash(it), algo!!) },
    totpSecret = this[CredentialsTable.twoFactorSecret]?.let { TOTPSecret(it) },
  )
}
