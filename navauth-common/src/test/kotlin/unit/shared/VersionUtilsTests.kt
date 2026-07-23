package unit.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pl.spcode.navauth.common.shared.utils.VersionUtils
import utils.generateRandomString

class VersionUtilsTests {

  @Test
  fun `parseVersion strips prefix and suffix`() {
    val suffix = generateRandomString(5).dropWhile { it.digitToIntOrNull() != null }
    val prefix = generateRandomString(5).dropLastWhile { it.digitToIntOrNull() != null }
    assertEquals("0.2.0", VersionUtils.parseVersion("${prefix}0.2.0$suffix"))
  }

  @Test
  fun `parseVersion returns plain version as-is`() {
    assertEquals("0.2.0", VersionUtils.parseVersion("0.2.0"))
  }

  @Test
  fun `isVersionOlderThan returns true when version is older`() {
    assertTrue(VersionUtils.isVersionOlderThan("0.1.9", "0.2.0"))
  }

  @Test
  fun `isVersionOlderThan returns true when major is older`() {
    assertTrue(VersionUtils.isVersionOlderThan("0.2.0", "1.0.0"))
  }

  @Test
  fun `isVersionOlderThan returns false when version equals minimum`() {
    assertFalse(VersionUtils.isVersionOlderThan("0.2.0", "0.2.0"))
  }

  @Test
  fun `isVersionOlderThan returns false when version is newer`() {
    assertFalse(VersionUtils.isVersionOlderThan("0.2.1", "0.2.0"))
  }

  @Test
  fun `isVersionOlderThan returns false when version is newer on minor`() {
    assertFalse(VersionUtils.isVersionOlderThan("0.3.0", "0.2.0"))
  }

  @Test
  fun `isVersionOlderThan handles SNAPSHOT versions correctly`() {
    assertFalse(VersionUtils.isVersionOlderThan("0.2.0-SNAPSHOT", "0.2.0"))
  }

  @Test
  fun `isVersionOlderThan handles newer SNAPSHOT correctly`() {
    assertFalse(VersionUtils.isVersionOlderThan("0.2.1-SNAPSHOT", "0.2.0"))
  }

  @Test
  fun `isVersionOlderThan handles older SNAPSHOT correctly`() {
    assertTrue(VersionUtils.isVersionOlderThan("0.1.9-SNAPSHOT", "0.2.0"))
  }
}
