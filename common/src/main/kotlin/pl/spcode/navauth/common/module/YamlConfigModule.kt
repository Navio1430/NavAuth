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

package pl.spcode.navauth.common.module

import com.google.inject.AbstractModule
import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer
import java.io.File
import kotlin.reflect.KClass

class YamlConfigModule<T : OkaeriConfig>(val configClass: KClass<T>, val configFile: File) :
  AbstractModule() {

  override fun configure() {

    val configInstance =
      ConfigManager.create(configClass.java) {
        it.configure { opt ->
          opt.configurer(YamlSnakeYamlConfigurer())
          opt.bindFile(configFile)
        }
        it.saveDefaults()
        it.load(true)
      }

    bind(configClass.java).toInstance(configInstance)
  }
}
