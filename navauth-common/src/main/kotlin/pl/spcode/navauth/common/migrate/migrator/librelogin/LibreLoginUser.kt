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

import java.util.UUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table

/**
 * LibreLogin's user table. Every column is read as nullable text, so a damaged row (missing
 * nickname, malformed uuid) is skipped by the migrator instead of failing the whole chunk.
 */
object LibreLoginUserTable : Table("librepremium_data") {
  val uuid = varchar("uuid", 255).nullable()
  val premiumUuid = varchar("premium_uuid", 255).nullable()
  val passwordHash = varchar("hashed_password", 255).nullable()
  val passwordSalt = varchar("salt", 255).nullable()
  val passwordAlgo = varchar("algo", 255).nullable()
  val lastNickname = varchar("last_nickname", 255).nullable()
  val secret = varchar("secret", 255).nullable()
}

/** One `librepremium_data` row as stored, before validation. */
class LibreLoginRow(
  val uuid: String?,
  val premiumUuid: String?,
  val passwordHash: String?,
  val passwordSalt: String?,
  val passwordAlgo: String?,
  val lastNickname: String?,
  val secret: String?,
)

/** A LibreLogin user whose uuids were parsed. */
class LibreLoginUser(
  val uuid: UUID?,
  val premiumUuid: UUID?,
  val passwordHash: String?,
  val passwordSalt: String?,
  val passwordAlgo: String?,
  val lastNickname: String?,
  val secret: String?,
)

fun ResultRow.toLibreLoginRow(): LibreLoginRow =
  LibreLoginRow(
    uuid = this[LibreLoginUserTable.uuid],
    premiumUuid = this[LibreLoginUserTable.premiumUuid],
    passwordHash = this[LibreLoginUserTable.passwordHash],
    passwordSalt = this[LibreLoginUserTable.passwordSalt],
    passwordAlgo = this[LibreLoginUserTable.passwordAlgo],
    lastNickname = this[LibreLoginUserTable.lastNickname],
    secret = this[LibreLoginUserTable.secret],
  )
