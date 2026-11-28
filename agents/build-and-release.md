# Build and release

One Gradle build (Kotlin DSL, JDK 25 toolchain) produces the shaded plugin jar, the published API
and the generated docs. Releases go from a version branch through a tag, a draft GitHub release and
Modrinth.

## Where it lives

| Path | What |
| --- | --- |
| `build.gradle.kts` | `allprojects { version = … }` (the single version), repositories, JDK 25 toolchain, spotless, `formatAll` |
| `gradle/libs.versions.toml` | every dependency and plugin version, pinned exactly |
| `buildSrc/src/main/kotlin/navauth-shadow.gradle.kts` | shading convention: relocations, excludes, service-file merging |
| `navauth-velocity/build.gradle.kts` | `shadowJar` → `target/navauth-velocity-<version>.jar`, copy into `navauth-velocity/run/velocity/plugins` when that folder exists; Blossom template `src/main/kotlin-templates/.../BuildParameters.kt.peb` (version); kapt for `velocity-plugin.json` |
| `navauth-api/build.gradle.kts` | `maven-publish` to GitHub Packages |
| `navauth-velocity/docker-compose.yaml`, `navauth-velocity/run/` | local test network (Velocity, Paper, limbo, PostgreSQL); `run/` is git-ignored |
| `minecraft-mcp.json`, `.run/mcp/velocity/` | playtest proxy for the minecraft MCP: target `proxy` (Velocity, `127.0.0.1:25590`) with a PicoLimbo limbo plugin on `127.0.0.1:25591` (modern forwarding), H2 database; `.run/` is git-ignored. Copy the shaded jar into its `plugins/` only while it is stopped. Offline bots (`stress_run`) cover cracked logins, a real client premium ones. Leave at least 50 ms between commands a bot sends (Velocity's `command-rate-limit`) |
| `.github/workflows/` | CI and release workflows (below) |
| `CHANGELOG.md` | changelog, newest version first |

## Commands

| Command | Does |
| --- | --- |
| `./gradlew build` | compile and test everything |
| `./gradlew formatAll` | spotless: ktfmt (Google style) and google-java-format; run before every commit |
| `./gradlew :navauth-common:test`, `:navauth-velocity:test` | tests (see [../TESTING.md](../TESTING.md)) |
| `./gradlew :navauth-velocity:shadowJar` | the plugin jar in `target/` |
| `./gradlew :navauth-docs-gen:generate` | regenerate `docs/docs/generated/` |

## Shading

Velocity gives every plugin its own class loader but plugins can see each other's classes, so a
bundled library that is not relocated can clash with another plugin's copy.

- Every bundled library is relocated under `pl.spcode.navauth.lib`. Velocity provides Gson, Guice
  and SLF4J: they are excluded, not bundled.
- **`org.sqlite` is not relocated:** its native library looks up `org/sqlite/core/NativeDB` by
  name; relocated, opening a database fails with `NoClassDefFoundError`. It runs fine next to
  another plugin with its own sqlite-jdbc.
- **Never change the relocated name of `org.h2`:** H2 database files store class names of that
  package; a different name cannot open existing `.mv.db` files.
- **kotlinx-coroutines** comes from `exposed-core` and is required (`DatabaseConfig` creates
  `Dispatchers.IO`); the `kotlin` relocation prefix also covers `kotlinx`, and `_COROUTINE` is
  relocated separately. Excluding it passes the tests (kotest brings coroutines) but the jar fails
  on the proxy.
- **Jackson 3** (`tools.jackson`, from Flyway) and `com.fasterxml` annotations are kept and
  relocated.
- **`META-INF/services` files must be merged** (`DuplicatesStrategy.INCLUDE` for them, relocation
  rewrites their names and contents). With only one jar's copy Flyway finds no database plugin and
  every driver fails at startup. Check with `unzip -p target/<jar> META-INF/services/<name>` after
  changing dependencies.
- Nothing is downloaded at runtime; everything ships in the jar.
- Unit tests run on unshaded classes. After any change to dependencies, shading or startup order,
  start the shaded jar on a real Velocity with every driver (fresh, shared schema, upgrade from the
  previous release).

## Dependencies

- Versions are pinned exactly in `gradle/libs.versions.toml`; take the newest stable release that is
  at least two weeks old. An older one kept for a reason has the reason in a comment.
- A new dependency is the owner's decision.
- `buildSrc` pins Shadow separately from the catalog; keep them the same.

## Versioning and changelog

- One version for all modules in the root `build.gradle.kts` (`X.Y.Z-SNAPSHOT` during development
  and in release jar names). Patch = fix, minor = new capability or a schema/config change with an
  automatic migration, major = breaking change. `navauth-api` follows SemVer on its own contract.
- Every change adds to `CHANGELOG.md` in the same commit, sections *Changes*, *Fixes*, *Config*
  (diff of keys), *Permissions*, *API*, *Database* (migrations, rollback possible or not),
  *Upgrade notes* (what an admin must know or do), *New contributors*. Security fixes are described
  without details: [security.md](security.md).
- `CHANGELOG.md` uses CRLF line endings.

## Branches and release flow

- Each release is developed on a branch named after the version (`0.4.0`) and merged to `main` by
  the owner. Agents never commit to `main`, never merge, never force-push, never push without being
  asked, and commit only when asked.
- Release: push tag `v<version>` → `release.yml` builds the jars into a **draft** GitHub release →
  the owner publishes it → `modrinth.yml` uploads the `navauth-velocity` jar with the version's
  `CHANGELOG.md` section (game versions 1.13.1 and newer, a pre-release becomes a Modrinth beta;
  needs the `MODRINTH_PROJECT_ID` variable and the `MODRINTH_TOKEN` secret; a version Modrinth
  already has is skipped).
- Pushing a security fix makes it public: push and release together.

## CI workflows

| Workflow | Trigger | Does |
| --- | --- | --- |
| `build.yml` | push to `main`, pull requests | `./gradlew build` (all five backends; Docker is available), dependency submission |
| `publish.yml` | push to `main`, release created, manual | build and publish `navauth-api` to GitHub Packages |
| `release.yml` | tag `v*` | shadow jars into a draft release |
| `modrinth.yml` | release published, manual | upload to Modrinth |
| `deploy-docs.yml` | push to `main` touching `docs/`, release created, manual | generated docs + javadoc + VitePress → GitHub Pages |

Every action is pinned to a commit SHA with its version in a comment (newest stable release at
least two weeks old). `gradle/actions` uses `cache-provider: basic` (MIT); the default provider
needs accepting Gradle's terms of use (owner's decision).

## Related

[architecture.md](architecture.md) · [persistence.md](persistence.md) · [security.md](security.md) ·
[docs-site.md](docs-site.md)
