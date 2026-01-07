
# Why NavAuth?

NavAuth is a minecraft **premium auto-login** and auth gateway plugin built for **speed, security, and seamless player authentication**.

## 👑 First of all, why do we need another login plugin?

I created this plugin because none of the existing ones met my expectations.  
My main issues were:
- Lack of "cloud-native" support
- Poor and messy codebases
- Security vulnerabilities caused by bad code design
- No support for modern Minecraft versions
- Usage of unsecure hashing algorithms

## 🔒 In NavAuth password cracking takes more than TWELVE TRILLION (1.2 × 10^13) times longer than in the worst login plugins.
Cracking the whole database of a bad plugin takes less than a **FEW MINUTES**.

## 🔥 Migrate now!

1. Download the plugin from Modrinth or our GitHub page.
2. Check the [migration guide](/migration/migration.html) for more information.

## 📊 Feature Comparison

| Feature                                                         | NavAuth                                | LibreLoginProd                        |
|:----------------------------------------------------------------|----------------------------------------|---------------------------------------|
| Supported Platforms                                             | Velocity                               | Paper, Velocity                       |
| Premium auto-login                                              | ✅                                      | ✅                                     |
| Security                                                        | High                                   | Moderate                              |
| Performance                                                     | Fast                                   | Moderate                              |
| Large scale support                                             | ✅                                      | ❌                                     |
| Codebase quality                                                | Good                                   | Bad                                   |
| Supported Databases                                             | PostgreSQL, MySQL, MariaDB, SQLite, H2 | PostgreSQL, MySQL, MariaDB, SQLite    |
| [2FA](/general/2fa.html)                                        | ✅                                      | 🟧 (needs additional plugin)          |
| [User sessions](/general/user-lookup.html#lookup-user-sessions) | ✅                                      | ❌                                     |
| Active Maintenance                                              | ✅                                      | ❌                                     |
| Active Support                                                  | ✅                                      | ❌                                     |
| Multification                                                   | ✅                                      | ❌                                     |
| Plugins migration                                               | ✅ (any db type)                        | 🟧 (only for specific configurations) |
| Database type migration                                         | ✅                                      | 🟧 (partial)                          |
| Floodgate                                                       | ❌ (planned)                            | ✅                                     |

## 🚀 Planned Features
- support Paper platform
- auto resolution of conflicts
- option to translate uuid4 to uuid7
- multi-proxy with redis support
- auto-login offline mod
- login/2FA required by permission (good for admins)
- login session management with cookies
- advanced localization with multi-language support at one time
- floodgate support
- minecraft dialogs support for login/register
- whitelist for offline players
- password hashing algorithm auto-migration
- protected permission groups -> no way of changing passwords etc.

## 2FA Support

We care about security, and we want to make sure that your players/administrators are safe.  
Check [2FA page](/general/2fa.html) for more information.

## Forward skin and profile data to backend servers

Why use SkinsRestorer while you have an auth gateway?
NavAuth forwards profile & skin data to all backend servers for you.

![img.webp](public/offer/skin.webp)

## We test our code

We use CI tests for crucial parts to make sure our plugin is always fully secure and stable.

