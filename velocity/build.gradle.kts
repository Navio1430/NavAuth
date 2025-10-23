
// todo add shadow plugin
// todo add blossom plugin
// todo setup velocity bootstrap java class
// todo setup velocity kotlin main class

dependencies {

  project(":common")

  compileOnly(libs.velocitypowered.velocity.api)
  annotationProcessor(libs.velocitypowered.velocity.api)
}
