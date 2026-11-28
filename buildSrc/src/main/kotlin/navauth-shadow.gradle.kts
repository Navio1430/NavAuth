package buildsrc

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.gradleup.shadow")
}

val relocatePrefix = "pl.spcode.navauth.lib"

tasks.named<ShadowJar>("shadowJar") {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  // EXCLUDE would keep only the first jar's copy; Flyway finds its plugins only in the merged files
  filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
  mergeServiceFiles()

  fun relocatePrefixed(pkg: String) {
    relocate(pkg, "$relocatePrefix.$pkg")
  }

  relocate("com.google", "$relocatePrefix.com.google") {
    exclude("com/google/inject/**")
    exclude("com/google/gson/**")
  }

  exclude("com/google/inject/**")
  exclude("com/google/gson/**")

  exclude("org/slf4j/**")

  // also matches kotlinx (coroutines, datetime)
  relocatePrefixed("kotlin")
  relocatePrefixed("_COROUTINE")
  relocatePrefixed("org.bstats")
  relocatePrefixed("at.favre")
  relocatePrefixed("com.eternalcode")
  relocatePrefixed("com.fasterxml")
  relocatePrefixed("com.mysql")
  relocatePrefixed("com.zaxxer")
  relocatePrefixed("dev.rollczi")
  relocatePrefixed("eu.okaeri")
  relocatePrefixed("google.protobuf")
  relocatePrefixed("jakarta.inject")
  relocatePrefixed("javax.annotation")
  relocatePrefixed("org.aopalliance")
  relocatePrefixed("org.bouncycastle")
  relocatePrefixed("org.checkerframework")
  relocatePrefixed("org.flywaydb")
  // keep this name: H2 database files store class names from this package
  relocatePrefixed("org.h2")
  relocatePrefixed("org.intellij")
  relocatePrefixed("org.jetbrains")
  relocatePrefixed("org.postgresql")
  // not org.sqlite: its native code finds org/sqlite/core/NativeDB by name (NoClassDefFoundError)
  relocatePrefixed("org.yaml")
  // Jackson 3, pulled in by Flyway
  relocatePrefixed("tools.jackson")
}
