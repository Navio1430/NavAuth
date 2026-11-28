# Login flow

How a connection becomes an authenticated player. Velocity runs with `online-mode = false`;
NavAuth decides per connection whether Mojang must authenticate it (premium) or the player logs in
with a password (cracked). Everything here is security-critical: read [security.md](security.md)
before changing it.

## Where it lives

| Path | What |
| --- | --- |
| `velocity/listener/velocity/LoginListeners.kt` | PreLogin, GameProfileRequest, LoginEvent, PostLogin |
| `common/application/auth/username/UsernameResolutionService.kt` | decides online/offline mode and renames at PreLogin |
| `common/domain/auth/handshake/AuthHandshakeSession.kt` | state of one connection between PreLogin and PostLogin; `verifyPremiumIdentity` |
| `common/application/auth/handshake/AuthHandshakeSessionService.kt` | handshake sessions, Guava cache, `SESSION_TTL` 15 s |
| `velocity/infra/auth/VelocityUniqueSessionId.kt` | session key `username:ip:port` |
| `common/application/auth/session/AuthSessionService.kt` | auth sessions of connected players |
| `velocity/application/auth/session/VelocityAuthSessionFactory.kt`, `velocity/infra/auth/*AuthSession.kt`, `common/infra/auth/*` | login, register and auto-login sessions, timers, attempts |
| `velocity/listener/velocity/ConnectionListeners.kt` | initial server (limbo or target), server switches, disconnect |
| `velocity/listener/velocity/ChatListeners.kt`, `CommandsListeners.kt` | chat and commands before login |
| `velocity/application/auth/InvalidSessionCacheService.kt` | descriptive "invalid session" message for a failed premium handshake |
| `velocity/application/server/*` | limbo / initial server selection and sending players on |

## Steps

1. **PreLogin** (`PostOrder.LAST`, whole body in try/catch → deny on any error):
   - drops a stale handshake session with the same id;
   - name must match `usernamePattern`, else `invalidUsernameError`;
   - `UsernameResolutionService.resolveUsernameConflicts(name, accountByNameIgnoreCase)` returns
     `ENFORCE_PREMIUM` (Velocity forces online mode: Mojang encryption + session server) or `NONE`
     (offline), or a failure reason that becomes a kick message;
   - stores an `AuthHandshakeSession(existingUser, connUsername, encryption, state)`.
