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

package pl.spcode.navauth.common.migrate.migrator.librelogin

import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword

/** The NavAuth rows one LibreLogin user turns into. */
data class LibreLoginConvertedUser(val user: User, val credentials: UserCredentials?)

/**
 * Converts one LibreLogin user into NavAuth rows. The LibreLogin `uuid` (offline, random or
 * premium, depending on LibreLogin's UUID creator) stays the NavAuth user UUID, so backend data
 * keyed by it is kept.
 *
 * Premium players (`premium_uuid` set) log in through Mojang, exactly as after `/premium` in
 * NavAuth: a password LibreLogin kept for them is dropped, and credentials are required only when
 * they have 2FA.
 *
 * @param hashedPassword the converted LibreLogin password; ignored for premium players
 * @return null for a non-premium user without a password and 2FA secret (cannot log in)
 */
fun convertLibreLoginUser(
  lUser: LibreLoginUser,
  username: Username,
  hashedPassword: HashedPassword?,
): LibreLoginConvertedUser? {
  val userUuid = UserUuid(requireNotNull(lUser.uuid) { "LibreLogin user without uuid" })
  val totpSecret = lUser.secret?.let { TOTPSecret(it) }
  val premiumUuid = lUser.premiumUuid

  if (premiumUuid != null) {
    val credentials = totpSecret?.let { UserCredentials.create(userUuid, null, it) }
    val user = User.premium(userUuid, username, MojangId(premiumUuid), credentials != null)
    return LibreLoginConvertedUser(user, credentials)
  }

  if (hashedPassword == null && totpSecret == null) return null
  return LibreLoginConvertedUser(
    User.nonPremium(userUuid, username),
    UserCredentials.create(userUuid, hashedPassword, totpSecret),
  )
}
