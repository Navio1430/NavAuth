plugins {
  kotlin("jvm")
  id("maven-publish")
}

group = "pl.spcode.navauth"

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(libs.velocitypowered.velocity.api)
}

publishing {
  publications {
    create<MavenPublication>("gpr") {
      from(components["java"])
    }
  }
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/Navio1430/NavAuth")
      credentials {
        username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
        password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
      }
    }
  }
}
