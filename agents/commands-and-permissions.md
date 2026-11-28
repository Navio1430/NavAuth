# Commands and permissions

NavAuth's commands are LiteCommands annotation classes in `velocity/command/`, registered at the
end of startup in `NavAuthVelocity.registerCommands`. Admins rename, re-alias or disable them in
`commands.yml` and grant or deny the permission nodes with their permission plugin (LuckPerms), so
names, aliases and nodes are a contract with existing installs. Until a player is logged in,
`CommandsListeners` blocks every command except `login`, `register` and `2fa`.

Paths: `common/…` = `navauth-common/src/main/kotlin/pl/spcode/navauth/common/…`, `velocity/…` =
`navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/…`.

## Where it lives

| Path | What |
| --- | --- |
| `velocity/NavAuthVelocity.kt` (`registerCommands`) | LiteCommands builder: commands, argument parser, handlers, editor |
| `velocity/command/CommandsRegistry.kt` | `commandsClasses`: every registered command class (also read by the docs generator) |
| `velocity/command/Permissions.kt` | permission node constants |
| `velocity/command/user/`, `admin/`, `root/` | player, admin and console-only command classes |
| `velocity/command/configurer/CommandConfigurer.kt` | applies `commands.yml` to every command |
| `velocity/infra/command/` | Velocity subclasses of the handlers (bound in `velocity/module/VelocityCommandsModule.kt`) |
| `common/command/handler/` | invalid usage, missing permission, `MissingPermissionException` and `UserResolveException` handlers |
| `common/command/user/` | `UsernameOrUuidRaw`, `UsernameOrUuidParser`, `UserArgumentResolver` |
| `common/command/exception/` | `MissingPermissionException`, `UserResolveException` |
| `common/annotation/Description.kt` | `@Description`, text for the generated docs |
| `velocity/listener/velocity/CommandsListeners.kt` | commands allowed before login, command tree filtering |
| `velocity/util/CommandSourceUtils.kt` | `isConsoleOrRcon` |
| `navauth-docs-gen/src/main/kotlin/pl/spcode/navauth/docsgen/generator/velocity/CommandsGenerator.kt` | writes `docs/docs/generated/velocity-commands.md` |
| `navauth-velocity/src/test/kotlin/unit/command/CommandsListenersTests.kt` | whitelist tests (TESTING.md L39) |

## LiteCommands setup

`registerCommands` runs after the database is migrated and the listeners are registered:

- `.commands(...)`: `CommandsRegistry.getWithInjection(injector)`, one Guice instance per class.
- `.argumentParser(UsernameOrUuidRaw::class.java, UsernameOrUuidParser)`.
- `.editorGlobal(CommandConfigurer)`: for each command it looks up
  `commandsConfig.commands[context.name()]` (the code name). No entry: the command stays as coded.
  Otherwise `name` (when not null or empty) renames it, `aliases` (empty strings dropped) replace
  its aliases, `enabled` switches it on or off.
- `.missingPermission(VelocityMissingPermissionHandler)` and
  `.exception(MissingPermissionException, VelocityMissingPermissionExceptionHandler)`: both send the
  `missingPermissionError` notice with `%PERM%` = the (first) missing node.
- `.invalidUsage(VelocityInvalidUsageHandler)`: the `invalidUsageError` notice, then one
  `invalidUsageLine` per usage scheme (`%SCHEME%`).
- `.exception(UserResolveException, UserResolveExceptionHandler)`: hardcoded red English text.
- `liteCommands.unregister()` runs on `ProxyShutdownEvent`.

`@Async` moves the method off the thread Velocity runs the command on, onto LiteCommands' async
scheduler. Every method that touches the database, the Mojang API or the encryption queue has it.
Exceptions thrown inside (`UserResolveException`, `MissingPermissionException`) still reach the
handlers above.

## Commands

`[x]` = optional argument. "Inverted" = no `@Permission`; the method throws
`MissingPermissionException` only when the node is explicitly **false** (see below).

