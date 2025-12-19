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

package pl.spcode.navauth.common.infra.crypto

import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm

/**
 * Represents a hashed password.
 *
 * Always includes a salted hash to ensure enhanced security and resistance against common
 * dictionary or brute-force attacks.
 *
 * @property value The hashed password string with applied salt. Format may differ between
 *   implementations.
 */
@JvmInline value class PasswordHash(val value: String)

/**
 * Represents a hashed password and the algorithm used to hash it.
 *
 * @property hash The hashed password value.
 * @property algo The algorithm used to generate the hash.
 */
data class HashedPassword(val hash: PasswordHash, val algo: HashingAlgorithm)
