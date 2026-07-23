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
import eu.okaeri.configs.OkaeriConfig
import kotlin.io.path.Path
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.module.PluginDirectoryModule

class TestsConfigModule : AbstractModule() {

  override fun configure() {
    val testConfig = TestConfig()

    bind(GeneralConfig::class.java).toInstance(testConfig)
    bindSubconfigsRecursively(testConfig)

    install(PluginDirectoryModule(Path("")))
  }

  private fun bindSubconfigsRecursively(parentConfig: Any) {
    val fields = mutableListOf<java.lang.reflect.Field>()
    var clazz: Class<*>? = parentConfig.javaClass
    while (clazz != null && clazz != Any::class.java) {
      clazz.declaredFields.forEach { fields.add(it) }
      clazz = clazz.superclass
    }

    fields.forEach { field ->
      field.isAccessible = true
      val value = field.get(parentConfig)
      if (value is OkaeriConfig && value != parentConfig) {
        @Suppress("UNCHECKED_CAST") bind(field.type as Class<OkaeriConfig>).toInstance(value)
        bindSubconfigsRecursively(value)
      }
    }
  }
}