| Class | Command (code name) | Permission | Sender |
| --- | --- | --- | --- |
| `user/LoginCommand` (`@RootCommand`) | `/login <password> [2fa]`, `/2fa <code>` | inverted `navauth.user.login` (both) | player, allowed before login |
| `user/RegisterCommand` | `/register <password> <repeat_password>` | inverted `navauth.user.register` | player, allowed before login |
| `user/ChangePasswordCommand` | `/changepassword <current_password> <new_password>` | class `navauth.user.changepassword` (must be granted) | player |
| `user/PremiumAccountCommand` | `/premium` | inverted `navauth.user.premium` | player |
| `user/TwoFactorSetupCommand` (`@RootCommand`) | `/setup2fa [current_password]` (alias `enable2fa`), `/verify2fa <2fa_code>` (alias `complete2fa`), `/generate2faqr` | inverted `navauth.user.twofactorsetup`, checked by `/setup2fa` only | player |
| `user/TwoFactorDisableCommand` (`@RootCommand`) | `/disable2fa <2fa_code>` | inverted `navauth.user.twofactordisable` | player |
| `admin/ForceChangePasswordAdminCommand` | `/forcesetpassword <username\|uuid> <password>` | class `navauth.admin.forcesetpassword` | any |
| `admin/ForceCrackedAdminCommand` | `/forcecracked <username\|uuid> [newPassword]` | class `navauth.admin.forcecracked` | any |
| `admin/ForcePremiumAdminCommand` | `/forcepremium <username\|uuid>` | class `navauth.admin.forcepremium` | any |
| `admin/MigrateUserDataAdminCommand` | `/migrateuser <username\|uuid> <newAccountUsername>` | class `navauth.admin.migrateuserdata` | player only |
| `admin/PlayerLookupAdminCommand` | `/lookup profile <username\|uuid>` | class `navauth.admin.playerlookup` + method `navauth.admin.playerlookup.profile` | player only |
| `admin/ReloadConfigAdminCommand` | `/navauth reload` | method `navauth.admin.reload` | any |
| `root/MigrationRootCommand` | `/migration start` | class `navauth.root` (a literal, not in `Permissions.kt`) | console or RCON only |

- Default `commands.yml` entries: `navauth` (alias `na`), `login` (alias `l`), `register` (alias
  `reg`). Any other code name can be added as a key.
- `/forcecracked` without a password generates an 8-character one; the console gets it as plain
  text, a player gets `adminCopyPasswordText` (click to copy).
- `/migration start` checks `CommandSourceUtils.isConsoleOrRcon` in the method and refuses players
  even when they hold `navauth.root`.
- `user/UnregisterCommand.kt` and `admin/ForceUnregisterAdminCommand.kt` are fully commented out
  and not registered; their nodes are commented out in `Permissions.kt`.
- What each account command does to stored data: [accounts.md](accounts.md).

## Permissions

- **LiteCommands checks the class-level and the method-level `@Permission`; a sender needs both**
  (`/lookup profile` needs `navauth.admin.playerlookup` and `navauth.admin.playerlookup.profile`).
- Commands every player needs have no `@Permission`, so they work without any grant. Their methods
  check `sender.getPermissionValue(node) == Tristate.FALSE` and throw `MissingPermissionException`:
  an admin blocks such a command for a group by setting its node to false; an undefined node allows
  it.
- The console passes every permission check. `CommandSourceUtils.isConsoleOrRcon` also accepts the
  Velocity RCON plugin's `IRconCommandSource` and catches `NoClassDefFoundError` when that plugin is
  absent.
- `Permissions.BASE`, `USER_BASE`, `ADMIN_BASE` are prefixes; no command checks them.

## Commands before login

`CommandsListeners` (`velocity/listener/velocity/`) decides what a player who is not logged in may
run:

- `AUTH_COMMANDS = [login, register, 2fa]` (code names). `allowedCommandNames(commandsConfig)` maps
  each one through `commands.yml` exactly as `CommandConfigurer` does: no entry → the code name;
  `enabled: false` → nothing; otherwise the configured name (or the code name when empty) plus the
  non-empty aliases; all lower case. The set is computed once, when the listener is built.
