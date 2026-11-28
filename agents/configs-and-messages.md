# Configs and messages

NavAuth reads four YAML files that admins edit, all in the plugin data folder (`plugins/navauth/`):
`general.yml`, `messages.yml`, `commands.yml` and `migration.yml`. Each one is an okaeri-configs
class loaded once at startup by `YamlConfigModule` and bound in Guice as a single shared instance;
only `messages.yml` can be reloaded at runtime. Kick (disconnect) texts are `TextComponent`
MiniMessage strings, chat/title/action bar/sound notices are multification `Notice`s.

Paths: `common/…` = `navauth-common/src/main/kotlin/pl/spcode/navauth/common/…`, `velocity/…` =
`navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/…`.

## Where it lives

| Path | What |
| --- | --- |
| `common/module/YamlConfigModule.kt` | Guice module: loads one file into one config class, binds the instance (and, for `general.yml`, its subconfigs) |
| `common/config/GeneralConfig.kt` | `general.yml`. Sections are classes in the same folder (`UsernamesConfig`, `TwoFactorAuthConfig`, `PasswordsConfig` with nested `Argon2Config`, `EncryptionQueueConfig`, `MojangAPIConfig`) plus `common/infra/database/DatabaseConfig.kt` |
| `common/config/MessagesConfig.kt` | `messages.yml`: top-level `TextComponent` kick texts, `multification:` section (`NoticesConfig`) of `Notice`s |
| `common/config/CommandsConfig.kt`, `common/command/configurer/CommandConfig.kt` | `commands.yml`: per command `name`, `enabled`, `aliases` ([commands-and-permissions.md](commands-and-permissions.md)) |
| `common/config/MigrationConfig.kt` | `migration.yml`: `chunkSize`, `sourceDatabaseConfig`, `originPluginType` ([migrators.md](migrators.md)) |
| `common/config/serdes/` | `TextComponentSerdesPack` + `TextComponentSerializer`: a `TextComponent` is stored as its raw string |
| `common/component/TextComponent.kt` | MiniMessage text with `%KEY%` placeholders |
| `common/extension/StringExtensions.kt` | `applyPlaceholders`, the placeholder engine of `TextComponent` |
| `velocity/multification/VelocityMultification.kt` | sends `Notice`s to players and the console |
| `velocity/multification/VelocityViewerProvider.kt` | console/player lookup for multification |
| `common/component/MultificationProvider.kt`, `common/component/AudienceProvider.kt` | adapt a LiteCommands `Invocation` to a notice broadcast / Adventure audience for the handlers in `common/command/handler/` |
| `velocity/command/admin/ReloadConfigAdminCommand.kt` | `/navauth reload` |
| `navauth-common/src/test/kotlin/unit/config/ConfigUpgradeTests.kt` | loads files of an older release exactly as the plugin does (TESTING.md section C) |

## How loading works

`NavAuthVelocity.onProxyInitializeEvent` builds one `YamlConfigModule` per file and installs them
right after `PluginDirectoryModule`, before every other module. The module does all the work in
`configure()`:

```kotlin
ConfigManager.create(configClass.java) {
  it.configure { opt ->
    opt.configurer(YamlSnakeYamlConfigurer(), SerdesCommons(),
      MultificationSerdesPack(noticeRegistry), TextComponentSerdesPack())
    opt.bindFile(configFile)
  }
  it.saveDefaults()
  it.load(true)
}
```

- `saveDefaults()` writes the class defaults only when the file does not exist yet.
- `load(true)` reads the file and saves it back: a key missing from the admin's file gets its
  default written in, comments come from the `@Comment`/`@Header` annotations, values present are
  kept. Keys the class no longer declares stay in the file untouched and are never read (old
  installs still carry `sessionsConfig` and `databaseConfig.connectionTimeoutMs`).
- `SerdesCommons` handles `Duration` (written `30s`, `5m`) and the other common types; enums are
  stored by name; `MultificationSerdesPack` parses `Notice`s; `TextComponentSerdesPack` maps
  `TextComponent` to and from a plain string.
- A file that does not parse throws while the child injector is created; the startup `catch` in
  `NavAuthVelocity` shuts the proxy down.
- The loaded object is bound with `toInstance`, so every class injects the same instance.
  `general.yml` is loaded with `autoBindSubconfigs = true`: `bindSubconfigsRecursively` walks
  `declaredFields` and binds every nested `OkaeriConfig` by its declared field type
  (`DatabaseConfig`, `UsernamesConfig`, `PasswordsConfig`, `Argon2Config`, …), so a service injects
  only the section it needs. The other three files bind only their root class.

