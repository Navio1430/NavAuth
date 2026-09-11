plugins {
  id("buildsrc.navauth-shadow")
  id("net.kyori.blossom") version "2.2.0"
  kotlin("jvm")
  kotlin("kapt")
}

val projectName = "navauth-ajqueue-integration"

group = "pl.spcode.navauth"
version = "1.0.0-SNAPSHOT"

val requiredMinNavAuthVersion = "0.2.0" // always X.Y.Z format

sourceSets {
  main {
    blossom {
      kotlinSources {
        property("version", project.version.toString())
        property("requiredMinNavAuthVersion", requiredMinNavAuthVersion)
      }
    }
  }
}

tasks.shadowJar {
  destinationDirectory.set(file("../../target"))

  archiveBaseName.set(projectName)
  archiveClassifier = null

  exclude("kotlin/**")
  exclude("META-INF/**")
  exclude("org/jetbrains/**")
  exclude("org/intellij/**")
}

repositories {
  maven { url = uri("https://repo.ajg0702.us/releases") }
}

dependencies {
  compileOnly(project(":navauth-common"))
  compileOnly(libs.velocitypowered.velocity.api)
  kapt(libs.velocitypowered.velocity.api)
  compileOnly(libs.ajqueue.api)
}