- `CommandExecuteEvent`, players only, has two deny-only handlers; neither ever sets `allowed()`.
  `onCommandFirst` (`PostOrder.FIRST`) denies a non-whitelisted command of a player who is not
  logged in before other plugins see it; `onCommand` (`priority = Short.MIN_VALUE`) runs after every
  plugin and checks again. The decision is made on the effective command (the result's replacement
  command when an earlier listener set one, else the typed one); its first word is compared
  case-insensitively. A result that is already denied stays denied. A non-whitelisted command is
  denied when the player has no auth session or an unauthenticated one, also when the result is
  `forwardToServer`: this covers every command that passes through the proxy, including ones
  forwarded to backend servers.
- `PlayerAvailableCommandsEvent` (`Short.MIN_VALUE`): no session → the command tree is cleared;
  unauthenticated → only whitelisted nodes stay.
- Player commands outside the whitelist can therefore assume an authenticated player with a stored
  account (`userService.findUserByExactUsername(sender.username)!!`).

## Username or UUID arguments

- `UsernameOrUuidRaw` wraps the raw argument. `UsernameOrUuidParser.parse` always succeeds; `match`
  accepts a string that passes `UsernameValidator` (the configured `usernamePattern`) or
  `UuidUtils.UUID_REGEX` (36 characters with dashes, or 32 hex digits). It suggests nothing.
- The command body calls `UserArgumentResolver.resolve(raw)` itself, inside `@Async`: a valid
  username → `findUserByUsernameIgnoreCase`; anything else is parsed by `UuidUtils.fromString` and
  looked up as the NavAuth user UUID (`findUserByUuid`, not the Mojang UUID). Failures throw
  `UserResolveException.UsernameNotFound`, `UuidNotFound` or `InvalidUuid`.

## Generated docs

`./gradlew :navauth-docs-gen:generate` (also run by `.github/workflows/deploy-docs.yml`) writes
`docs/docs/generated/velocity-commands.md`, which `docs/docs/general/commands.md` includes.
`CommandsGenerator` reflects over `CommandsRegistry.commandsClasses`: the class `@Command` name and
`@Permission`, then each `@Execute` name and aliases, its `@Permission` and its `@Description`
(lines joined with a space; none → "No description"). The permission column is class and method
nodes joined with ` + `, or "available by default" when there are none (the inverted commands). It
knows nothing of `commands.yml`: it documents code names and code aliases only. See
[docs-site.md](docs-site.md).

## Rules

- **Command names, aliases and permission nodes are configured by admins** (`commands.yml`,
  LuckPerms). Renaming one, or changing which node guards a command, is a breaking change; list
  added and removed nodes in the changelog *Permissions* section.
- **Regenerate the generated docs after changing commands** (`./gradlew
  :navauth-docs-gen:generate`); every `@Execute` carries a `@Description`.
- A new command: its class goes into `CommandsRegistry.commandsClasses` (otherwise it is neither
  registered nor documented), its nodes into `Permissions.kt`, `@Async` when it touches the
  database, HTTP or hashing, its texts into `messages.yml` as `Notice`s
  ([configs-and-messages.md](configs-and-messages.md)).
- Only `CommandsListeners.AUTH_COMMANDS` may run before login. Adding to it is a security decision
  ([security.md](security.md)); `allowedCommandNames` must stay in step with `CommandConfigurer`.

## Traps

- `@Context sender: Player` makes a command player-only: the console cannot run `/lookup profile`
  or `/migrateuser`.
- `/migration start` is not `@Async`: the whole migration runs on the thread that executes the
  command, blocking the console until it ends.
- Removing a key from `commands.yml` brings back the code name and code aliases (`CommandConfigurer`
  returns the command untouched when there is no entry).
- `/verify2fa` and `/generate2faqr` do not check the inverted `navauth.user.twofactorsetup` node.
- The generated table follows `KClass.functions` reflection order, so regenerating can reorder
  rows.
- `navauth-docs-gen/build.gradle.kts` declares `dev.rollczi:litecommands-velocity:3.10.6` by hand
  instead of the catalog entry (`litecommands = 3.11.0`).

## Related

[configs-and-messages.md](configs-and-messages.md) · [login-flow.md](login-flow.md) ·
[accounts.md](accounts.md) · [security.md](security.md) · [migrators.md](migrators.md) ·
[architecture.md](architecture.md) · [docs-site.md](docs-site.md) ·
[build-and-release.md](build-and-release.md) · [README.md](README.md) ·
[../TESTING.md](../TESTING.md)
