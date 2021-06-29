package at.haha007.extrajail.bungee;

import at.haha007.extrajail.common.JailData;
import at.haha007.extrajail.common.MySqlDatabase;
import at.haha007.extrajail.common.PluginVariables;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static at.haha007.extrajail.common.PluginVariables.ADD_CHANNEL;
import static at.haha007.extrajail.common.PluginVariables.SET_CHANNEL;
import static net.md_5.bungee.api.ChatColor.*;

public class JailCommand extends Command {
    private final ServerInfo jailServer;
    private final String statement;
    private final MySqlDatabase database;
    ExtraJailBungeePlugin plugin = ExtraJailBungeePlugin.getInstance();

    public JailCommand() {
        super("extrajail", "extrajail.command", "jail", "extrajail");
        statement = ExtraJailBungeePlugin.getInstance().getStatement();
        jailServer = ExtraJailBungeePlugin.getInstance().getJailServer();
        database = ExtraJailBungeePlugin.getInstance().getDatabase();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        /*
            /jail <name>
            /jail <name> history
            /jail <name> add <amount> <reason>
            /jail <name> set <amount> <reason>
         */
        if(!(sender instanceof ProxiedPlayer player)){
            sender.sendMessage(comp(GOLD + "This command can only be executed by players."));
            return;
        }
        if (args.length == 0) {
            sendCommandInfo(sender);
            return;
        }
        if (args.length == 1) {
            sendJailBlocks(sender, args[0]);
            return;
        }
        String reason;
        int amount;
        switch (args[1]) {
            case "set" -> {
                if (args.length < 3) {
                    sendCommandInfo(sender);
                    return;
                }
                reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sendCommandInfo(sender);
                    return;
                }
                setJailBlocks(player, args[0], amount, reason);
            }
            case "add" -> {
                if (args.length < 3) {
                    sendCommandInfo(sender);
                    return;
                }
                reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sendCommandInfo(sender);
                    return;
                }
                addJailBlocks(player, args[0], amount, reason);
            }
            case "history" -> {
                UUID uuid = plugin.getUUID(args[0]);
                List<HistoryEntry> history = HistoryEntry.getHistory(uuid);
                history.sort(Comparator.comparingLong(HistoryEntry::getTimeCreated));
                sender.sendMessage(comp(GOLD + "Jail History of " + AQUA + args[0]));
                history.forEach(e -> sender.sendMessage(comp(GOLD + e.toString())));
            }
        }
    }

    private void addJailBlocks(ProxiedPlayer sender, String name, int amount, String reason) {
        if (amount <= 0) {
            sender.sendMessage(new ComponentBuilder("Amount must be at least 1!").color(GOLD).create());
            return;
        }
        UUID uuid = plugin.getUUID(name);
        if (uuid == null) {
            sender.sendMessage(comp(GOLD + "Player not found: " + name));
            return;
        }
        if (jailServer.getPlayers().isEmpty())
            setJailBlocksLocal(uuid, getJailBlocks(uuid) + amount);
        else
            jailServer.sendData(ADD_CHANNEL, new JailData(uuid, amount).encode());
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        if (p != null && p.isConnected() && p.getServer().getInfo() != jailServer)
            p.connect(jailServer);
        sender.sendMessage(new ComponentBuilder("Jail blocks updated!").color(GOLD).create());
        new HistoryEntry(sender.getUniqueId(), sender.getName(), uuid, name, reason, System.currentTimeMillis(), amount, "add").insert();
    }

    private void setJailBlocks(ProxiedPlayer sender, String name, int amount, String reason) {
        if (amount <= 0) {
            sender.sendMessage(new ComponentBuilder("Amount must be at least 1!").color(GOLD).create());
            return;
        }
        UUID uuid = plugin.getUUID(name);
        if (uuid == null) {
            sender.sendMessage(comp(GOLD + "Player not found: " + name));
            return;
        }
        if (jailServer.getPlayers().isEmpty())
            setJailBlocksLocal(uuid, amount);
        else
            jailServer.sendData(SET_CHANNEL, new JailData(uuid, amount).encode());
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        if (p != null && p.isConnected() && p.getServer().getInfo() != jailServer)
            p.connect(jailServer);
        sender.sendMessage(new ComponentBuilder("Jail blocks updated!").color(GOLD).create());
        new HistoryEntry(sender.getUniqueId(), sender.getName(), uuid, name, reason, System.currentTimeMillis(), amount, "set").insert();
    }

    private void setJailBlocksLocal(UUID uuid, int amount) {
        try (PreparedStatement ps = database.prepareStatement("REPLACE INTO " + PluginVariables.blockTable + " (UUID, BLOCKS) values(?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getJailBlocks(UUID uuid) {
        try (PreparedStatement ps = database.prepareStatement(statement)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }


    private void sendJailBlocks(CommandSender sender, String name) {
        UUID uuid = ExtraJailBungeePlugin.getInstance().getUUID(name);
        if (uuid == null) {
            sender.sendMessage(comp(GOLD + "Player not found: " + name));
            return;
        }
        try (PreparedStatement ps = database.prepareStatement(statement)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int blocks = rs.getInt(1);
                if (blocks == 0)
                    sender.sendMessage(comp(GOLD + "Player not in jail: " + name));
                else
                    sender.sendMessage(comp(GOLD + name + ": " + AQUA + blocks));
                return;
            }
            sender.sendMessage(comp(GOLD + "Player not in jail: " + name));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendCommandInfo(CommandSender sender) {
        sender.sendMessage(comp("/jail <name>"));
        sender.sendMessage(comp("/jail <name> history"));
        sender.sendMessage(comp("/jail <name> add <amount> <reason>"));
        sender.sendMessage(comp("/jail <name> set <amount> <reason>"));
    }

    private BaseComponent[] comp(String text) {
        return new ComponentBuilder(text).color(GOLD).create();
    }
}
