# Mojang profiles

NavAuth asks a profile API whether a name belongs to a premium (Mojang) account and which Mojang
UUID owns it. The answer decides whether a connection must authenticate with Mojang and gates the
premium switch and renames. It is a hint about a name, not proof of identity: answers are cached
and can be stale, and only Mojang's session server proves who is connecting, during login.

Paths: `common/…` = `navauth-common/src/main/kotlin/pl/spcode/navauth/common/…`,
`velocity/…` = `navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/…`.

## Where it lives

| Path | What |
| --- | --- |
| `common/application/mojang/ProfileService.kt` | `fetchProfileInfo(usernameCaseIgnored, useNotFoundCache = false): MojangProfile?` |
| `common/application/mojang/ProfileApiFetchException.kt` | "could not decide"; carries `causes` |
| `common/domain/mojang/MojangProfile.kt` | `MojangProfile(uuid: MojangId, name: Username)`, enum `MojangProfileApi { MINETOOLS, MOJANG }` |
| `common/infra/mojang/CompositeProfileService.kt` | the bound `ProfileService`: cache, API order, error handling |
| `common/infra/mojang/MojangProfileServiceImpl.kt` | `GET https://api.minecraftservices.com/minecraft/profile/lookup/name/<name>` |
| `common/infra/mojang/MineToolsProfileService.kt` | `GET https://api.minetools.eu/uuid/<name>` |
| `common/infra/mojang/ProfileCache.kt` | found / not-found cache |
| `common/config/MojangAPIConfig.kt` | `mojangAPIConfig` section of `general.yml` |
| `common/module/ServicesModule.kt`, `HttpClientModule.kt` | `ProfileService` → `CompositeProfileService`; the `HttpClient` |
| `navauth-common/src/test/kotlin/unit/mojang/*`, `fake/FakeProfileService.kt` | unit tests; the fake used by login scenario tests |

## How it works

**Answer contract.** A `MojangProfile` means the name is premium: `uuid` is the Mojang UUID and
`name` Mojang's spelling (lookups ignore letter case). `null` means no premium account has that
name: every API said so, or Mojang did (its answer is authoritative, see below).
`ProfileApiFetchException` means NavAuth could not decide.

**`CompositeProfileService.fetchProfileInfo`:**

1. The cache key is `name.lowercase()`. A cached `Found` is returned to every caller. A cached
   `NotFound` is returned (as `null`) only when `useNotFoundCache` is true; otherwise it is ignored
   and the APIs are asked.
2. The APIs run in `apiOrder` (default `[MINETOOLS, MOJANG]`), each request bounded by `apiTimeout`
   (default 5 s). The first API that returns a profile wins: it is cached and returned, even when
   an earlier API threw.
3. An API that returns `null` passes to the next; one that throws is remembered and the next is
   asked.
4. No profile found: when Mojang answered "not found", the result is `null` even if another API
   threw; otherwise, if any API threw, `ProfileApiFetchException` (nothing cached). A `null` result
   is cached as `NotFound` only when `useNotFoundCache` is true.

**What each API calls "not found":**

- Mojang: HTTP 404 → not found; 2xx → a profile (a missing `id` or `name` is an error); anything
  else is an error.
- MineTools: a non-2xx status is an error. A 2xx answer with `status` `OK`, `id` and `name` is a
  profile; `status` `ERR` with `id` null is not found (MineTools echoes the queried name in
  `name`); anything else, including a body that is not JSON, is an error.

**`ProfileCache`:** Guava cache, `expireAfterWrite(profileCacheTTL)` (default 5 min, read at
startup), no size limit, entries `Found(profile)` or `NotFound`. `invalidate` exists; nothing in the
plugin calls it.

**HTTP:** blocking `HttpClient.send`; worst case for one lookup is about `apiTimeout` × number of
APIs (10 s by default). `ProfileApiFetchException` logs itself when constructed: one error line for
the name plus one per cause with its stack trace.

## Where lookups happen

| Caller | Name | `useNotFoundCache` | On `ProfileApiFetchException` |
| --- | --- | --- | --- |
| PreLogin: `UsernameResolutionService.resolveUsernameConflicts` | connection name | `true` | `ProfileAPIFailure`: kick with `profileApiFailureKickMessage`, never a guess |
| `/premium` (`velocity/command/user/PremiumAccountCommand.kt`) | the player's name | `false` | `unexpectedErrorOccurred` |
| `/forcepremium` (`ForcePremiumAdminCommand`) | stored name of the target | `false` | `unexpectedErrorOccurred` |
| `/migrateuser` (`MigrateUserDataAdminCommand`), then `UserService.migrateData` | the new name (looked up twice) | `false` | `unexpectedErrorOccurred` |
| Migrators | none: LibreLogin's premium status comes from `premium_uuid` | — | — |

How PreLogin uses the answer (premium or offline, renames, refusals) is in
[login-flow.md](login-flow.md#username-resolution). A found profile can rename the account that
holds its Mojang UUID there, before Mojang verified the connection.

PreLogin may use the not-found cache because every cracked join looks its name up: caching "not
found" spares the APIs during join waves. A stale `NotFound` cannot hand out an account: an
existing premium account still requires online mode, and the session-server check refuses another
Mojang UUID (`LoginScenarioTests`, "stale not-found cache for a reclaimed name").

## Identity

A profile answer can be minutes old (`profileCacheTTL`), a third-party API can lag behind Mojang,
and names are released and reclaimed. Identity is proven only by the Mojang session server during
login (online-mode encryption), compared with the stored `mojang_uuid` in
`AuthHandshakeSession.verifyPremiumIdentity`: [login-flow.md](login-flow.md). A stale answer at
PreLogin can refuse a login or demand online mode; it never hands out a stored identity.

## Rules

- **Authoritative decisions never trust the not-found cache.** Premium switches, renames and
  `/migrateuser` call `fetchProfileInfo` with `useNotFoundCache = false`; only PreLogin username
  resolution passes `true`.
- **A profile answer is never proof of identity.** Never hand a connection a stored identity,
  auto-login or rename an account on a profile answer alone; that needs the Mojang-verified UUID
  ([login-flow.md](login-flow.md), [security.md](security.md)).
- **A failed lookup is never "not found".** `ProfileApiFetchException` refuses or aborts; never
  fall back to treating the name as cracked.
- The cache key stays normalised exactly like `username_lowercase` (`String.lowercase()`).
- Lookups are blocking HTTP: never on Velocity network threads, never inside a database
  transaction.
- A new API gets a `MojangProfileApi` constant (admins list them in `apiOrder`, so the names are a
  config contract) and returns `null` only for a definite "not found"; anything else throws.
- Mojang's "not found" is authoritative: another API's error never turns it into a failure. Without
  it, any API error fails the lookup.

## Traps

- `Found` entries are served to every caller, commands included: `/premium` can act on an answer
  up to `profileCacheTTL` old.
- The cache has no size limit: every distinct name of a join flood stays cached for the TTL.
- Names go into the request URL unencoded. The default `usernamePattern` is safe; a pattern that
  allows spaces, `/`, `?` or `#` makes lookups fail or ask about another name.
- `ProfileApiFetchException` already logs at error level; do not log it again with its stack trace.
- `HttpClientModule` is unscoped: each injection gets a new `HttpClient`.
- `integration/service/MojangProfileServiceTests` calls the real Mojang API and needs network.

## Related

[login-flow.md](login-flow.md) · [accounts.md](accounts.md) · [security.md](security.md) ·
[configs-and-messages.md](configs-and-messages.md) · [migrators.md](migrators.md) ·
[../TESTING.md](../TESTING.md) (section L)
