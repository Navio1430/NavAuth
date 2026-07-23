package pl.spcode.navauth.common.shared.utils

class VersionUtils {

  companion object {
    private val semverRegex = Regex("""(\d+\.\d+\.\d+)""")

    fun parseVersion(version: String): String {
      return semverRegex.find(version)?.value ?: version
    }

    fun isVersionOlderThan(version: String, minimum: String): Boolean {
      val versionParts = parseVersion(version).split(".").map { it.toIntOrNull() ?: 0 }
      val minParts = parseVersion(minimum).split(".").map { it.toIntOrNull() ?: 0 }

      if (versionParts.size < 3 || minParts.size < 3) return true
      if (versionParts[0] < minParts[0]) return true
      if (versionParts[0] == minParts[0] && versionParts[1] < minParts[1]) return true
      return versionParts[0] == minParts[0] &&
        versionParts[1] == minParts[1] &&
        versionParts[2] < minParts[2]
    }
  }
}
