plugins {
  kotlin("jvm")
}

group = "pl.spcode.navauth.example"
version = "0.1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":navauth-api"))

  compileOnly(libs.velocitypowered.velocity.api)
  annotationProcessor(libs.velocitypowered.velocity.api)
}

kotlin {
  jvmToolchain(21)
}