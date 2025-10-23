
// todo add licenser and spotless plugins

plugins {
  id("java")
  kotlin("jvm") version "2.2.20"
}

repositories {
  mavenCentral()
}

allprojects {
  group = "pl.spcode.navauth"
  version = "0.1.0-SNAPSHOT"
}

subprojects {
  repositories {
    mavenCentral()
    maven {
      name = "papermc"
      url = uri("https://repo.papermc.io/repository/maven-public/")
    }
  }

  apply {
    plugin("java")
    plugin("kotlin")
  }

  dependencies {
    testImplementation(kotlin("test"))
  }

  kotlin {
    jvmToolchain(21)
  }

  java {
    version = JavaVersion.VERSION_21
  }

  tasks.test {
    useJUnitPlatform()
  }

}
