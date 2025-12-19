# 📦 MinecraftAura v0.1 Legacy Release

MinecraftAura v0.1 is the original, stable release of the plugin, focused on simple global greetings and farewells with minimal configuration.

This version is ideal for servers that want a lightweight, no-bs low customization and simple solution without advanced command logic or external files.

### ✨ What v0.1 Does
- Broadcasts random **join** and **quit** messages
- Provides `/greeting` and `/bye` commands
- Supports `%player%` placeholder
- Simple permission system
- Single shared cooldown for commands
- Configurable via `config.yml`

### ⚠️ Limitations
- All messages are **global broadcasts**
- No `/aura` parent command
- No subcommands or aliases
- No targeted messages
- No external message files
- No debug mode
- Limited permission granularity

v0.1 is stable, predictable, and intentionally minimal.

---

## 🔄 Upgrading to v0.2

Upgrading from **v0.1 → v0.2** is straightforward, but **not drop-in compatible**.

### Before Upgrading
1. **Back up your server**
2. Delete the old plugin jar
3. Remove the old `plugins/MinecraftAura/` folder (recommended)

### JUST UPGRADE
1. Grab the **v0.2** jar from [Github](https://github.com/indishere/mcaura_plugin/releases/tag/v0.2) or here's the direct download [LINK](https://github.com/indishere/mcaura_plugin/releases/download/v0.2/MinecraftAura-6.7.6.7_v0.2.jar)

### After Installing v0.2
- Review the **new permission nodes** (they have changed)
- Update any LuckPerms groups/users accordingly
- Review the new `/aura` command structure
- Optionally add custom `.txt` message files
- Adjust cooldowns (now separated by command type)

Old configs are **not guaranteed** to behave correctly with v0.2.

---

## 🧪 v0.2 Status — BETA

MinecraftAura **v0.2 is currently in BETA**.

While it introduces major improvements (targeted messages, better permissions, file-based messages, debug mode, autocompletion), it may still contain:
- Minor bugs
- Issues
- Behavior changes between updates

This is expected during beta.

**Recommendation:**
- Test v0.2 on a staging or smaller server first
- Keep v0.1 if you require absolute stability
- Report issues via GitHub/Discord if you encounter unexpected behavior

v0.2 represents a significant step forward, but stability will continue to improve over time.

---

If you prefer:
- stability → **v0.1**
- features & control → **v0.2 (beta)**

Choose accordingly.
