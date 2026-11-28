
dependencies {

  api(project(":navauth-api"))

  // EternalCode Multification
  api(libs.multification.core)
  api(libs.multification.okaeri)

  implementation(libs.guice)
  api(libs.gson)

  // database
  implementation(libs.hikaricp)
  // brings kotlinx-coroutines: DatabaseConfig creates Dispatchers.IO even for plain transactions
  api(libs.exposed.core)
  api(libs.exposed.jdbc)
  implementation(libs.flyway.core)
  implementation(libs.flyway.mysql)
  implementation(libs.flyway.postgresql)
  implementation(libs.flyway.sqlite)

  // config
  api(libs.okaeri.configs.yaml.snakeyaml)

  // crypto
  implementation(libs.bcrypt)
  implementation(libs.bouncycastle.prov)
  implementation(libs.bouncycastle.util)

  compileOnly(libs.adventure.text.minimessage)

  // drivers
  runtimeOnly(libs.h2)
  runtimeOnly(libs.mysql)
  runtimeOnly(libs.postgresql)
  runtimeOnly(libs.sqlite.jdbc)

  // litecommands core (compileOnly because it is platform-dependent)
  compileOnly(libs.litecommands.core)

  // qr code generation
  api(libs.zxing.core)

  // tests
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.mockk)
  // TextComponent (messages config) needs MiniMessage; Velocity provides it at runtime
  testImplementation(libs.adventure.text.minimessage)
  testImplementation(platform(libs.testcontainers.bom))
  testImplementation(libs.testcontainers.junit)
  testImplementation(libs.testcontainers.mysql)
  testImplementation(libs.testcontainers.mariadb)
  testImplementation(libs.testcontainers.postgresql)
}