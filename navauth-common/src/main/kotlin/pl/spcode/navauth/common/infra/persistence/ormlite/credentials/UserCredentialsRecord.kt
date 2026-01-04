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

package pl.spcode.navauth.common.infra.persistence.ormlite.credentials

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.UUID
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm

@DatabaseTable(tableName = "navauth_credentials")
class UserCredentialsRecord(
  // one-to-one relationship with a user entity
  @DatabaseField(id = true) var uuid: UUID = UUID.randomUUID(),
  @DatabaseField val passwordHash: String? = null,
  @DatabaseField val algo: HashingAlgorithm? = null,
  @DatabaseField val twoFactorSecret: String? = null,
) {
  init {
    if (passwordHash != null) {
      require(algo != null) { "algo must not be null" }
    }

    require(twoFactorSecret != null || passwordHash != null) { "credentials must have either password or TOTP secret" }
  }
}
