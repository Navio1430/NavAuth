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

package unit.qr

import com.google.zxing.common.BitMatrix
import org.junit.jupiter.api.Test
import pl.spcode.navauth.common.shared.qr.QRCodeGenerator

class QRCodeGeneratorTests {

  @Test
  fun test() {
    val value = QRCodeGenerator().generateQRMatrix("test")

    val grid = gridToMinecraftUnicodeWithMiniMessage(bitMatrixToGrid(value))

    for (row in grid) {
      for (cell in row) {
        print(cell)
      }
      print("\n")
    }
  }

  fun bitMatrixToGrid(matrix: BitMatrix): List<List<Int>> {
    val grid = mutableListOf<MutableList<Int>>()
    repeat(matrix.height) { row ->
      val rowData = mutableListOf<Int>()
      repeat(matrix.width) { col -> rowData.add(if (matrix.get(col, row)) 1 else 0) }
      grid.add(rowData)
    }
    return grid
  }

  val FULL_VERT = "▋" // U+258B ▋ left five eighths (vertical full)
  val UPPER_QUAD = "▘" // U+2598 ▘ quadrant upper left
  val LOWER_QUAD = "▖" // U+2596 ▖ quadrant lower left
  val BLANK = "⠀" // U+2800 Braille blank (matches <black>⠀</black>)

  fun gridToMinecraftUnicodeWithMiniMessage(grid: List<List<Int>>): List<String> {
    val result = mutableListOf<String>()
    for (i in grid.indices step 2) {
      val upper = grid[i]
      val lowerRow = if (i + 1 < grid.size) grid[i + 1] else List(upper.size) { 0 }

      val row = buildString {
        for (j in upper.indices) {
          val u = upper[j]
          val d = lowerRow[j]
          when {
            u == 1 && d == 1 -> append(FULL_VERT) // ▋ both filled
            u == 1 -> append(UPPER_QUAD) // ▘ only top
            d == 1 -> append(LOWER_QUAD) // ▖ only bottom
            else -> append(BLANK) // ⠀ blank
          }
        }
      }
      result.add(row)
    }
    return result
  }
}
