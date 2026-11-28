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

package unit.config

import com.google.inject.Guice
import eu.okaeri.configs.OkaeriConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.module.YamlConfigModule

/**
 * Configs are edited by admins and survive plugin updates. These tests load files written by an
 * older release the same way the plugin does (`YamlConfigModule`).
 */
class ConfigUpgradeTests :
  FunSpec({
    fun <T : OkaeriConfig> load(type: KClass<T>, file: File): T =
      Guice.createInjector(YamlConfigModule(type, file)).getInstance(type.java)

    fun tempFile(): File =
      Files.createTempFile("navauth-config", ".yml").toFile().also { it.delete() }

    test("general.yml from 0.3.0 with sessionsConfig loads and keeps the admin's values") {
      val file = tempFile()
      load(GeneralConfig::class, file) // writes the current defaults
      val edited =
        file
          .readText()
          .replace(
            Regex("descriptiveInvalidSessionCacheTimeMs: \\d+"),
            "descriptiveInvalidSessionCacheTimeMs: 12345",
          ) +
          "\nsessionsConfig:\n  userActivitySessionsEnabled: true\n  sessionMinimumTimeMs: 30000\n"
      file.writeText(edited)

      val config = load(GeneralConfig::class, file)

      config.descriptiveInvalidSessionCacheTimeMs shouldBe 12345
      file.readText() shouldContain "descriptiveInvalidSessionCacheTimeMs: 12345"
    }

    test("messages.yml without a key added in 0.4.0 gets its default and keeps the rest") {
      val file = tempFile()
      load(MessagesConfig::class, file)
      val withoutNewKey =
        buildList {
            var skipping = false
            for (line in file.readLines()) {
              // drop the key and its continuation lines (long values are wrapped)
              if (line.startsWith("premiumUsernameOwnedByAnotherAccountError")) skipping = true
              else if (skipping && !line.startsWith(" ")) skipping = false
              if (!skipping) add(line)
            }
          }
          .joinToString(System.lineSeparator())
          .replace(Regex("invalidUsernameError: .*"), "invalidUsernameError: <red>Custom text")
      file.writeText(withoutNewKey)
      file.readText() shouldNotContain "premiumUsernameOwnedByAnotherAccountError"

      val config = load(MessagesConfig::class, file)

      config.premiumUsernameOwnedByAnotherAccountError.plainText shouldBe
        MessagesConfig().premiumUsernameOwnedByAnotherAccountError.plainText
      file.readText() shouldContain "premiumUsernameOwnedByAnotherAccountError"
      file.readText() shouldContain "invalidUsernameError: <red>Custom text"
    }
  })
