package at.haha007.extrajail.bungee;

import at.haha007.extrajail.common.MySqlDatabase;
import at.haha007.extrajail.common.PluginVariables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import static at.haha007.extrajail.common.PluginVariables.ADD_CHANNEL;

public class ExtraJailBungeePlugin extends Plugin implements Listener {


    private static final JSONParser parser = new JSONParser();
    Cache<String, UUID> uuidCache;
    Cache<UUID, String> nameCache;

    @Getter
    private static ExtraJailBungeePlugin instance;
    @Getter
    private ServerInfo jailServer;
    private String kickMessage;
    @Getter
    private MySqlDatabase database;
    @Getter
    private BungeeConfig cfg;
    @Getter
    private String statement;

    public void onEnable() {
        instance = this;
        loadConfig();
        int cacheSize = cfg.getConfig().getInt("cacheSize");
        uuidCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
        nameCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
        initSql();
        getProxy().getPluginManager().registerCommand(this, new JailCommand());
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel(ADD_CHANNEL);
    }

    private void initSql() {
        Configuration cfg = this.cfg.getConfig();
        Configuration sql = Objects.requireNonNull(cfg.getSection("sql"));
        database = new MySqlDatabase(
                sql.getString("host"),
                sql.getString("username"),
                sql.getString("password"),
                sql.getString("database"),
                sql.getString("datasource"),
                false
        );
        database.connect();
        tryExecuteUpdate("CREATE TABLE IF NOT EXISTS " + PluginVariables.blockTable +
                "(UUID varchar(36), BLOCKS int(32), PRIMARY KEY (UUID))");
        tryExecuteUpdate("CREATE TABLE IF NOT EXISTS " + PluginVariables.playerTable +
                "(UUID varchar(36), NAME varchar(100), PRIMARY KEY (UUID, NAME))");
        try {
            HistoryEntry.createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void tryExecuteUpdate(String statement) {
        try (PreparedStatement ps = database.prepareStatement(statement)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        cfg = new BungeeConfig();
        cfg.reloadConfig();
        PluginVariables.blockTable = cfg.getConfig().getString("sql.blocks");
        PluginVariables.playerTable = cfg.getConfig().getString("sql.players");
        PluginVariables.historyTable = cfg.getConfig().getString("sql.history");
        kickMessage = cfg.getConfig().getString("kickMessage");
        jailServer = getProxy().getServerInfo(cfg.getConfig().getString("jailServer"));
        statement = "SELECT BLOCKS FROM " + PluginVariables.blockTable + " WHERE UUID = ?";
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        checkJailAsync(event.getPlayer(), event.getTarget() != jailServer);
    }

    private void checkJailAsync(ProxiedPlayer player, boolean checkJail) {
        getProxy().getScheduler().runAsync(this, () -> {
            if (checkJail) {
                checkJail(player);
            }
        });
    }

    private void checkJail(ProxiedPlayer player) {
        UUID uuid = player.getUniqueId();
        try (PreparedStatement ps = database.prepareStatement(statement)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0)
                player.connect(jailServer, (success, error) -> {
                    if (!success) {
                        player.disconnect(TextComponent.fromLegacyText(kickMessage));
                    }
                });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public UUID getUUID(String name) {
        UUID uuid = uuidCache.getIfPresent(name);
        if (uuid != null) {
            return uuid;
        }
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStream is = url.openStream();
            StringBuilder s = new StringBuilder(new String(is.readAllBytes()));
            is.close();
            s = new StringBuilder(((JSONObject) parser.parse(s.toString())).get("id").toString());
            s.insert(20, '-');
            s.insert(16, '-');
            s.insert(12, '-');
            s.insert(8, '-');
            uuid = UUID.fromString(s.toString());
            uuidCache.put(name, uuid);
            return uuid;
        } catch (IOException | ParseException e) {
            return null;
        }
    }

    public String getName(UUID uuid) {
        String name = nameCache.getIfPresent(uuid);
        if (name != null) {
            return name;
        }
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", ""));
            InputStream is = url.openStream();
            String s = new String(is.readAllBytes());
            is.close();
            name = ((JSONObject) parser.parse(s)).get("name").toString();
            uuidCache.put(name, uuid);
            nameCache.put(uuid, name);
            return name;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        try (PreparedStatement ps = database.prepareStatement("SELECT NAME FROM " + PluginVariables.playerTable + " WHERE UUID = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
