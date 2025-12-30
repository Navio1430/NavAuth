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

package pl.spcode.navauth.velocity.command

class Permissions {
  companion object {
    const val BASE = "navauth"

    const val USER_BASE = "$BASE.user"
    const val USER_LOGIN = "$USER_BASE.login"
    const val USER_REGISTER = "$USER_BASE.register"
    const val USER_UNREGISTER = "$USER_BASE.unregister"
    const val USER_CHANGE_PASSWORD = "$USER_BASE.changepassword"
    const val USER_CHANGE_TO_PREMIUM_ACCOUNT = "$USER_BASE.premium"

    const val ADMIN_BASE = "$BASE.admin"
    const val ADMIN_FORCE_UNREGISTER = "$ADMIN_BASE.forceunregister"
    const val ADMIN_FORCE_SET_PASSWORD = "$ADMIN_BASE.forcesetpassword"
    const val ADMIN_MIGRATE_USER_DATA = "$ADMIN_BASE.migrateuserdata"
  }
}
