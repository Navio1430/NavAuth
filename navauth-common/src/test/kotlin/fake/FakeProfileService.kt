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

package fake

import pl.spcode.navauth.common.application.mojang.ProfileApiFetchException
import pl.spcode.navauth.common.application.mojang.ProfileService
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.Username

/**
 * In-memory Mojang profile API. Lookups ignore letter case like the real API. Tests simulate Mojang
 * name changes with [addProfile] / [removeProfile], and stale caches by simply not updating it.
 */
class FakeProfileService : ProfileService {

  private val profiles = mutableMapOf<String, MojangProfile>()

  /** When true, every lookup fails like an unreachable or rate-limited API. */
  var failing: Boolean = false

  fun addProfile(username: Username, profile: MojangProfile) {
    profiles[username.value.lowercase()] = profile
  }

  /** Registers [profile] under its own name. */
  fun addProfile(profile: MojangProfile) = addProfile(profile.name, profile)

  fun removeProfile(username: Username) {
    profiles.remove(username.value.lowercase())
  }

  override fun fetchProfileInfo(
    usernameCaseIgnored: Username,
    useNotFoundCache: Boolean,
  ): MojangProfile? {
    if (failing) {
      throw ProfileApiFetchException(usernameCaseIgnored, listOf(RuntimeException("API down")))
    }
    return profiles[usernameCaseIgnored.value.lowercase()]
  }

  fun reset() {
    profiles.clear()
    failing = false
  }
}
