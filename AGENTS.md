# NavAuth

Premium auto-login and authentication plugin for **Velocity** (Kotlin, Java 25, Gradle). NavAuth
runs on live networks every day and stores real players' accounts, password hashes and 2FA secrets.
**Every change ships to servers that already hold data and configs from earlier releases.** That
rule comes before every other rule here.

## Start here: the agent wiki

[`agents/`](agents/README.md) describes what is where, how it works, the rules of each area and
its traps. **Read the page of the area you change before changing it, and update it in the same
change.** This file keeps only what every session needs.

| Module | What it is |
| --- | --- |
| `navauth-api` | Public Java API for other plugins; a SemVer contract |
| `navauth-common` | Core: domain, services, persistence (Exposed + Flyway), crypto, Mojang API, migrators, configs |
| `navauth-velocity` | Velocity adapter: bootstrap, listeners, commands; builds the shaded jar into `target/` |
| `integration/ajqueue`, `navauth-api-examples`, `navauth-docs-gen` | AjQueue integration, API example, generator of `docs/docs/generated/` |
| `docs/` | VitePress user docs |
| `agents/` | agent wiki · [`TESTING.md`](TESTING.md): edge case catalogue |

## Always

- **Upgrade safety:** an admin replaces the jar and restarts; without reading anything they keep
  every account, password, 2FA secret and setting. A change that cannot meet that is breaking: the
  owner decides, the version bumps, the changelog gets an *Upgrade notes* entry.
- **Database:** Flyway owns the schema; a schema change adds the same version to all five dialect
  directories; a released migration is never edited; never drop or rewrite data without the
  owner's decision; lookups tolerate duplicate rows. Details: [persistence.md](agents/persistence.md).
- **Identity:** a premium account belongs to its Mojang UUID, proven only by Mojang's session
  server during login; only online-mode connections complete a premium login; listeners fail
  closed. Details: [security.md](agents/security.md), [login-flow.md](agents/login-flow.md).
- **Never change how a user's UUID is derived**, and every password hash ever written keeps
  verifying.
- **Contracts admins and plugins rely on:** config keys (new keys default to the old behaviour,
  renames need a migration), command names, aliases and permission nodes, table and column names,
  `navauth-api`.
- **Never block Velocity's network threads** with database, HTTP or hashing work.
- **Privacy:** never log passwords, hashes, TOTP secrets or tokens; no IP addresses stored.
- **Security fixes are described without details** in the changelog, commits, docs and tests;
  push and release together ([security.md](agents/security.md)).

## Build and test

- `./gradlew build` (everything), `./gradlew formatAll` before every commit.
- `./gradlew :navauth-common:test` runs on H2, SQLite, MySQL, MariaDB and PostgreSQL (Docker classes
  skip without Docker); `./gradlew :navauth-velocity:test` covers the listeners.
- A change in behaviour adds its rows and tests to [`TESTING.md`](TESTING.md). A new migration gets
  an upgrade test from the previous schema with data. A change to dependencies, shading or startup
  order is proven by starting the shaded jar on a real Velocity.
- `./gradlew :navauth-velocity:shadowJar` → `target/`; `./gradlew :navauth-docs-gen:generate` after
  changing commands, events or migration origins.

## Git, versions, changelog

- Each release is developed on a branch named after the version (`0.4.0`). Agents never commit to
  `main`, never merge, never force-push, never push unless asked, and commit only when asked.
- One version for all modules in the root `build.gradle.kts`; patch = fix, minor = new capability
  or an automatic schema/config migration, major = breaking.
- Every change adds to `CHANGELOG.md` in the same commit ([build-and-release.md](agents/build-and-release.md)
  lists the sections).

## Code and docs

- Kotlin (`navauth-api` stays Java-friendly); Exposed DSL only; directory path = package name;
  every new source file starts with the AGPL header of the existing files.
- Code, configs, docs, PRDs and the wiki are English. No names of other projects, networks or test
  tooling anywhere in this repository.
- User-facing behaviour is documented in `docs/docs/` in the same change ([docs-site.md](agents/docs-site.md)).

## Planning

Features are planned in `roadmap/<feature>/PRD.md`, a local folder ignored by git. Nothing
committed (code, wiki, `TESTING.md`, docs, changelog) links to or names a PRD: the wiki describes
what exists, not what is planned. Every PRD starts with *In plain words* (a few sentences a player
or admin understands, with an example, no class names), has an *Upgrade safety* section and marks
the owner's decisions with ⛔. When a feature ships, its PRD is retired in the same change: how it
works, its rules and traps move to `agents/`, its cases to `TESTING.md`, its user-facing behaviour
to `docs/docs/`, the change to `CHANGELOG.md`; then its folder is deleted.

## Reference skills

- `.opencode/skills/kotlin-exposed-patterns/` — Exposed DSL patterns.
- `.opencode/skills/ormlite-to-exposed/` — decisions and traps of the OrmLite → Exposed move;
  delete it once that release is out.
