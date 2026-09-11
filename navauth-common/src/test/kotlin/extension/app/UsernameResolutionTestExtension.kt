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

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.util.Modules
import fake.FakeProfileService
import module.TestsConfigModule
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import pl.spcode.navauth.common.application.mojang.ProfileService
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.module.DataPersistenceModule
import pl.spcode.navauth.common.module.EventsModule
import pl.spcode.navauth.common.module.ExecutorsModule
import pl.spcode.navauth.common.module.HttpClientModule
import pl.spcode.navauth.common.module.ServicesModule
import utils.GuiceUtils

class UsernameResolutionTestExtension : TestInstancePostProcessor {

  private val fakeProfileService = FakeProfileService()

  var injector: Injector =
    Guice.createInjector(
      Modules.override(
          EventsModule(),
          TestsConfigModule(),
          HttpClientModule(),
          DataPersistenceModule(),
          ExecutorsModule(),
          ServicesModule(),
        )
        .with(
          object : AbstractModule() {
            override fun configure() {
              bind(FakeProfileService::class.java).toInstance(fakeProfileService)
              bind(ProfileService::class.java).toInstance(fakeProfileService)
            }
          }
        )
    )

  init {
    injector.getInstance(DatabaseManager::class.java).connectAndInit()
  }

  override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
    GuiceUtils.Companion.injectToDeclaredFields(testInstance, injector)
  }
}
