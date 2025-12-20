
plugins {
  alias(libs.plugins.blossom)
  alias(libs.plugins.shadow)
}

val projectName = "navauth"

tasks.shadowJar {
  destinationDirectory.set(file("../target"))

  archiveBaseName.set("${projectName}-velocity")
  archiveClassifier = null

  var prefix = "pl.spcode.${projectName.lowercase()}.lib";
  relocate("kotlin", "${prefix}.kotlin")
  // todo: relocate gson lib

  exclude("com/google/inject/**")

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

blossom {
  replaceTokenIn("pl/spcode/$projectName/velocity/Bootstrap.java")
  replaceToken("@version@", rootProject.version.toString())
}

repositories {
  maven { url = uri("https://repo.panda-lang.org/releases") }
  maven { url = uri("https://mvn.tribufu.com/releases") }
}

dependencies {

  implementation(project(":navauth-common"))

  implementation("dev.rollczi:litecommands-velocity:3.10.6")

  compileOnly(libs.velocitypowered.velocity.api)
  annotationProcessor(libs.velocitypowered.velocity.api)

  // Tribufu-Rcon used for containers
  compileOnly("com.tribufu:Tribufu-VelocityRcon:1.2.0")
}
