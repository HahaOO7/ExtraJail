package at.haha007.extrajail.spigot;

import at.haha007.extrajail.common.MySqlDatabase;
import at.haha007.extrajail.common.PluginVariables;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExtraJailSpigotPlugin extends JavaPlugin {
    @Getter
    private static ExtraJailSpigotPlugin instance;
    @Getter
    private  JailPluginMessageHandler channel;
    @Getter
    private MySqlDatabase database;
    private Jail jail;

    /*
      jail player from any server
      teleport jailed players to jail server
      inventory has to be saved


      bungee -> spigot
        add, set

      onAdd:
      onSet
      onJoin:
        updateJail

      onSendToJail:
        save inventory
      onQuit:
        save blocks
      onJoin
        checkJail
     */

    public void onEnable() {
        instance = this;
        channel = new JailPluginMessageHandler();
        saveDefaultConfig();
        reloadConfig();
        initDatabase();
        Bukkit.getScheduler().runTask(this, this::enable);
    }

    private void enable() {
        new CommandBlocker();
        jail = new Jail();
    }

    @Override
    public void onDisable() {
        JailPlayer.clearCache();
        jail.respawnAllBlocks();
    }

    @SneakyThrows
    private void initDatabase() {
        FileConfiguration cfg = getConfig();
        database = new MySqlDatabase(cfg.getString("sql.host"),
                cfg.getString("sql.username"),
                cfg.getString("sql.password"),
                cfg.getString("sql.database"),
                cfg.getString("sql.datasource"),
                cfg.getBoolean("sql.useSSL"));
        database.connect();
        PluginVariables.blockTable = cfg.getString("sql.blocks");
        PluginVariables.playerTable = cfg.getString("sql.players");
        PluginVariables.inventoryTable = cfg.getString("sql.inventories");
        database.prepareStatement("CREATE TABLE IF NOT EXISTS " + PluginVariables.playerTable +
                "(UUID varchar(36), NAME varchar(100), PRIMARY KEY (UUID, NAME))").executeUpdate();
        database.prepareStatement("CREATE TABLE IF NOT EXISTS " + PluginVariables.inventoryTable +
                "(UUID varchar(36), INVENTORY blob, PRIMARY KEY (UUID))").executeUpdate();
        database.prepareStatement("CREATE TABLE IF NOT EXISTS " + PluginVariables.blockTable +
                "(UUID varchar(36), BLOCKS int(32), PRIMARY KEY (UUID))").executeUpdate();
    }
}
