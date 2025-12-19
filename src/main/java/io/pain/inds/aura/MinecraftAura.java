package io.pain.ind.aura;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class MinecraftAura extends JavaPlugin implements Listener {

    // Prefix used everywhere (chat + logs where it makes sense)
    private static final String PREFIX = "§7[§bAura§7] §r";

    // Built-in defaults (used if no file is configured or file load fails)
    private static final List<String> DEFAULT_GREETINGS = List.of(
            "You exist, %player%. Hello.",
            "The server noticed you, %player%.",
            "Oh look, %player% showed up.",
            "%player%, you logged in. Bold choice.",
            "Reality loaded successfully for %player%.",
            "Another soul joins the chaos: %player%.",
            "The void says hi to %player%.",
            "This world just got louder. Thanks, %player%.",
            "Congrats, %player%. You spawned.",
            "Presence detected: %player%.",
            "You made it, %player%. Somehow.",
            "The server sighs. Welcome, %player%.",
            "Pixels aligned. Hello, %player%.",
            "%player%, you are now part of this mess.",
            "%player% spawned without instructions.",
            "Welcome, %player%. Try not to die.",
            "The simulation continues with %player%.",
            "Your journey begins again, %player%.",
            "Existence confirmed: %player%.",
            "Server status: still running. %player% too."
    );

    private static final List<String> DEFAULT_BYES = List.of(
            "Leaving so soon, %player%?",
            "Goodbye, %player%. Try not to break anything.",
            "%player% has left the simulation.",
            "Farewell, %player%. The void awaits.",
            "See you later, %player%. Or not...",
            "%player% logged out. Reality feels emptier.",
            "Another one departs: %player%.",
            "Goodbye, %player%. Don't let the pixels bite.",
            "%player% has exited the matrix.",
            "Logging off already, %player%?",
            "Goodbye, %player%. May your next world be kinder.",
            "%player% vanished into the ether.",
            "Farewell, %player%. The server breathes easier.",
            "See you next time, %player%. If there is one.",
            "%player% has left this digital realm.",
            "Goodbye, %player%. The code will miss you.",
            "%player% logged out. The game pauses.",
            "Another soul departs: %player%.",
            "Get outta here, %player%.",
            "Bye, %player%."
    );

    // Runtime message pools (loaded from files if configured)
    private List<String> greetingsPool = DEFAULT_GREETINGS;
    private List<String> byesPool = DEFAULT_BYES;

    // Config-driven settings
    private boolean enableJoinMessages;
    private boolean enableQuitMessages;
    private boolean hideVanillaMessages;
    private boolean broadcastCommandMessages;
    private boolean debug;

    // Cooldowns (0 = none, -1 = disabled)
    private int greetByeCooldownSeconds;
    private int reloadCooldownSeconds;

    // Optional message files (in plugins/MinecraftAura/)
    private String greetingsFile;
    private String byesFile;

    // Cooldown tracking (per player)
    private final ConcurrentHashMap<UUID, Long> lastGreetByeUseMs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastReloadUseMs = new ConcurrentHashMap<>();

    // Shutdown guard
    private volatile boolean shuttingDown = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        loadSettings(true);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Enabled " + getDescription().getName() + " v" + getDescription().getVersion()
                + " | join=" + enableJoinMessages
                + " quit=" + enableQuitMessages
                + " hideVanilla=" + hideVanillaMessages
                + " broadcastCmd=" + broadcastCommandMessages
                + " greetByeCooldown=" + greetByeCooldownSeconds + "s"
                + " reloadCooldown=" + reloadCooldownSeconds + "s"
                + " debug=" + debug);

        if (debug) {
            getLogger().info("[DEBUG] Data folder: " + getDataFolder().getAbsolutePath());
            getLogger().info("[DEBUG] greetingsPool=" + greetingsPool.size() + " lines, byesPool=" + byesPool.size() + " lines");
            getLogger().info("[DEBUG] greetings-file=" + greetingsFile + ", byes-file=" + byesFile);
        }
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        lastGreetByeUseMs.clear();
        lastReloadUseMs.clear();
        getLogger().info("Disabled " + getDescription().getName());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (shuttingDown) return true;

        String cmd = command.getName().toLowerCase();

        // -------------------------
        // /aura <subcommand> [options]
        // -------------------------
        if (cmd.equals("aura")) {

            // plugin.yml enforces aura.main, but aura.aura is "god mode"
            if (!hasAnyPermission(sender, "aura.main", "aura.aura")) {
                sender.sendMessage(PREFIX + "You do not have permission to use /aura.");
                return true;
            }

            // /aura or /aura help
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sendAuraHelp(sender);
                return true;
            }

            String sub = args[0].toLowerCase();

            // /aura configreload (also accept reloadconfig / reload)
            if (sub.equals("configreload") || sub.equals("reloadconfig") || sub.equals("reload")) {

                if (reloadCooldownSeconds == -1) {
                    sender.sendMessage(PREFIX + "Config reload is disabled by config. (reload-cooldown-seconds = -1)");
                    return true;
                }

                if (!hasAnyPermission(sender, "aura.reloadconfig", "aura.aura")) {
                    sender.sendMessage(PREFIX + "You do not have permission to reload the configuration.");
                    return true;
                }

                if (sender instanceof Player p) {
                    if (!checkCooldown(p, lastReloadUseMs, reloadCooldownSeconds, "Reload")) return true;
                }

                reloadConfig();
                loadSettings(false);
                sender.sendMessage(PREFIX + "MinecraftAura config reloaded.");

                if (debug) {
                    getLogger().info("[DEBUG] Config reloaded by " + sender.getName());
                }
                return true;
            }

            // /aura greet [player_name]
            if (sub.equals("greet")) {

                if (greetByeCooldownSeconds == -1) {
                    sender.sendMessage(PREFIX + "Greeting commands are disabled by config. (greet-bye-cooldown-seconds = -1)");
                    return true;
                }

                if (!hasAnyPermission(sender, "aura.cmd.greet", "aura.aura")) {
                    sender.sendMessage(PREFIX + "What did you do to deserve this? NO Perms man.");
                    return true;
                }

                Player target;
                if (args.length >= 2) {
                    target = findOnlinePlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(PREFIX + "Player not found (must be online).");
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(PREFIX + "Console must use: §f/aura greet <player>§r");
                    return true;
                }

                if (sender instanceof Player p) {
                    if (!checkCooldown(p, lastGreetByeUseMs, greetByeCooldownSeconds, "Command")) return true;
                }

                deliverCommandAura(greetingsPool, sender, target);
                return true;
            }

            // /aura bye [player_name]
            if (sub.equals("bye")) {

                if (greetByeCooldownSeconds == -1) {
                    sender.sendMessage(PREFIX + "Bye commands are disabled by config. (greet-bye-cooldown-seconds = -1)");
                    return true;
                }

                if (!hasAnyPermission(sender, "aura.cmd.bye", "aura.aura")) {
                    sender.sendMessage(PREFIX + "What did you do to deserve this? NO Perms man.");
                    return true;
                }

                Player target;
                if (args.length >= 2) {
                    target = findOnlinePlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(PREFIX + "Player not found (must be online).");
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(PREFIX + "Console must use: §f/aura bye <player>§r");
                    return true;
                }

                if (sender instanceof Player p) {
                    if (!checkCooldown(p, lastGreetByeUseMs, greetByeCooldownSeconds, "Command")) return true;
                }

                deliverCommandAura(byesPool, sender, target);
                return true;
            }

            sender.sendMessage(PREFIX + "Unknown subcommand. Try §f/aura help§r.");
            return true;
        }

        // -------------------------
        // /greet [player_name] (player-only by design)
        // -------------------------
        if (cmd.equals("greet")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(PREFIX + "Console must use: §f/aura greet <player>§r");
                return true;
            }

            if (greetByeCooldownSeconds == -1) {
                player.sendMessage(PREFIX + "This command is disabled by config. (greet-bye-cooldown-seconds = -1)");
                return true;
            }

            if (!hasAnyPermission(player, "aura.cmd.greet", "aura.aura")) {
                player.sendMessage(PREFIX + "What did you do to deserve this? NO Perms man.");
                return true;
            }

            Player target = player;
            if (args.length >= 1) {
                Player maybe = findOnlinePlayer(args[0]);
                if (maybe == null) {
                    player.sendMessage(PREFIX + "Player not found (must be online).");
                    return true;
                }
                target = maybe;
            }

            if (!checkCooldown(player, lastGreetByeUseMs, greetByeCooldownSeconds, "Command")) return true;

            deliverCommandAura(greetingsPool, player, target);
            return true;
        }

        // -------------------------
        // /bye [player_name] (player-only by design)
        // -------------------------
        if (cmd.equals("bye")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(PREFIX + "Console must use: §f/aura bye <player>§r");
                return true;
            }

            if (greetByeCooldownSeconds == -1) {
                player.sendMessage(PREFIX + "This command is disabled by config. (greet-bye-cooldown-seconds = -1)");
                return true;
            }

            if (!hasAnyPermission(player, "aura.cmd.bye", "aura.aura")) {
                player.sendMessage(PREFIX + "What did you do to deserve this? NO Perms man.");
                return true;
            }

            Player target = player;
            if (args.length >= 1) {
                Player maybe = findOnlinePlayer(args[0]);
                if (maybe == null) {
                    player.sendMessage(PREFIX + "Player not found (must be online).");
                    return true;
                }
                target = maybe;
            }

            if (!checkCooldown(player, lastGreetByeUseMs, greetByeCooldownSeconds, "Command")) return true;

            deliverCommandAura(byesPool, player, target);
            return true;
        }

        return false;
    }

    // -------------------------
    // Join/Quit events
    // -------------------------
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (shuttingDown) return;

        if (hideVanillaMessages) event.setJoinMessage(null);

        Player p = event.getPlayer();
        if (!enableJoinMessages) return;

        if (!hasAnyPermission(p, "aura.greet", "aura.aura")) return;

        broadcastAura(greetingsPool, p);

        if (debug) {
            getLogger().info("[DEBUG] Join aura sent for " + p.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (hideVanillaMessages) event.setQuitMessage(null);

        Player p = event.getPlayer();
        if (!shuttingDown && enableQuitMessages && hasAnyPermission(p, "aura.bye", "aura.aura")) {
            broadcastAura(byesPool, p);

            if (debug) {
                getLogger().info("[DEBUG] Quit aura sent for " + p.getName());
            }
        }

        // cleanup maps
        lastGreetByeUseMs.remove(p.getUniqueId());
        lastReloadUseMs.remove(p.getUniqueId());
    }

    // -------------------------
    // Tab completion (autocompletion)
    // -------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("aura")) {
            if (args.length == 1) {
                return filterStartsWith(List.of("help", "greet", "bye", "configreload", "reloadconfig", "reload"), args[0]);
            }

            if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("greet") || sub.equals("bye")) {
                    return filterStartsWith(onlinePlayerNames(), args[1]);
                }
            }

            return Collections.emptyList();
        }

        if (cmd.equals("greet") || cmd.equals("bye")) {
            if (args.length == 1) {
                return filterStartsWith(onlinePlayerNames(), args[0]);
            }
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    // -------------------------
    // Message delivery logic
    // -------------------------
    private void deliverCommandAura(List<String> pool, CommandSender sender, Player target) {
        String raw = pickLine(pool).replace("%player%", target.getName());
        String finalMsg = PREFIX + raw;

        boolean senderIsTarget = (sender instanceof Player p) && p.getUniqueId().equals(target.getUniqueId());

        // Stop broadcasting when target ≠ sender (always direct to target)
        // If sender == target: broadcast only if config allows, otherwise direct to player.
        if (senderIsTarget && broadcastCommandMessages) {
            getServer().broadcastMessage(finalMsg);
        } else {
            target.sendMessage(finalMsg);
        }

        // confirmation to sender if they targeted someone else
        if (!senderIsTarget) {
            sender.sendMessage(PREFIX + "Sent to §f" + target.getName() + "§r.");
        }

        if (debug) {
            getLogger().info("[DEBUG] deliverCommandAura sender=" + sender.getName()
                    + " target=" + target.getName()
                    + " broadcast=" + (senderIsTarget && broadcastCommandMessages));
        }
    }

    private void broadcastAura(List<String> pool, Player player) {
        String raw = pickLine(pool).replace("%player%", player.getName());
        getServer().broadcastMessage(PREFIX + raw);
    }

    private String pickLine(List<String> pool) {
        if (pool == null || pool.isEmpty()) return "…nothing happened. (empty message pool)";
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    // -------------------------
    // Cooldowns
    // -------------------------
    private boolean checkCooldown(Player player, ConcurrentHashMap<UUID, Long> map, int cooldownSeconds, String label) {
        if (shuttingDown) return false;
        if (cooldownSeconds <= 0) return true;

        // bypass perms
        if (hasAnyPermission(player, "aura.cooldown.bypass", "aura.aura")) return true;

        long now = System.currentTimeMillis();
        long last = map.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMs = cooldownSeconds * 1000L;
        long remainingMs = (last + cooldownMs) - now;

        if (remainingMs > 0) {
            long remainingSec = (remainingMs + 999) / 1000;
            player.sendMessage(PREFIX + label + " cooldown: wait " + remainingSec + "s.");
            if (debug) {
                getLogger().info("[DEBUG] cooldown blocked player=" + player.getName()
                        + " label=" + label + " remaining=" + remainingSec + "s");
            }
            return false;
        }

        map.put(player.getUniqueId(), now);
        return true;
    }

    // -------------------------
    // Help output (useful + roast at top & bottom)
    // -------------------------
    private void sendAuraHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + "§eHelp Menu §7(because reading is hard apparently)");
        sender.sendMessage(PREFIX + "§7/aura help §8- Shows this menu");
        sender.sendMessage(PREFIX + "§7/aura greet [player_name] §8- Send a greeting aura");
        sender.sendMessage(PREFIX + "§7/aura bye [player_name] §8- Send a goodbye aura");
        sender.sendMessage(PREFIX + "§7/greet [player_name] §8- Same thing (player-only)");
        sender.sendMessage(PREFIX + "§7/bye [player_name] §8- Same thing (player-only)");
        sender.sendMessage(PREFIX + "§7Aliases: §f/greeting§7, §f/goodbye");
        sender.sendMessage(PREFIX + "§7/aura configreload §8- Reload config (admin)");
        sender.sendMessage(PREFIX + "§8Now go touch grass. Or at least a config file.");
    }

    // -------------------------
    // Config loading (+ file loading + debug)
    // -------------------------
    private void loadSettings(boolean startup) {
        enableJoinMessages = getConfig().getBoolean("enable-join-messages", true);
        enableQuitMessages = getConfig().getBoolean("enable-quit-messages", true);
        hideVanillaMessages = getConfig().getBoolean("hide-vanilla-messages", true);

        broadcastCommandMessages = getConfig().getBoolean("broadcast-command-messages", true);
        debug = getConfig().getBoolean("debug", false);

        greetByeCooldownSeconds = clampCooldown("greet-bye-cooldown-seconds",
                getConfig().getInt("greet-bye-cooldown-seconds", 30));

        reloadCooldownSeconds = clampCooldown("reload-cooldown-seconds",
                getConfig().getInt("reload-cooldown-seconds", 60));

        greetingsFile = getConfig().getString("greetings-file", "");
        byesFile = getConfig().getString("byes-file", "");

        greetingsPool = loadPoolFromFileOrDefault(greetingsFile, DEFAULT_GREETINGS, "greetings");
        byesPool = loadPoolFromFileOrDefault(byesFile, DEFAULT_BYES, "byes");

        if (debug) {
            getLogger().info("[DEBUG] Settings loaded"
                    + " | join=" + enableJoinMessages
                    + " quit=" + enableQuitMessages
                    + " hideVanilla=" + hideVanillaMessages
                    + " broadcastCmd=" + broadcastCommandMessages
                    + " greetByeCooldown=" + greetByeCooldownSeconds
                    + " reloadCooldown=" + reloadCooldownSeconds
                    + " greetingsPool=" + greetingsPool.size()
                    + " byesPool=" + byesPool.size());
        } else if (startup) {
            getLogger().info("Config loaded | greetings=" + greetingsPool.size()
                    + " byes=" + byesPool.size()
                    + " | broadcastCmd=" + broadcastCommandMessages
                    + " | debug=" + debug);
        }
    }

    private int clampCooldown(String key, int value) {
        if (value == -1) return -1; // disabled

        if (value < 0) {
            getLogger().warning("Config '" + key + "' was " + value + " (invalid). Treated as 0.");
            return 0;
        }

        if (value > 3600) {
            getLogger().warning("Config '" + key + "' was " + value + " (too high). Capped at 3600 seconds.");
            return 3600;
        }

        return value;
    }

    private List<String> loadPoolFromFileOrDefault(String fileName, List<String> fallback, String kind) {
        String name = (fileName == null) ? "" : fileName.trim();

        // Treat placeholders like "<file.txt>" as "not set"
        if (name.isEmpty() || name.contains("<") || name.contains(">")) {
            if (debug) getLogger().info("[DEBUG] " + kind + "-file not set, using defaults.");
            return fallback;
        }

        if (!name.toLowerCase().endsWith(".txt")) {
            getLogger().warning("Invalid " + kind + "-file '" + name + "': only .txt supported. Using defaults.");
            return fallback;
        }

        // PRODUCTION READY - path traversal protection
        File f = new File(getDataFolder(), name);

        if (!f.exists()) {
            getLogger().warning(
              "Missing " + kind + "-file '" + name + "' at " + f.getAbsolutePath()
                            + ". Using defaults."
            );
            return fallback;
        }

        // Path traversal protection (canonical path must stay inside plugin data folder)
        try {
            String canonicalFile = f.getCanonicalPath();
            String canonicalFolder = getDataFolder().getCanonicalPath();

        // File must be inside plugin folder
        if (!canonicalFile.startsWith(canonicalFolder)) {
            getLogger().warning(
                "Security: " + kind + "-file '" + name
                        + "' is outside plugin folder. Using defaults."
        );

        if (debug) {
            getLogger().info("[DEBUG] Blocked path: " + canonicalFile);
        }
        return fallback;
    }

    // File cannot be the plugin folder itself
    if (canonicalFile.equals(canonicalFolder)) {
        getLogger().warning(
                "Invalid " + kind + "-file '" + name
                        + "': path points to plugin folder itself. Using defaults."
        );
        return fallback;
    }

    } catch (IOException e) {
        getLogger().warning(
            "Security check failed for " + kind + "-file '" + name
                    + "'. Using defaults."
    );

    if (debug) {
        getLogger().info("[DEBUG] Security check exception: " + e.getMessage());
    }
    return fallback;
    }

    try {
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        List<String> cleaned = new ArrayList<>();

        for (String line : lines) {
            if (line == null) continue;
            String s = line.trim();
            if (s.isEmpty()) continue;
            if (s.startsWith("#")) continue;
            
            cleaned.add(ChatColor.translateAlternateColorCodes('&', s));
        }

        if (cleaned.isEmpty()) {
            getLogger().warning("File '" + name + "' had no usable lines. Using defaults.");
            return fallback;
        }

        if (debug) {
            getLogger().info("[DEBUG] Loaded " + cleaned.size() + " " + kind + " lines from " + f.getAbsolutePath());
        }

        return List.copyOf(cleaned);

    } catch (IOException e) {
        getLogger().warning("Failed reading " + kind + "-file '" + name + "': " + e.getMessage() + ". Using defaults.");
        return fallback;
    }
}


    // -------------------------
    // Helpers
    // -------------------------
    private Player findOnlinePlayer(String name) {
        Player exact = getServer().getPlayerExact(name);
        if (exact != null) return exact;
        return getServer().getPlayer(name);
    }

    private boolean hasAnyPermission(CommandSender sender, String... nodes) {
        if (sender == null || nodes == null) return false;
        for (String n : nodes) {
            if (n != null && sender.hasPermission(n)) return true;
        }
        return false;
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player p : getServer().getOnlinePlayers()) {
            names.add(p.getName());
        }
        Collections.sort(names);
        return names;
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        if (options == null) return Collections.emptyList();
        if (prefix == null) prefix = "";
        String p = prefix.toLowerCase();

        List<String> out = new ArrayList<>();
        for (String opt : options) {
            if (opt != null && opt.toLowerCase().startsWith(p)) out.add(opt);
        }
        return out;
    }
}
