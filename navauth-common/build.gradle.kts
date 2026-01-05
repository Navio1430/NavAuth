
dependencies {

  // todo: move dependencies to libs.toml

  // EternalCode Multification
  api("com.eternalcode:multification-core:1.2.2")
  api("com.eternalcode:multification-okaeri:1.2.2")

  implementation("com.google.inject:guice:7.0.0")
  api("com.google.code.gson:gson:2.13.2")

  // database
  implementation("com.zaxxer:HikariCP:7.0.2")
  api("com.j256.ormlite:ormlite-jdbc:6.1")

  // config
  api("eu.okaeri:okaeri-configs-yaml-snakeyaml:6.0.0-beta.3")

  // crypto
  implementation("at.favre.lib:bcrypt:0.10.2")
  implementation("org.bouncycastle:bcprov-jdk18on:1.83")
  implementation("org.bouncycastle:bcutil-jdk18on:1.83")

  compileOnly("net.kyori:adventure-text-minimessage:4.25.0")

  // drivers
  runtimeOnly("com.h2database:h2:2.4.240")
  runtimeOnly("com.mysql:mysql-connector-j:9.5.0")
  runtimeOnly("org.postgresql:postgresql:42.7.8")
  runtimeOnly("org.xerial:sqlite-jdbc:3.51.1.0")

  // litecommands core (compileOnly because it is platform-dependent)
  // todo add litecommands version to versions.toml for full compatibility across modules
  compileOnly("dev.rollczi:litecommands-core:3.10.6")

  // qr code generation
  api("com.google.zxing:core:3.5.4")

  // tests
  testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
  testImplementation("io.kotest:kotest-assertions-core:5.8.0")
  testImplementation("io.mockk:mockk:1.13.8")
}