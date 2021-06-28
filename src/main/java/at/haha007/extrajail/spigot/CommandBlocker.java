package at.haha007.extrajail.spigot;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Set;

public class CommandBlocker implements Listener {
    ExtraJailSpigotPlugin plugin;
    Set<String> blockedCommands;
    String blockedMessage;

    public CommandBlocker() {
        plugin = ExtraJailSpigotPlugin.getInstance();
        FileConfiguration cfg = plugin.getConfig();
        blockedCommands = new HashSet<>(cfg.getStringList("blockedCommands"));
        blockedMessage = cfg.getString("blockedMessage");
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    void onCommand(PlayerCommandPreprocessEvent event) {
        if (JailPlayer.get(event.getPlayer().getUniqueId()).isInJail() &&
                blockedCommands.contains(event.getMessage().split(" ")[0].toLowerCase())) {
            event.getPlayer().sendMessage(blockedMessage);
            event.setCancelled(true);
        }
    }

}
