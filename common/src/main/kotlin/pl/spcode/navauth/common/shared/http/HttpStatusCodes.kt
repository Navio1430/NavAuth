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

package pl.spcode.navauth.common.shared.http

object HttpStatusCodes {
  const val CONTINUE = 100
  const val SWITCHING_PROTOCOLS = 101
  const val OK = 200
  const val CREATED = 201
  const val ACCEPTED = 202
  const val NO_CONTENT = 204
  const val BAD_REQUEST = 400
  const val UNAUTHORIZED = 401
  const val FORBIDDEN = 403
  const val NOT_FOUND = 404
  const val METHOD_NOT_ALLOWED = 405
  const val INTERNAL_SERVER_ERROR = 500
  const val NOT_IMPLEMENTED = 501
  const val BAD_GATEWAY = 502
  const val SERVICE_UNAVAILABLE = 503
}