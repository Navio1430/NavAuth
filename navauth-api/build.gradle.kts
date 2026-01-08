plugins {
  kotlin("jvm")
}

group = "pl.spcode.navauth.api"
version = "0.1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(21)
}