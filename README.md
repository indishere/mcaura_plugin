# MinecraftAura

MinecraftAura is a lightweight, configurable **greeting and farewell plugin** designed for Paper-based servers.
It focuses on clarity, permission control, and predictable behavior without unnecessary complexity.

No NMS. No magic hacks. Just clean server-side logic.

---

## ✨ Features

* Custom **join and quit messages**
* `/greet` and `/bye` commands with optional target players
* Central `/aura` command with subcommands
* Fully permission-based behavior (LuckPerms compatible)
* Configurable **cooldowns** to prevent spam
* Optional **file-based message pools** (`.txt`)
* Supports `&` color codes in custom files
* Controlled broadcasting (no accidental global spam)
* Autocompletion for commands and player names
* Debug mode for detailed logging

---

## 📦 Compatibility

* **Platform:** Paper & Paper forks (Purpur, Pufferfish, etc.)
* **Minecraft Versions:** 1.20.x
* **Java:** 17+
* **Not compatible with Forge or Fabric** (this is a server plugin)

---

## 🚀 Installation

1. Download the latest `.jar` from **Releases**
2. Place it in:

   ```
   /plugins/
   ```
3. Start the server once
4. Stop the server
5. Edit:

   ```
   plugins/MinecraftAura/config.yml
   ```
6. (Optional) Add custom message files
7. Start the server again

---

## ⚙️ Commands

### Main Command

```
/aura
```

Subcommands:

```
/aura help
/aura greet [player]
/aura bye [player]
/aura reload
```

### Player Commands

```
/greet [player]
/bye [player]
```

Aliases:

```
/greeting
/goodbye
```

Console behavior:

* ✅ `/aura greet <player>`
* ❌ `/greet <player>` (intentional)

---

## 🔐 Permissions

| Permission             | Description            |
| ---------------------- | ---------------------- |
| `aura.main`            | Access `/aura`         |
| `aura.greet`           | Receive join greetings |
| `aura.bye`             | Receive quit messages  |
| `aura.cmd.greet`       | Use `/greet`           |
| `aura.cmd.bye`         | Use `/bye`             |
| `aura.reloadconfig`    | Reload configuration   |
| `aura.cooldown.bypass` | Ignore cooldowns       |
| `aura.aura`            | Full admin override    |

All permissions are compatible with **LuckPerms**.

---

## 🧠 Configuration (`config.yml`)

```yaml
enable-join-messages: true
enable-quit-messages: true
hide-vanilla-messages: true

broadcast-command-messages: true
debug: false

greet-bye-cooldown-seconds: 30
reload-cooldown-seconds: 60

greetings-file: greetings.txt
byes-file: byes.txt
```

### Notes

* `-1` disables a feature
* `0` removes cooldowns
* Cooldowns are internally capped for safety
* Debug mode provides verbose logging for troubleshooting

---

## 📄 Custom Message Files

Place `.txt` files inside:

```
plugins/MinecraftAura/
```

### Rules

* One message per line
* Lines starting with `#` are ignored
* Supports `&` color codes
* Use `%player%` as a placeholder

Example:

```txt
&aWelcome back, %player%!
&7Another player has joined the server.
```

---

## 🧪 Building from Source

### Requirements

* Java 17+
* Maven

```bash
mvn clean package
```

Output:

```
target/MinecraftAura-<version>.jar
```

---

## 📌 Design Philosophy

* Predictable behavior over clever tricks
* Permissions first
* Configurable without recompiling
* No silent failures
* Minimal overhead

Built for servers that prefer control over chaos.

---

## 🧾 License

**The Unlicense**

This software is released into the public domain.
Do whatever you want with it.

---

## 👤 Author & Contact

**IND_is_Here**
GitHub: [https://github.com/indishere](https://github.com/indishere)
Discord: **ind_is_here**
