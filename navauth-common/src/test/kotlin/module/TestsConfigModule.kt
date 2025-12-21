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

package module

import com.google.inject.AbstractModule
import config.TestConfig
import kotlin.io.path.Path
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.module.PluginDirectoryModule

class TestsConfigModule : AbstractModule() {

  override fun configure() {
    bind(GeneralConfig::class.java).toInstance(TestConfig())

    install(PluginDirectoryModule(Path("")))
  }
}
