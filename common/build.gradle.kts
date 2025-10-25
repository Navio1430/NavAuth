
dependencies {

  // todo: move dependencies to libs.toml

  implementation("com.google.inject:guice:7.0.0")

  implementation("com.zaxxer:HikariCP:7.0.2")
  implementation("com.j256.ormlite:ormlite-jdbc:6.1")
  implementation("javax.persistence:javax.persistence-api:2.2")

  testRuntimeOnly("com.h2database:h2:2.4.240")
}