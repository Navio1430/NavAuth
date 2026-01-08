plugins {
  kotlin("jvm")
}

group = "pl.spcode.navauth.api"
version = "0.1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(libs.velocitypowered.velocity.api)
}

kotlin {
  jvmToolchain(21)
}