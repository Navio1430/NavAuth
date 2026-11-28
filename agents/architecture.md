# Architecture

NavAuth is a Velocity plugin split into a platform-independent core (`navauth-common`) and a
Velocity adapter (`navauth-velocity`) that wires everything with Guice and ships the shaded jar. A
small public API (`navauth-api`) lets other plugins read auth state and subscribe to events.

## Modules

| Module | What it is |
| --- | --- |
| `navauth-api` | Public Java API: `NavAuthAPI`, `AuthUser`, `AuthPlayer`, events. Published to GitHub Packages. See [api-and-events.md](api-and-events.md). |
| `navauth-common` | Core: domain, services, persistence (Exposed + Flyway), crypto, Mojang profile API, migrators, configs. No Velocity classes. |
| `navauth-velocity` | Velocity adapter: bootstrap, listeners, commands, auth sessions, multification, scheduler. Builds `target/navauth-velocity-<version>.jar`. |
| `integration/ajqueue` | Separate plugin connecting NavAuth to AjQueue. |
| `navauth-docs-gen` | Generates `docs/docs/generated/*` from annotations (commands, events, migration origins). |
| `navauth-api-examples` | Example API consumer. |
| `buildSrc` | Shading convention `navauth-shadow`. |
| `docs/` | VitePress user docs ([docs-site.md](docs-site.md)). |

## Layers in `navauth-common`

| Package | Holds | May import |
| --- | --- | --- |
| `domain/` | models, value classes (`UserUuid`, `MojangId`, `Username`), repository interfaces, auth session and handshake models | nothing from Exposed, JDBC, Velocity or HTTP |
| `application/` | services and use cases (`UserService`, `UsernameResolutionService`, `AuthHandshakeSessionService`, …) | domain |
| `infra/` | Exposed tables and repositories (`infra/persistence`), database connection and Flyway (`infra/database`), crypto and hashers, Mojang HTTP clients, event bus | anything |
| `migrate/` | migrators from other plugins and between databases | |
| `config/`, `component/` | okaeri configs, `TextComponent` | |
| `command/` | platform-independent command pieces (argument resolver, handlers, configurer interface) | |
| `module/` | Guice modules | |

Platform code (Velocity events, commands, players) lives only in `navauth-velocity`. A directory
path always equals the package name.

## Guice modules

Built in `velocity/NavAuthVelocity.kt` as a child injector of Velocity's, in this order:
`PluginDirectoryModule`, four `YamlConfigModule`s (`general.yml` with subconfigs bound, `messages.yml`,
`migration.yml`, `commands.yml`), `ExecutorsModule`, `EventsModule`, `VelocityMultificationsModule`,
`VelocityCommandsModule`, `SchedulerModule`, `GsonModule`, `HttpClientModule`,
`DataPersistenceModule`, `ServicesModule`, `VelocityServicesModule`, `MigrationModule`. The order
matters: later modules read configs bound by earlier ones.

## Startup and shutdown

`NavAuthVelocity.onProxyInitializeEvent`:
1. builds the injector (configs are created or updated on disk here);
2. `DatabaseManager.connectAndInit()`: Hikari pool, Flyway migrations, Exposed `Database`
   ([persistence.md](persistence.md)). **Nothing may resolve a repository before this**, because
   repositories get the bound Exposed `Database`;
3. sets the `NavAuthAPI` instance, starts bStats, registers listeners
   (`VelocityListenersRegistry`) and commands (`CommandsRegistry`, LiteCommands).

Any `Throwable` during startup shuts the proxy down: a proxy without NavAuth's listeners would let
everyone in unauthenticated. `ProxyShutdownEvent` closes the pool and unregisters commands, and
tolerates a failed startup (uninitialised injector).

## Where the main flows live

| Flow | Entry point |
| --- | --- |
| Joining the proxy | `velocity/listener/velocity/LoginListeners.kt` ([login-flow.md](login-flow.md)) |
| Limbo, initial server, disconnects | `velocity/listener/velocity/ConnectionListeners.kt` |
| Chat and commands before login | `velocity/listener/velocity/ChatListeners.kt`, `CommandsListeners.kt` |
| Login, register, 2FA sessions | `velocity/infra/auth/*AuthSession.kt`, `common/infra/auth/*` |
| Account changes | `common/application/user/UserService.kt` ([accounts.md](accounts.md)) |
| Commands | `velocity/command/**` ([commands-and-permissions.md](commands-and-permissions.md)) |
| Database | `common/infra/database`, `common/infra/persistence` ([persistence.md](persistence.md)) |

## Related

[login-flow.md](login-flow.md) · [persistence.md](persistence.md) ·
[build-and-release.md](build-and-release.md) · [README.md](README.md)
