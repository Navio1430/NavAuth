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

package extension.app

import com.google.inject.Guice
import com.google.inject.Injector
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import pl.spcode.navauth.common.infra.database.DatabaseConfig
import pl.spcode.navauth.common.infra.database.DatabaseDriverType
import pl.spcode.navauth.common.module.DataPersistenceModule
import pl.spcode.navauth.common.module.HttpClientModule
import pl.spcode.navauth.common.module.ServicesModule
import utils.GuiceUtils

class ApplicationTestExtension : TestInstancePostProcessor {

  var injector: Injector

  init {
    val config = DatabaseConfig(DatabaseDriverType.H2_MEM, 5, 30000, "", "", "", 0, "default")
    injector =
      Guice.createInjector(HttpClientModule(), DataPersistenceModule(config), ServicesModule())
  }

  override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
    GuiceUtils.Companion.injectToDeclaredFields(testInstance, injector)
  }
}
