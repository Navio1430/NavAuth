# NavAuth agent wiki

What is where in NavAuth, how it works and which rules must hold, written for AI agents (and
humans) who change the code. `AGENTS.md` holds the few rules every session needs; this folder
holds the rest. Read the page of the area you change before you change it.

Paths are shortened: `common/…` = `navauth-common/src/main/kotlin/pl/spcode/navauth/common/…`,
`velocity/…` = `navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/…`.

## Pages

| Page | Read it when you touch… |
| --- | --- |
| [architecture.md](architecture.md) | modules, layers, Guice modules, plugin startup and shutdown |
| [login-flow.md](login-flow.md) | PreLogin → GameProfileRequest → LoginEvent → PostLogin, username resolution, handshake and auth sessions, limbo and initial server, chat and command blocking |
| [accounts.md](accounts.md) | users, premium/cracked accounts, UUIDs, credentials, hashing, encryption queue, 2FA, account commands |
| [persistence.md](persistence.md) | Exposed tables and repositories, transactions, `DatabaseManager`, Flyway migrations, dialect traps, database rules |
| [configs-and-messages.md](configs-and-messages.md) | `general.yml`, `messages.yml`, `commands.yml`, `migration.yml`, messages and multification |
| [commands-and-permissions.md](commands-and-permissions.md) | LiteCommands, every command and permission, commands allowed before login |
| [mojang-profiles.md](mojang-profiles.md) | Mojang/MineTools profile lookups and their cache |
| [migrators.md](migrators.md) | imports from other plugins and database type migration |
| [api-and-events.md](api-and-events.md) | `navauth-api`, events, the AjQueue integration |
| [build-and-release.md](build-and-release.md) | Gradle, shading and relocation, the jar, local network, CI, versioning, changelog, releases |
| [security.md](security.md) | security invariants, privacy, how security fixes are released and described |
| [known-issues.md](known-issues.md) | bugs and inconsistencies that exist today (no security weaknesses) |
| [docs-site.md](docs-site.md) | the VitePress user docs and the generated pages |
| [../TESTING.md](../TESTING.md) | the edge case catalogue: every case, its test, how to run tests |

## Keeping the wiki true

- A change that alters behaviour, a contract or a trap described here updates the page **in the
  same change**. A wrong page is worse than none.
- Describe what is, in the present tense: no dates, no history, no "used to", no session notes.
  Git keeps the history.
- One fact lives in one place; elsewhere link to it. Rules every session needs go to `AGENTS.md`,
  everything else here.
- Save what a future agent needs and cannot see quickly in the code: where things live, contracts,
  non-obvious values with their reason, traps. Delete what is no longer true.
- English. No names of other projects, networks or test tooling.
- A new area gets a new page and a row in the table above.
