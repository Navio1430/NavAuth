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

package pl.spcode.navauth.common.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment

class SessionsConfig : OkaeriConfig() {

  @Comment(
    "Should we store player sessions?",
    "https://navio1430.github.io/NavAuth/docs/general/user-lookup.html#lookup-user-sessions",
    "Make sure you've included information about this function in your Privacy Policy.",
    "We store join at, left at times and IP's.",
  )
  var userActivitySessionsEnabled = false

  @Comment("Minimum time of milliseconds required to save a player activity session.")
  var sessionMinimumTimeMs = 30000
}
