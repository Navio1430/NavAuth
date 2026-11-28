# API and events

`navauth-api` is the Java API other Velocity plugins compile against: `NavAuthAPI` (auth state and
the event bus), `AuthUser`, `AuthPlayer` and six events. NavAuth implements it with its own small
reflection event bus (not Velocity's) and posts the events from services and listeners in
`navauth-common` and `navauth-velocity`. It is a contract with third-party plugins and with the
AjQueue integration, which ships as a separate plugin in `integration/ajqueue`.

Paths: `common/…` = `navauth-common/src/main/kotlin/pl/spcode/navauth/common/…`, `velocity/…` =
`navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/…`.

## Where it lives

| Path | What |
| --- | --- |
| `navauth-api/src/main/java/pl/spcode/navauth/api/` | the API: `NavAuthAPI`, `domain/`, `event/` (Java; `domain/auth/AuthSessionType.kt` is the one Kotlin file) |
| `navauth-api/build.gradle.kts` | `maven-publish` publication `gpr` to GitHub Packages; Javadoc into `docs/dist/javadoc` |
| `navauth-common/src/main/java/pl/spcode/navauth/common/infra/NavAuthEventBusInternal.java` | event bus implementation (`@Singleton`), with `MethodInvocation.java` and `Invocation.java` |
| `common/module/EventsModule.kt` | binds `NavAuthEventBus` to `NavAuthEventBusInternal` |
| `common/domain/event/UserAuthenticatedEventInternal.kt` | the concrete `UserAuthenticatedEvent` NavAuth posts |
| `velocity/NavAuthApiImpl.kt` | the `NavAuthAPI` implementation |
| `velocity/application/server/VelocityServerSelectionService.kt` | posts the two server-selection events |
| `navauth-api-examples/` | example consumer plugin (Java) |
| `integration/ajqueue/` | AjQueue integration plugin |
| `.github/workflows/publish.yml` | publishes `navauth-api` |

## Public API

- `NavAuthAPI` (abstract class): static `getInstance()`, `getEventBus()`,
  `isAuthenticated(UUID playerUuid)`, static `setAPIInstance(...)`. `NavAuthVelocity` sets the
  instance in `onProxyInitializeEvent` after the database is connected and migrated, before
  listeners and commands are registered; until then, and after a failed startup, `getInstance()` is
  `null`. `NavAuthApiImpl.isAuthenticated` is `false` for an offline player or one without an auth
  session, otherwise the session's `isAuthenticated`.
- `AuthUser`: `getUUID()` (the NavAuth user UUID, the one backend servers see), `@Nullable
  getMojangUUID()`, `getUsername()`, default `Boolean isPremium()` (boxed). NavAuth passes a
  snapshot built by `User.toAuthUser()` (`common/domain/user/User.kt`).
- `AuthPlayer`: `getIdentifier()`, the connected player's UUID. Implemented by `PlayerAdapter`
  (`common/domain/player/PlayerAdapter.kt`) and `VelocityPlayerAdapter`.
- `AuthSessionType`: `LOGIN`, `REGISTER`, `PREMIUM`.
- `NavAuthEventBus`: `register(NavAuthEventListener)`, `unregister(NavAuthEventListener)`. Listener
  methods carry `pl.spcode.navauth.api.event.Subscribe` (`int priority()`, default
  `EventPriority.NORMAL` = 20; `LOWEST` 0, `LOW` 10, `HIGH` 30, `HIGHEST` 40, all `public`; higher
  runs earlier).

## Events and where they are posted

| Event | Payload | Posted by | When |
| --- | --- | --- | --- |
| `user/UserAuthenticatedEvent` (abstract; NavAuth posts `UserAuthenticatedEventInternal`) | `getPlayer(): AuthPlayer`, `getSessionType()` | `AuthSession.authenticate()` (`common/domain/auth/session/AuthSession.kt`) | The session becomes authenticated while the player is online, before `onAuthenticated()` (success notice, transfer to the initial server). `PREMIUM`: `LoginListeners.createAuthSession` inside Velocity's `PostLoginEvent`. `LOGIN`: `LoginAuthSession.enqueueAuthTask` when the check succeeds (on the encryption queue thread after a password check, on the command's thread for a 2FA-only login). `REGISTER`: `RegisterCommand` after the hashed account is stored (encryption queue thread). |
| `user/UserPremiumMigrationEvent` | `getUser()` (after the change) | `UserService.migrateToPremium` (`common/application/user/`) | `/premium`, `/forcepremium` |
| `user/UserNonPremiumMigrationEvent` | `getUser()` | `UserService.migrateToNonPremium` | `/forcecracked` |
| `user/UserUsernameMigrationEvent` | `getUser()` (new name), `getOldUsername()` | `UserService.migrateUsername` | Username resolution in PreLogin (`UsernameResolutionService`): a stored premium player joins after renaming on Mojang, or the letter case of the name changed. `/migrateuser` (`UserService.migrateData`) does not post it. |
| `velocity/AuthenticatedInitialServerEvent` | `getPlayer(): Player`, `getInitialServer()`, `setInitialServer(@Nullable RegisteredServer)` | `VelocityServerSelectionService.getInitialServer` | From `ConnectionListeners.setInitialServerAuthenticated` (Velocity's `PlayerChooseInitialServerEvent` of an auto-logged-in player) and `VelocityServerConnectService.sendPlayerToInitialServer` (after `/login` or `/register`). `null`: in the first case NavAuth leaves the choice to Velocity, in the second the player stays on the limbo and a warning is logged. |
| `velocity/UnauthenticatedInitialLimboEvent` | `getPlayer(): Player`, `getInitialLimbo()`, `setInitialLimbo(@Nullable RegisteredServer)` | `VelocityServerSelectionService.getLimboServer` | From `ConnectionListeners.setInitialLimboUnauthenticated` (`PlayerChooseInitialServerEvent`). `null` disconnects the player. |

The server NavAuth pre-selects is the least-connected of `initialServers` / `limboServers`
([configs-and-messages.md](configs-and-messages.md)). The `velocity/` events use Velocity types
(`Player`, `RegisteredServer`, Guava `Preconditions`); `velocity-api` is `compileOnly`.

## The internal event bus

- `register`: scans `listener.getClass().getDeclaredMethods()` for NavAuth's `@Subscribe` with
  exactly one parameter; the parameter type is the event key. Each method is made accessible and
  wrapped in a `MethodInvocation` (a `MethodHandle`, a weak reference to the listener, the
  priority; `Invocation.getPriority()`; it is not `Comparable`), stored in one
  `CopyOnWriteArrayList` per event class, sorted by priority (higher first, equal priorities in
  registration order). `registrations` keeps listener → event types and holds the listener
  strongly until `unregister`.
- `unregister`: removes the invocations whose target `equals` the listener.
- `post(event)` runs synchronously on the caller's thread and walks a snapshot of each list, so
  listeners may register or unregister meanwhile. It walks the event's class and its
  superclasses (not interfaces): a subscriber to `UserAuthenticatedEvent` receives
  `UserAuthenticatedEventInternal`; a subscriber to `NavAuthEvent` receives nothing. An exception
  in a listener is logged (`MethodInvocation` catches `Throwable`) and the next listener runs.
- The API interface has no `post`; NavAuth code posts with `(eventBus as
  NavAuthEventBusInternal).post(...)`.

## Example plugin

`navauth-api-examples` is a Java Velocity plugin (`navauthexamples`, `@Dependency(id = "navauth")`).
`Main` registers `listener/NavAuthListeners.java`, injected by Guice, on `ProxyInitializeEvent`;
the listener subscribes to `UserAuthenticatedEvent` and both server-selection events. It compiles
against `project(":navauth-api")` and is not shaded or released. Its comment saying that
`setInitialServer(null)` disconnects the player is wrong (see the table).

## AjQueue integration

`integration/ajqueue` is a separate Velocity plugin (`navauth-ajqueue`) with its own version
(`1.0.0-SNAPSHOT` in its `build.gradle.kts`), depending on the `navauth` and `ajqueue` plugins.

- `AjQueueIntegrationPlugin.onProxyInitializeEvent` compares NavAuth's plugin version with
  `requiredMinNavAuthVersion` (`0.2.0`, passed through Blossom into `BuildParameters`) using
  `VersionUtils`; an older NavAuth logs an error and the integration stays off. It loads its own
  `config.yml` with NavAuth's `YamlConfigModule` and registers `NavAuthListeners` on
  `NavAuthAPI.getInstance().getEventBus()`. Any other startup exception shuts the proxy down.
- `config.yml` (`config/GeneralConfig.kt`): `queueName` (`lobby`; blank turns queueing off),
  `queueAddFailMessage` (`TextComponent`), `kickOnQueueAddFail` (false),
  `cancelQueueJoinEventIfNotAuth` (false).
- `listener/NavAuthListeners.kt`: on every `UserAuthenticatedEvent` (all session types) adds the
  player to `queueName`; on failure sends `queueAddFailMessage` and kicks with it when configured.
- `listener/AjQueueListeners.kt`: when `cancelQueueJoinEventIfNotAuth` is on, cancels AjQueue's
  `PreQueueEvent` for players `NavAuthAPI.isAuthenticated` reports as not logged in.
- Built by the same `navauth-shadow` convention into `target/navauth-ajqueue-integration-<version>.jar`,
  excluding `kotlin/**`, `org/jetbrains/**`, `org/intellij/**` and `META-INF/**`: at runtime it uses
  the Kotlin and libraries inside the NavAuth jar under the same relocated names. It depends on
  `compileOnly(project(":navauth-common"))` and uses internals (`YamlConfigModule`, `TextComponent`,
  `VersionUtils`), not only the API.

## How the events are documented

- NavAuth's API events have no generator: they are described by the `navauth-api` Javadoc
  (`./gradlew navauth-api:javadoc` → `docs/dist/javadoc`, built and deployed by
  `.github/workflows/deploy-docs.yml`) and by `docs/docs/developers/api.md`.
- `EventsGenerator` in `navauth-docs-gen` writes `docs/docs/generated/events-table.md`: the
  **Velocity** events NavAuth itself listens to, with their order, read from the Velocity
  `@Subscribe` methods of `VelocityListenersRegistry.listenersClasses`. It is included by
  `docs/docs/developers/velocity/events-order.md`. Regenerate it after adding a Velocity listener or
  changing a listener's order or priority ([docs-site.md](docs-site.md)).

## Publishing

`navauth-api` has no version of its own: it is published with the root `allprojects { version }`.
`publish.yml` runs `./gradlew publish` on every push to `main`, on a created release and on manual
dispatch, to `https://maven.pkg.github.com/Navio1430/NavAuth` as `pl.spcode.navauth:navauth-api`.
The API classes also ship inside the NavAuth plugin jar; consumers depend on it `compileOnly` and
declare `@Dependency(id = "navauth")`.

## Rules

- **`navauth-api` follows SemVer**: nothing is removed or changed incompatibly in a minor release;
  deprecate first (`@Deprecated` plus Javadoc naming the replacement) and remove in a major. Since
  the API shares the plugin's version, an incompatible API change makes a major NavAuth release.
  API changes go into the changelog *API* section.
- **The API stays Java-friendly**: new types are Java; signatures use no Kotlin-only constructs
  (default arguments, extension functions, `kotlin.*` types); nullability is annotated.
- **Events are posted outside database transactions**: run `txService.inTransaction { … }` first
  and post after it returns, as `UserService` does. A listener must never run while a transaction is
  open, and must see committed data.
- Event payloads are binary contracts: add getters, never remove or retype one (even
  `Boolean isPremium()` → `boolean` breaks compiled plugins).
- A new event is a Java class with Javadoc in `navauth-api`, posted from one place, and gets a row
  in the table above.

## Traps

- Only methods declared on the listener's own class are registered; inherited `@Subscribe` methods
  are ignored, and Velocity's `@Subscribe` on a NavAuth listener registers nothing. Registering the
  same listener twice delivers every event twice.
- Listeners run inside NavAuth's flow on NavAuth's threads: the premium `UserAuthenticatedEvent`
  inside `PostLoginEvent`, the server-selection events inside `PlayerChooseInitialServerEvent`. A
  slow listener delays the login; its exceptions are only logged.
- `AuthSessionType.kt` is Kotlin inside `src/main/java`; with the `kotlin("jvm")` plugin applied,
  the published artifact depends on the Kotlin standard library.
- `NavAuthAPI.setAPIInstance` is public, so any plugin can replace the instance.
- Changing `YamlConfigModule`, `TextComponent`, `VersionUtils` or the relocation prefix in
  `buildSrc/src/main/kotlin/navauth-shadow.gradle.kts` breaks the released AjQueue integration jar.

## Related

[architecture.md](architecture.md) · [login-flow.md](login-flow.md) · [accounts.md](accounts.md) ·
[persistence.md](persistence.md) · [configs-and-messages.md](configs-and-messages.md) ·
[commands-and-permissions.md](commands-and-permissions.md) ·
[build-and-release.md](build-and-release.md) · [docs-site.md](docs-site.md) ·
[security.md](security.md) · [README.md](README.md) · [../TESTING.md](../TESTING.md)
