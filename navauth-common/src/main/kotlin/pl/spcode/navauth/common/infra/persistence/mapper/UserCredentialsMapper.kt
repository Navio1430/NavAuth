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

import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.persistence.ormlite.credentials.UserCredentialsRecord

fun UserCredentialsRecord.toDomain(): UserCredentials {
  return UserCredentials.create(
    userUuid = UserUuid(uuid),
    hashedPassword =
      passwordHash?.let {
        return@let HashedPassword(PasswordHash(it), algo!!)
      },
    totpSecret =
      twoFactorSecret?.let {
        return@let TOTPSecret(it)
      },
  )
}

fun UserCredentials.toRecord(): UserCredentialsRecord =
  UserCredentialsRecord(
    uuid = userUuid.value,
    passwordHash = hashedPassword?.passwordHash?.value,
    algo = hashedPassword?.algo,
    twoFactorSecret = totpSecret?.value,
  )
