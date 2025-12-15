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

package pl.spcode.navauth.docsgen

import java.io.FileWriter
import kotlin.io.path.Path
import kotlin.system.exitProcess
import pl.spcode.navauth.docsgen.generator.velocity.EventsGenerator

val generatorsRegistry = listOf(EventsGenerator())

fun generateFiles() {
  val path = System.getenv("TARGET_PATH")
  if (path == null) {
    System.err.println("TARGET_PATH not specified")
    exitProcess(-1)
  }

  val folderPath = Path(path)

  generatorsRegistry.forEach {
    val fileName = it.getFileName()
    println("generating $fileName...")
    val content = it.generate()
    val file = folderPath.resolve(fileName).toFile()

    FileWriter(file).use { writer -> writer.write(content) }

    println("success.. $fileName")
  }
}

fun main(args: Array<String>) {
  try {
    generateFiles()
  } catch (e: Exception) {
    System.err.println("unexpected error occurred")
    e.printStackTrace()
  }
}
