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

import pl.spcode.navauth.common.application.mojang.ProfileService
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.Username

class FakeProfileService : ProfileService {
  private val profiles = mutableMapOf<Username, MojangProfile>()

  fun addProfile(username: Username, profile: MojangProfile) {
    profiles[username] = profile
  }

  override fun fetchProfileInfo(
    usernameCaseIgnored: Username,
    useNotFoundCache: Boolean,
  ): MojangProfile? {
    return profiles[usernameCaseIgnored]
  }

  fun reset() {
    profiles.clear()
  }
}
