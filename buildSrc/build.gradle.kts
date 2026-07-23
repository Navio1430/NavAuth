plugins {
  `kotlin-dsl`
  `java-library`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation("com.gradleup.shadow:shadow-gradle-plugin:9.2.0")
}
