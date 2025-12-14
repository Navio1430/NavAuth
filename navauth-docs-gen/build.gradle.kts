plugins {
  kotlin("jvm")
}

group = "pl.spcode.navauth"
version = "0.1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
}

dependencies {
  implementation(project(":navauth-velocity"))
  implementation(kotlin("reflect"))
  implementation("com.github.Steppschuh:Java-Markdown-Generator:1.3.2")

  implementation(libs.velocitypowered.velocity.api)
}

tasks.register<JavaExec>("generate") {
  group = "docs"
  description = "Runs navauth docs generator"

  environment("TARGET_PATH", "../docs/docs/generated")

  dependsOn("compileKotlin")
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("pl.spcode.navauth.docsgen.MainKt")
}

kotlin {
  jvmToolchain(21)
}