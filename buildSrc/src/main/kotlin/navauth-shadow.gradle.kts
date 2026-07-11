package buildsrc

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.gradleup.shadow")
}

val relocatePrefix = "pl.spcode.navauth.lib"

tasks.named<ShadowJar>("shadowJar") {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

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

  relocatePrefixed("kotlin")
  relocatePrefixed("org.bstats")
  relocatePrefixed("at.favre")
  relocatePrefixed("com.eternalcode")
  relocatePrefixed("com.j256")
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
  relocatePrefixed("org.h2")
  relocatePrefixed("org.intellij")
  relocatePrefixed("org.jetbrains")
  relocatePrefixed("org.postgresql")
  // todo make sure we relocate this or something
//  relocatePrefixed("org.sqlite")
  relocatePrefixed("org.yaml")
}
