
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
  api("eu.okaeri:okaeri-configs-yaml-snakeyaml:6.0.0-beta.27")

  // crypto
  implementation("at.favre.lib:bcrypt:0.10.2")

  compileOnly("net.kyori:adventure-text-minimessage:4.25.0")

  // drivers
  runtimeOnly("com.h2database:h2:2.4.240")
  runtimeOnly("com.mysql:mysql-connector-j:9.5.0")
  runtimeOnly("org.postgresql:postgresql:42.7.8")
  runtimeOnly("org.xerial:sqlite-jdbc:3.51.1.0")

}