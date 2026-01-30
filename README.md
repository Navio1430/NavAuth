
## NavAuth is available in alpha version ❗
As of now, only an alpha version is available.
We are working on additional configuration options and other improvements to prepare for the release of NavAuth 1.0.

<div align="center">

  <h1>NavAuth 🔐 - autologin plugin</h1>
  <p><b>NavAuth</b> is a next-generation Minecraft login plugin built for <b>speed</b>, <b>security</b>, and seamless player authentication. Designed with modern servers in mind, it combines <b>performance</b>, <b>reliability</b>, and <b>integration flexibility</b>.</p>

  [![Velocity](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/velocity_vector.svg)](https://modrinth.com/plugin/navauth)
  [![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/plugin/navauth)
  [![Github](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/Navio1430/NavAuth)

  [![Gradle](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)
  [![Kotlin](assets/built_with_kotlin.svg)](https://kotlinlang.org/docs/home.html)
  ![Java](assets/built_with_java.svg)

  [![Documentation](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/generic_vector.svg)](https://navio1430.github.io/NavAuth/docs)
  [![Kotlin](assets/read_javadoc.svg)](https://navio1430.github.io/NavAuth/javadoc)

</div>
<br>

## Join our Discord server
[![discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/discord-plural_vector.svg)](https://discord.gg/kREwg5Drnn)

## 🧱 Main principles
- be an auth/login plugin not an anti-vpn plugin
- simple, fast & secure

## 📊 Features

### 🚀 Some of the NavAuth features
- **Premium Auto-Login**
- **[2FA](https://navio1430.github.io/NavAuth/docs/general/2fa.html)**
- **[User sessions](https://navio1430.github.io/NavAuth/docs/general/user-lookup.html#lookup-user-sessions)**
- **[Multification](https://navio1430.github.io/NavAuth/docs/configuration/multification.html)**
- [Migration from other plugins](https://navio1430.github.io/NavAuth/docs/migration/migration.html)
- [Database type migrations](https://navio1430.github.io/NavAuth/docs/migration/database-type-migration.html)

### 🔒 In NavAuth password cracking takes more than TWELVE TRILLION (1.2 × 10^13) times longer than in the worst login plugins.
Cracking the whole database of a bad plugin takes less than a **FEW MINUTES**.

### What is not included?

- **Anti-Bot** - use [Sonar](https://github.com/jonesdevelopment/sonar/) for that
- **Anti-VPN**
- block multiple accounts per IP - currently not supported.
We've implemented user sessions which can be later used for an advanced mutli-account blocking solution.
You can also use other plugins (like Anti-VPN's) for that.

## 👥 Contributors

Thanks to **NavAuth contributors**:
- urhatedjack - logo design  
- [KotreQ](https://github.com/KotreQ) - help with the QR code generation

Thanks to **Discord support team**:
- [Blavez](https://github.com/Blavezz)
- [Jakub](https://github.com/jakmar06)
- 403

Thanks to people responsible for **QA**:
- [Jakub6666](https://github.com/jakmar06)
- [Helios](https://github.com/Helios3991)

Thanks to people that kept **LibreLoginProd alive** until NavAuth release and keep **Sapphire Hub alive**:
- [vuxeim](https://github.com/vuxeim) - newest versions supporter
- [Helios](https://github.com/Helios3991) - support on Discord
- [Jakub](https://github.com/jakmar06) - support on Discord

## 📘 Basic info

### 🧩 Requirements

* Java 21
* Limbo server e.g., NanoLimbo, PicoLimbo
* Velocity forwarding set to MODERN
* All backend servers must be 1.13+

### FAQ
Q: Why have I switched from maintaining LibreLoginProd?  
A: Please check [offer page](https://navio1430.github.io/NavAuth/docs/offer.html) for more info.

Q: Why is NavAuth almost 40MB in size?  
A: We bundle all the dependencies into one file.
> Downloading dependencies at runtime - where an application fetches required libraries upon startup or during execution rather than bundling them beforehand is generally considered bad practice in production environments due to reliability risks, performance overhead, and security concerns

### 📜 License

NavAuth is licensed under the GNU AGPL v3. See the license file for more information.

[![GNU AGPL Logo](https://www.gnu.org/graphics/agplv3-155x51.png)](https://www.gnu.org/licenses/agpl-3.0.en.html)

## 💡 TODO List
More planned features are described in [Documentation](https://navio1430.github.io/NavAuth/docs/offer.html#%F0%9F%9A%80-planned-features)
- readme:
   - add banner
- github/gh actions:
   - modrinth CD
   - issue template:
      - bug report
      - feature request
- commands and messages localization
