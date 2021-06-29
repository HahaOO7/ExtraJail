package at.haha007.extrajail.spigot;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;
import java.util.stream.Collectors;

public class CommandBlocker implements Listener {
    ExtraJailSpigotPlugin plugin;
    Set<String> blockedCommands;
    String blockedMessage;

    public CommandBlocker() {
        plugin = ExtraJailSpigotPlugin.getInstance();
        FileConfiguration cfg = plugin.getConfig();
        blockedCommands = cfg.getStringList("blockedCommands").stream().map(String::toLowerCase).collect(Collectors.toSet());

        blockedMessage = cfg.getString("blockedMessage");
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    void onCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().split(" ")[0].substring(1).toLowerCase();
        if (JailPlayer.get(event.getPlayer().getUniqueId()).isInJail() && blockedCommands.contains(cmd)) {
            event.getPlayer().sendMessage(blockedMessage);
            event.setCancelled(true);
        }
    }

}
