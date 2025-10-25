
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

  exclude("com/google/inject/**")
}

blossom {
  replaceTokenIn("pl/spcode/$projectName/velocity/Bootstrap.java")
  replaceToken("@version@", rootProject.version.toString())
}

dependencies {

  implementation(project(":common"))

  compileOnly(libs.velocitypowered.velocity.api)
  annotationProcessor(libs.velocitypowered.velocity.api)
}