### configVersion

`GeneralConfig` and `MessagesConfig` declare `configVersion` (`@Variable("CONFIG_VERSION")`,
default `0`, `protected set`). No code reads it and no migration step exists yet: the first change
that needs a config migration adds the step (in `YamlConfigModule`, before `load(true)` saves the
file) and raises the default. `commands.yml` and `migration.yml` have no version field.

## Messages

### TextComponent: kick texts

`TextComponent(plainText)` keeps the raw MiniMessage string. `toComponent()` parses it as is;
`withPlaceholders().placeholder("USERNAME", value).toComponent()` first runs
`applyPlaceholders` (every `%KEY%` becomes `%s`, then `String.format`), then parses MiniMessage.
Line breaks are `<br>`. It is used wherever one `Component` is needed: disconnect reasons
(`LoginListeners`, `VelocityLoginAuthSession`, `VelocityRegisterAuthSession`,
`MigrateUserDataAdminCommand`), `multification.invalidUsageLine` (one line per usage scheme, sent by
`InvalidUsageHandler`) and the AjQueue integration's `queueAddFailMessage`. Most PreLogin denials
append `supportFooter` (`LoginListeners.withSupportFooter`); the login/register timeout and
too-many-attempts kicks do not.

### Notices: multification

`MessagesConfig.multification` (`NoticesConfig`) holds every chat notice. In YAML a `Notice` is a
string (chat) or a section (`chat`, `actionbar`, `title`, `subtitle`, `sound`, `bossbar`, …); the
user docs are `docs/docs/configuration/multification.md`. `VelocityMultification` extends
`Multification<CommandSource, MessagesConfig>`; its translation provider returns the bound
`MessagesConfig`, its serializer is MiniMessage. Sending:

```kotlin
multification.send(sender) { it.multification.wrongCredentialsError }
multification.create(sender) { it.multification.adminCmdPasswordSetSuccess }
  .placeholder("%USERNAME%", user.username.value).send()
multification.create().player(player.uniqueId).notice(messagesConfig.multification.loginSuccess).send()
```

`create(sender, …)` goes to the console when the sender is not a `Player`. Multification placeholder
keys include the percent signs (`"%USERNAME%"`); `TextComponent` keys do not (`"USERNAME"`).
`adminCopyPasswordText` is a plain `String`: `/forcecracked` fills `%PASSWORD%` with
`applyPlaceholders` and passes the result as `%PASSWORD_TEXT%`, relying on multification replacing
placeholders before MiniMessage parsing so the click tag works.

### Texts that are not configurable

Hardcoded English: the `/lookup profile` output, `UserResolveExceptionHandler` errors, `/migration
start` replies, the 2FA QR code, and a few fail-closed kicks in `LoginListeners` and
`ConnectionListeners` (`NavAuth: internal error`, `NavAuth: initial limbo server not found.`,
`NavAuth: Auth session expired…`). Moving one into `messages.yml` means a new key whose default is
today's text.

## Reload

`/navauth reload` calls `MessagesConfig.load()` on the shared instance (reads, does not save the
file). Code that reads `messagesConfig.<key>` at send time gets the new text. What does not change
without a restart:

- `general.yml`, `commands.yml`, `migration.yml`: never reloaded. Values are also copied at
  construction: `UsernameValidator` compiles `usernamePattern` once, `CommandsListeners` computes its
  whitelist once, LiteCommands applies names and aliases at registration, login/register sessions
  read their durations when they are created, and `/migration start` uses the `MigrationConfig`
  loaded at startup.
- A notice captured when a session starts keeps its old text for that session
  (`VelocityLoginAuthSession` picks the login instruction in `init`).

## Notable keys

