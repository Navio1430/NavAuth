
dependencies {

  // todo: move dependencies to libs.toml

  implementation("com.google.inject:guice:7.0.0")
  implementation("com.google.code.gson:gson:2.13.2")

  implementation("com.zaxxer:HikariCP:7.0.2")
  implementation("com.j256.ormlite:ormlite-jdbc:6.1")

  // crypto
  implementation("at.favre.lib:bcrypt:0.10.2")

  runtimeOnly("com.h2database:h2:2.4.240")
}