plugins {
  alias(libs.plugins.blossom)
  id("buildsrc.navauth-shadow")
  kotlin("kapt")
}

val projectName = "navauth"

tasks.shadowJar {
  destinationDirectory.set(file("../target"))

  archiveBaseName.set("${projectName}-velocity")
  archiveClassifier = null

  doLast {
    val pluginsDir = file("./run/velocity/plugins")
    if (pluginsDir.exists() && pluginsDir.isDirectory) {
      pluginsDir.listFiles { _, name -> name.startsWith("${projectName}-velocity") }?.forEach {
        it.delete()
      }

      val builtJar = archiveFile.get().asFile
      copy {
        from(builtJar)
        into(pluginsDir)
      }
    } else {
      logger.warn("run directory does not exist, skipping plugin copy to run directory")
    }
  }
}

sourceSets {
  main {
    blossom {
      kotlinSources {
        property("version", rootProject.version.toString())
      }
    }
  }
}

repositories {
  maven { url = uri("https://repo.panda-lang.org/releases") }
  maven { url = uri("https://mvn.tribufu.com/releases") }
}

dependencies {
  implementation(project(":navauth-common"))

  implementation(libs.litecommands.velocity)

  compileOnly(libs.velocitypowered.velocity.api)
  kapt(libs.velocitypowered.velocity.api)

  // bstats
  implementation("org.bstats:bstats-velocity:3.1.0")

  // Tribufu-Rcon used in itzg containers
  compileOnly("com.tribufu:Tribufu-VelocityRcon:1.2.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-java-parameters")
  }
}