| Key | Behaviour |
| --- | --- |
| `initialServers` (`[paper]`) | Least-connected of the names registered in Velocity; unknown names are skipped silently. None found: NavAuth does not set a server for an auto-logged-in player (Velocity decides) and a player who just logged in or registered stays on the limbo (warning logged). Plugins can override it ([api-and-events.md](api-and-events.md)). |
| `limboServers` (`[limbo]`) | None found: the unauthenticated player is disconnected. |
| `maxLoginAttempts` (3) | Per login session; then the `loginTooManyAttemptsError` kick. |
| `maxLoginDuration` (20s), `maxRegistrationDuration` (30s) | Kick with `loginTimeExceededError` / `registerTimeExceededError`. |
| `intervalBetweenLoginMultification`, `intervalBetweenRegisterMultification` (3s) | Repeat interval of the instruction notice; the first one is sent after 1 s. |
| `premiumAuthMultificationDelay` (1s) | Delay before `premiumAuthSuccess`. |
| `usernamesConfig.usernamePattern` | Invalid regex: warning and fallback `^[a-zA-Z0-9_]{3,16}$`. It also decides whether a command argument is a username or a UUID ([commands-and-permissions.md](commands-and-permissions.md)). |
| `passwordsConfig.*` | Rules for new passwords (`PasswordValidator`; whitespace is always rejected). `hashingAlgorithm` affects new hashes only ([accounts.md](accounts.md)). The `registerPasswordInvalidError` text is not generated from these rules and its default mentions an uppercase letter that the defaults do not require. |
| `twoFactorAuthConfig.issuer` | At most 15 characters, or the `otpauth://` QR code does not fit in chat. |
| `descriptiveInvalidSession*` | [login-flow.md](login-flow.md). |
| `databaseConfig`, `encryptionQueueConfig`, `mojangAPIConfig` | [persistence.md](persistence.md), [accounts.md](accounts.md), [mojang-profiles.md](mojang-profiles.md). |
| `messages.yml` `usernameUnavailableKickMessage`, `mojangAccountAlreadyLinkedError`, `adminCmdMojangAccountAlreadyLinkedError`, `twoFactorDisablePasswordRequiredError` | Keys with English defaults, written into an existing `messages.yml` on the next start ([login-flow.md](login-flow.md), [accounts.md](accounts.md)). |
| `messages.yml` `missingPermissionError` | Gets `%PERM%` (the missing node); the default text does not show it. |
| `commands.yml` `commands` | Map keyed by the command's code name; a missing entry leaves the command as coded ([commands-and-permissions.md](commands-and-permissions.md)). |
| `migration.yml` `sourceDatabaseConfig` | A second `DatabaseConfig`, not bound in Guice ([migrators.md](migrators.md)). |

## Rules

- **A new key gets a default that keeps the previous behaviour on existing installs**: `load(true)`
  writes that default into every admin's file on the next start.
- **Renaming, moving or removing a key, or changing its type, needs a config migration (bump
  `configVersion`) or must keep reading the old key.** Never silently reset an admin's value: after
  a plain rename, `load(true)` writes the new key with its default and leaves the admin's value
  under the old key, which nothing reads.
- **Changing a default changes only new installs**; existing files keep their value. Say so in the
  changelog *Config* section when it matters.
- **Kick messages are `TextComponent`, chat notices are multification `Notice`s** under
  `multification:`. A disconnect reason needs one `Component`; a notice can carry chat, title,
  sound and more.
- Every config property is a `var`: okaeri sets fields by reflection, and a `val` keeps its default
  (the admin's value and reloads are ignored).
- A config upgrade case gets a test in `ConfigUpgradeTests` and a row in
  [../TESTING.md](../TESTING.md) section C. User-facing keys are documented in `docs/docs/` in the
  same change.

## Traps

- A literal `%` in a `TextComponent` that is used with placeholders (`100%`) makes `String.format`
  throw; a `%KEY%` the code does not fill renders as `null`. `TextComponent.toComponent()` without
  placeholders is not affected.
- Placeholder values are inserted before MiniMessage parsing, in `TextComponent` and in notices: a
  value containing tags is parsed as MiniMessage. Pass only values that cannot hold tags (validated
  usernames, UUIDs) or escape them.
- `autoBindSubconfigs` binds by declared type: two fields of the same `OkaeriConfig` type anywhere
  in `GeneralConfig` make Guice fail at startup. `MigrationConfig.sourceDatabaseConfig` works only
  because `migration.yml` is not auto-bound. Fields inherited from a superclass are not bound
  (`declaredFields` only).
- `@Variable("CONFIG_VERSION")` lets an environment variable or system property of that name
  override `configVersion` in both files.
- `/navauth reload` sends `adminCmdConfigReloadError` when `load()` throws and then still sends
  `adminCmdConfigReloadSuccess` (the `catch` does not return).
- `VelocityMultification` is bound without a scope (a new instance per injection); this is harmless
  because all instances read the one `MessagesConfig`. Never copy a `Notice` or text into a field at
  construction if it must follow a reload.

## Related

[architecture.md](architecture.md) (Guice module order) ·
[commands-and-permissions.md](commands-and-permissions.md) · [login-flow.md](login-flow.md) ·
[accounts.md](accounts.md) · [persistence.md](persistence.md) ·
[mojang-profiles.md](mojang-profiles.md) · [migrators.md](migrators.md) ·
[api-and-events.md](api-and-events.md) · [docs-site.md](docs-site.md) · [README.md](README.md) ·
[../TESTING.md](../TESTING.md)
