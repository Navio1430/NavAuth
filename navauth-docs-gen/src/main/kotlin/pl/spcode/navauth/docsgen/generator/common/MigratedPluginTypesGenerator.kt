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

package pl.spcode.navauth.docsgen.generator.common

import net.steppschuh.markdowngenerator.table.Table
import pl.spcode.navauth.common.migrate.MigratedPluginType
import pl.spcode.navauth.docsgen.generator.Generator

class MigratedPluginTypesGenerator : Generator {

  override fun generate(): String {
    val tableBuilder =
      Table.Builder()
        .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER)
        .addRow("Plugin Name", "Supported")

    MigratedPluginType.entries.forEach { tableBuilder.addRow(it.name, "✅") }

    val md: StringBuilder = StringBuilder().append(tableBuilder.build())
    return md.toString()
  }

  override fun getFileName(): String {
    return "migrated-plugin-types.md"
  }
}
