
dependencies {

  api(project(":navauth-api"))

  // EternalCode Multification
  api(libs.multification.core)
  api(libs.multification.okaeri)

  implementation(libs.guice)
  api(libs.gson)

  // database
  implementation(libs.hikaricp)
  api(libs.ormlite.jdbc)

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
}