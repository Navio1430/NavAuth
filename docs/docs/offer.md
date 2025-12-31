
# Why NavAuth?

NavAuth is a minecraft **premium auto-login** and auth gateway plugin built for **speed, security, and seamless player authentication**.

## 👑 First of all, why do we need another login plugin?

I created this plugin because none of the existing ones met my expectations.  
My main issues were:
- Lack of "cloud-native" support
- Poor and messy codebases
- Security vulnerabilities caused by bad code design
- No support for modern Minecraft versions

## 🚀 Planned Features
- support Paper platform
- auto resolution of conflicts
- option to translate uuid4 to uuid7
- multi-proxy with redis support
- auto-login offline mod
- 2FA
- login/2FA required by permission (good for admins)
- login session management with cookies
- advanced localization with multi-language support at one time
- floodgate support
- minecraft dialogs support for login/register
- whitelist for offline players
- password hashing algorithm auto-migration
- protected permission groups -> no way of changing passwords etc.

## 📊 Feature Comparison

| Feature             | NavAuth    | LibreLoginProd  |
|:--------------------|------------|-----------------|
| Supported Platforms | Velocity   | Paper, Velocity |
| Premium auto-login  | ✅          | ✅               |
todo

## Forward skin and profile data to backend servers

Why use SkinsRestorer while you have an auth gateway?
NavAuth forwards profile & skin data to all backend servers for you.

![img.webp](public/offer/skin.webp)

## Tests

We use CI tests to make sure our plugin is always fully secure and stable.

