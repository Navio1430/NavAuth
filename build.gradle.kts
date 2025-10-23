
// todo add licenser and spotless plugins

plugins {
  id("java")
  kotlin("jvm") version "2.2.20"
  alias(libs.plugins.spotless)
}

repositories {
  mavenCentral()
}

allprojects {
  group = "pl.spcode.navauth"
  version = "0.1.0-SNAPSHOT"
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
    jvmToolchain(21)
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
