package at.haha007.extrajail.bungee;

import at.haha007.extrajail.common.MySqlDatabase;
import at.haha007.extrajail.common.PluginVariables;
import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import static at.haha007.extrajail.common.PluginVariables.ADD_CHANNEL;

public class ExtraJailBungeePlugin extends Plugin implements Listener {


    /*
      cmd
      onPlayerConnect:
        check jail
      onPlayerChangeServer:
        check jail
      check jail:
        send player to jail server
      bungee -> spigot
        add, set
     */
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
        kickMessage = cfg.getConfig().getString("kickMessage");
        jailServer = getProxy().getServerInfo(cfg.getConfig().getString("jailServer"));
        statement = "SELECT BLOCKS FROM " + PluginVariables.blockTable + " WHERE UUID = ?";
    }

    private void savePlayerUniqueId(ProxiedPlayer player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        try (PreparedStatement ps = database.prepareStatement("REPLACE INTO " + PluginVariables.playerTable + " (UUID, NAME) VAlUES (?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        checkJailAsync(event.getPlayer(), event.getTarget() != jailServer);
    }

    private void checkJailAsync(ProxiedPlayer player, boolean checkJail) {
        getProxy().getScheduler().runAsync(this, () -> {
            savePlayerUniqueId(player);
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
            if (rs.next())
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
        try (PreparedStatement ps = database.prepareStatement("SELECT UUID FROM " + PluginVariables.playerTable + " WHERE NAME = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return UUID.fromString(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
