
plugins {
  id("java")
  kotlin("jvm") version "2.4.10"
  alias(libs.plugins.spotless)
}

repositories {
  mavenCentral()
}

allprojects {
  group = "pl.spcode.navauth"
  version = "0.3.0-SNAPSHOT"
}

tasks.register("formatAll") {
  group = "formatting"
  description = "Runs format task on all subprojects"

  dependsOn(subprojects.mapNotNull { it.tasks.findByName("formatAll") })
}

subprojects {
  repositories {
    mavenCentral()
    maven {
      name = "papermc"
      url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.eternalcode.pl/releases")
  }

  apply {
    plugin("java")
    plugin("kotlin")
    plugin("com.diffplug.spotless")
  }

  dependencies {
    testImplementation(kotlin("test"))
  }

  kotlin {
    jvmToolchain(25)
  }

  java {
    toolchain {
      languageVersion = JavaLanguageVersion.of(25)
      targetCompatibility = JavaVersion.VERSION_25
      sourceCompatibility = JavaVersion.VERSION_25
    }
  }

  tasks.test {
    useJUnitPlatform()
  }

  tasks.register("formatAll") {
    group = "formatting"

    description = "Runs spotless tasks"

    dependsOn("spotlessJavaApply")
    dependsOn("spotlessKotlinApply")
  }

  spotless {
    java {
      googleJavaFormat().reorderImports(false)
    }
    kotlin {
      ktfmt().googleStyle()
    }
  }

}
