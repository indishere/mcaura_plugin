package io.pain.ind.aura;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class MinecraftAura extends JavaPlugin implements Listener {

    private static final List<String> GREETINGS = List.of(
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

    private static final List<String> BYES = List.of(
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

    // Config-driven settings
    private boolean enableJoinMessages;
    private boolean enableQuitMessages;
    private boolean hideVanillaMessages;
    private int commandCooldownSeconds;

    // Cooldown tracking (per player)
    private final ConcurrentHashMap<UUID, Long> lastCommandUseMs = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MinecraftAura enabled");
    }

    @Override
    public void onDisable() {
        lastCommandUseMs.clear();
        getLogger().info("MinecraftAura disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        // /configreload (console OR player)
        if (cmd.equals("configreload")) {
            if (!sender.hasPermission("aura.configreload")) {
                sender.sendMessage("You do not have permission to reload the configuration.");
                return true;
            }

            reloadConfig();
            loadSettings();
            sender.sendMessage("MinecraftAura config reloaded.");
            return true;
        }

        // Everything else is player-only
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        // Optional perms for commands (defaults can be true in plugin.yml)
        if (cmd.equals("greeting") && !player.hasPermission("aura.greeting")) {
            player.sendMessage("No permission.");
            return true;
        }
        if (cmd.equals("bye") && !player.hasPermission("aura.bye")) {
            player.sendMessage("No permission.");
            return true;
        }

        // Cooldown for /greeting + /bye (unless bypass)
        if ((cmd.equals("greeting") || cmd.equals("bye"))
                && commandCooldownSeconds > 0
                && !player.hasPermission("aura.cooldown.bypass")) {

            long now = System.currentTimeMillis();
            long last = lastCommandUseMs.getOrDefault(player.getUniqueId(), 0L);
            long cooldownMs = commandCooldownSeconds * 1000L;
            long remainingMs = (last + cooldownMs) - now;

            if (remainingMs > 0) {
                long remainingSec = (remainingMs + 999) / 1000; // round up
                player.sendMessage("Cooldown: wait " + remainingSec + "s.");
                return true;
            }

            lastCommandUseMs.put(player.getUniqueId(), now);
        }

        switch (cmd) {
            case "greeting" -> broadcastRandomFrom(GREETINGS, player);
            case "bye" -> broadcastRandomFrom(BYES, player);
            default -> { return false; }
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (hideVanillaMessages) {
            event.setJoinMessage(null);
        }
        if (enableJoinMessages) {
            broadcastRandomFrom(GREETINGS, event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (hideVanillaMessages) {
            event.setQuitMessage(null);
        }
        if (enableQuitMessages) {
            broadcastRandomFrom(BYES, event.getPlayer());
        }

        // ✅ cleanup so the map doesn’t grow forever
        lastCommandUseMs.remove(event.getPlayer().getUniqueId());
    }

    private void broadcastRandomFrom(List<String> lines, Player player) {
        if (lines.isEmpty()) return;

        String msg = lines.get(ThreadLocalRandom.current().nextInt(lines.size()))
                .replace("%player%", player.getName());

        getServer().broadcastMessage(msg);
    }

    private void loadSettings() {
        enableJoinMessages = getConfig().getBoolean("enable-join-messages", true);
        enableQuitMessages = getConfig().getBoolean("enable-quit-messages", true);
        hideVanillaMessages = getConfig().getBoolean("hide-vanilla-messages", true);

        commandCooldownSeconds = getConfig().getInt("command-cooldown-seconds", 30);
        if (commandCooldownSeconds < 0) commandCooldownSeconds = 0;
        if (commandCooldownSeconds > 3600) commandCooldownSeconds = 3600; // cap at 1 hour

        getLogger().info("Config: join=" + enableJoinMessages
                + ", quit=" + enableQuitMessages
                + ", hideVanilla=" + hideVanillaMessages
                + ", cooldown=" + commandCooldownSeconds + "s");
    }
}
