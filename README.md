
<div display="flex" justify-content="space-between" align="center">
 <h1>NavAuth 🔐 - autologin plugin</h1>
  NavAuth is an upcoming next-generation Minecraft login plugin built for speed, security, and seamless player authentication. Designed with modern servers in mind, it combines performance, reliability, and integration flexibility.
</div>
<br>
<br>

## 🧱 Main principles
- be an auth/login plugin not an anti-vpn plugin
- simple, fast & secure

## 👥 Contributors

urhatedjack - logo design

## 📘 Basic info

| Platform | Supported |
|-----------|:---------:|
| Velocity | ✅ |

### 🧩 Requirements

* Java 21
* Limbo server e.g., NanoLimbo, PicoLimbo

### 📜 License

NavAuth is licensed under the GNU AGPL v3. See the license file for more information.

[![GNU AGPL Logo](https://www.gnu.org/graphics/agplv3-155x51.png)](https://www.gnu.org/licenses/agpl-3.0.en.html)

## 💡 TODO List

- readme:
   - add banner
   - add badges:
      - github stars
      - support discord
      - modrinth
      - documentation
      - minecraft versions
      - license
- github/gh actions:
   - modrinth CD
   - github CD
   - issue template:
      - bug report
      - feature request
   - github pages CD
- initialize docs
- minecraft commands:
   - /login
   - /register
   - ...
- define config structure (yaml/hocon?)
- commands and messages localization
- features:
   - password strength requirement
   - minecraft names validation
   - premium account autologin
   - forwarding skin and uuid info to backend servers

### Potential TODO's
- 2FA (e.g., required for certain permissions)
- prefixes/suffixes for cracked/obsidian players
- [instant authentication via client mod](https://github.com/Navio1430/LibreLoginProd/issues/39)
- client login session management (via mc 'cookies')
- translate uuid4's to uuid7's and forward them to backend servers as standard uuid
