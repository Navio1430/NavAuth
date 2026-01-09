plugins {
  kotlin("jvm")
}

group = "pl.spcode.navauth.example"

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