2. **GameProfileRequest** (after Mojang authentication for online connections):
   `session.verifyPremiumIdentity(event.isOnlineMode, originalProfile.id)`. If it holds and an
   account exists, the profile gets the stored UUID and name (backends see the account's UUID).
   If not, the player keeps their own profile and the session is flagged. When it holds and no
   account exists, but an account already has the UUID Velocity assigned
   (`event.originalProfile.id`), the session is flagged `uuidInUseByAnotherAccount` and the
   connection gets a throwaway random-UUID profile (name and properties kept).
3. **LoginEvent** (`priority = Short.MIN_VALUE`, runs last, only ever denies): denies a missing
   session ("Auth session expired"), a premium identity mismatch
   (`premiumUsernameOwnedByAnotherAccountError` or `unexpectedLoginErrorKickMessage`) and a UUID that
   belongs to an account (`usernameUnavailableKickMessage`).
4. **PostLogin** (`PostOrder.FIRST`): creates the auth session
   (`createAuthSession`), then closes the handshake session. A flagged handshake session gets no
   auth session: the player is disconnected.
   - `ENFORCE_PREMIUM`: refused unless `player.isOnlineMode`. Existing account: a password an
     import left on it is removed (`UserService.removeLeftoverPremiumPassword`); if it still
     requires credentials (2FA) a login session starts, otherwise auto-login. No account: a premium
     account is stored with the Mojang UUID as its UUID, then auto-login.
   - `NONE`: login session for an existing account, register session otherwise.
5. **Initial server** (`PlayerChooseInitialServer`, lowest priority): unauthenticated players go
   to a `limboServers` server (state `WAITING_FOR_ALLOCATION` → `WAITING_FOR_HANDLER`),
   authenticated ones to `initialServers`. **ServerPreConnect** (`Short.MIN_VALUE`) keeps
   unauthenticated players on the limbo and denies players without a session. It returns when the
   result is already denied and decides on `event.result.server`, the server Velocity really
   connects to.
6. **Login / register / 2FA** run on the limbo through commands (`/login`, `/register`, `/2fa`).
   The time limit (`maxLoginDuration`) and the repeating prompts start when the session is created
   in PostLogin, not when the limbo is reached. `maxLoginAttempts` wrong passwords kick; a login
   session verifies one attempt at a time ([accounts.md](accounts.md#encryption-queue)). On success
   the session is authenticated (`UserAuthenticatedEvent`) and the player is sent to the initial
   server (`VelocityServerConnectService`).
7. **Disconnect** closes the auth session and removes the player's hashing task from the queue.

## Chat and commands before login

`ChatListeners` and `CommandsListeners` deny the chat and the commands of a player who is not
authenticated (no auth session counts as not authenticated). Each has two deny-only handlers, and
neither ever sets `allowed()`:

- `onChatFirst` / `onCommandFirst` at `PostOrder.FIRST`: other plugins already see the input of a
  player who is not logged in as denied.
- `onChat` / `onCommand` at `priority = Short.MIN_VALUE`: run after every other plugin and check
  again. `onCommand` decides on the effective command (the result's replacement command, else the
  typed one) and also denies `forwardToServer` results unless the command is one of the allowed
  auth commands ([commands-and-permissions.md](commands-and-permissions.md#commands-before-login)).
- `onPlayerAvailableCommands` runs at `Short.MIN_VALUE` as well.

Velocity runs handlers by `@Subscribe(priority)` whenever it is not 0, higher values first, and by
`order` otherwise; `PostOrder.LAST` maps to −32767. `Short.MIN_VALUE` therefore runs after every
other plugin.

## Username resolution

Decided in this order (`existing` = account found by name in any letter case, `profile` = profile
API answer for the name, may come from the not-found cache):

| Situation | Result |
| --- | --- |
| profile API failed | refuse `ProfileAPIFailure` |
| `existing` premium, `profile` belongs to another Mojang UUID | refuse `PremiumUsernameOwnedByAnotherAccount` |
| no `existing`, `profile` premium, an account with that Mojang UUID exists | rename it to the Mojang spelling; then refuse `PremiumUsernameNotIdentical` if the connection's spelling differs, else premium (`PREMIUM_USERNAME_MIGRATED`); rename onto a taken name → `UsernameMigrationFailedUsernameAlreadyTaken` |
| `existing` premium, Mojang spelling differs only in letter case | rename to the Mojang spelling, premium |
| `existing` cracked, `profile` premium with the same spelling | offline (`NONPREMIUM_WITH_SAME_PREMIUM_NICKNAME`): password login, never auto-login |
| `existing` cracked, `profile` premium in another case | refuse `NonPremiumWithPremiumConflict` |
| `existing` cracked, connection spelling differs | refuse `NonPremiumUsernameNotIdentical` |
| `existing` premium | connection spelling must equal the stored one, else `PremiumUsernameNotIdentical`; premium |
| no account | premium name: spelling must match Mojang's, premium (`NEW_ACCOUNT`); else offline register |

## Rules

- Only a connection Mojang authenticated (online mode) completes an `ENFORCE_PREMIUM` handshake;
  an offline connection never gets a premium identity or creates a premium account.
- Only the Mojang-verified UUID proves who is connecting; profile API answers are cached and can be
  stale. A stored identity is handed out only when it equals the stored `mojang_uuid`.
- Every listener fails closed: an exception or a missing session denies or disconnects. An allowed
  result lets the connection continue without NavAuth's checks.
- Session keys include the IP; never key a session by name or port alone.
- `LoginEvent`, `ServerPreConnect`, command and chat handling stay at `Short.MIN_VALUE` and
  deny-only; PostLogin stays first.
- Nothing blocking (database, HTTP, hashing) on Velocity network threads: listeners use async
  tasks, commands `@Async`, hashing the encryption queue.

## Traps

- Renames at PreLogin happen before Mojang verified anyone, from possibly stale profile data. They
  never grant access (the identity check refuses the login) and the owner's next join undoes them
  (TESTING.md L16).
- A premium name whose previous owner renamed stays blocked for the new owner until the previous
  owner joins with the new name or an admin acts.
- `AuthSessionState.AUTH_IN_PROGRESS` exists but nothing sets it.
- `LoginScenarioTests.join` mirrors these listeners step by step for end-to-end tests; keep it in
  step when the listeners change.

## Related

[security.md](security.md) · [accounts.md](accounts.md) · [mojang-profiles.md](mojang-profiles.md) ·
[commands-and-permissions.md](commands-and-permissions.md) · [../TESTING.md](../TESTING.md) (section L)
