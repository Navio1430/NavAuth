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
import java.time.Duration
import pl.spcode.navauth.common.domain.mojang.MojangProfileApi

class MojangAPIConfig : OkaeriConfig() {

  @Comment("How long to cache resolved Mojang profiles before re-fetching")
  var profileCacheTTL: Duration = Duration.ofMinutes(5)

  @Comment(
    "Order of API providers to use when resolving a Mojang profile.",
    "Available providers: MINETOOLS, MOJANG",
    "The first provider in this list that returns a result will be used.",
  )
  var apiOrder: List<MojangProfileApi> = listOf(MojangProfileApi.MINETOOLS, MojangProfileApi.MOJANG)

  @Comment("Timeout for each individual API call") var apiTimeout: Duration = Duration.ofSeconds(5)
}
