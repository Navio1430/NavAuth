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

package pl.spcode.navauth.velocity.command.configurer

import java.io.Serializable


class Command : Serializable {

  var name: String? = null
  var enabled: Boolean = false
  var aliases: MutableList<String?>? = ArrayList<String?>()
  var permissions: MutableList<String?>? = ArrayList<String?>()
  var subCommands: MutableMap<String?, SubCommand?>? = HashMap<String?, SubCommand?>()

  fun Command() {}

  fun Command(name: String?, aliases: MutableList<String?>?, permissions: MutableList<String?>?, enabled: Boolean) {
    this.name = name
    this.enabled = enabled
    this.aliases = aliases
    this.permissions = permissions
  }

  fun Command(name: String?, aliases: MutableList<String?>?, permissions: MutableList<String?>?, subCommands: MutableMap<String?, SubCommand?>?, enabled: Boolean) {
    this.name = name
    this.aliases = aliases
    this.permissions = permissions
    this.subCommands = subCommands
    this.enabled = enabled
  }

  fun name(): String? {
    return this.name
  }

  fun aliases(): MutableList<String?>? {
    return this.aliases
  }

  fun permissions(): MutableList<String?>? {
    return this.permissions
  }

  fun subCommands(): MutableMap<String?, SubCommand?>? {
    return this.subCommands
  }

  fun isEnabled(): Boolean {
    return this.enabled
  }

}