# Security

NavAuth decides who may play as which account on live networks, so a bug here gives one player
another player's UUID, data and permissions. These are the invariants every change keeps, and how
security fixes are handled.

## Invariants

- **Identity:** a premium account belongs to its Mojang UUID, not to its name. Only the UUID that
  Mojang's session server authenticated during login proves who is connecting
  (`AuthHandshakeSession.verifyPremiumIdentity`); profile API answers are cached and can be stale.
  Never hand a connection a stored identity, auto-login or rename an account on anything weaker.
- **UUIDs:** an account's UUID is never given to another player: a connection without an account
  whose Velocity-assigned UUID belongs to an account is denied (`LoginListeners.applyGameProfile`,
  [login-flow.md](login-flow.md)).
- **Online mode:** only a connection Mojang authenticated completes an `ENFORCE_PREMIUM`
  handshake. An offline connection never gets a premium identity and never creates a premium
  account.
- **Fail closed:** every Velocity listener denies the connection or disconnects the player on an
  exception or a missing session; an allowed result lets the connection continue without NavAuth's
  checks. A startup failure shuts the proxy down.
- **Sessions:** handshake and auth sessions are keyed by name, IP address and port.
- **Before login** a player can only reach the limbo, run the configured login/register/2FA
  commands and cannot chat.
- **Event order:** NavAuth's `LoginEvent`, `ServerPreConnect`, command and chat listeners run after
  every other plugin (`priority = Short.MIN_VALUE`) and only deny, so no other plugin can undo a
  denial. Commands and chat also have a deny-only handler at `PostOrder.FIRST`, so other plugins
  already see the input of a player who is not logged in as denied
  ([login-flow.md](login-flow.md#chat-and-commands-before-login)).
- **Passwords:** a premium account never keeps a password (only a 2FA secret). Every stored hash
  keeps verifying; legacy hashers stay until the owner removes them.
- **Secrets:** compare secrets in constant time; use `SecureRandom` for anything secret.

Details of each: [login-flow.md](login-flow.md), [accounts.md](accounts.md).

## Privacy

- Never log passwords, hashes, TOTP secrets or session tokens; log IP addresses only at debug level.
- Store no personal data without a clear purpose, a config switch and a docs note for privacy
  policies. NavAuth stores no IP addresses.

## Fixing and releasing security issues

- Public texts never explain how to exploit a hole: the `CHANGELOG.md` entry says what kind of
  issue was fixed and that every server should update; commit messages, docs, code comments and test
  names stay neutral too. No PR or issue numbers next to security entries.
- Credit reporters in *New contributors* without describing the issue.
- A fix that is pushed is public: push and release together, then the owner notifies server
  owners.
- **Known security weaknesses are never written into the repository** (wiki, docs, `TESTING.md`,
  comments, issues) before their fix is released; the owner tracks them privately. Tests that guard
  a fix use neutral names.
- Prefer private reports (GitHub Security Advisories) over public issues and pull requests.
- A hole in a released version deserves a patch release from `main`, not only a fix on the next
  version branch.

## Related

[login-flow.md](login-flow.md) · [accounts.md](accounts.md) · [build-and-release.md](build-and-release.md) ·
[../TESTING.md](../TESTING.md) (sections L and S)
