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

package pl.spcode.navauth.common.infra.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm

object CredentialsTable : Table("navauth_credentials") {
  val uuid = uuidVarchar("uuid")
  // OrmLite used the field names verbatim, so these columns are camelCase in the captured DDL.
  val passwordHash = varchar("passwordHash", 255).nullable()
  val algo = enumerationByName<HashingAlgorithm>("algo", 100).nullable()
  val twoFactorSecret = varchar("twoFactorSecret", 255).nullable()

  override val primaryKey = PrimaryKey(uuid)
}
