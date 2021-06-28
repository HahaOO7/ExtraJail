package at.haha007.extrajail.bungee;

import at.haha007.extrajail.common.JailData;
import at.haha007.extrajail.common.MySqlDatabase;
import at.haha007.extrajail.common.PluginVariables;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
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
            case "set":
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
                setJailBlocks(sender, args[0], amount, reason);
                break;
            case "add":
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
                addJailBlocks(sender, args[0], amount, reason);
                break;
            case "history":
                sender.sendMessage(comp(RED + "Feature not implemented yet!"));
                break;
        }
    }

    private void addJailBlocks(CommandSender sender, String name, int amount, String reason) {
        UUID uuid = plugin.getUUID(name);
        if (uuid == null) {
            sender.sendMessage(comp(GOLD + "Player not found: " + name));
            return;
        }
        if (jailServer.getPlayers().isEmpty())
            setJailBlocksLocal(uuid, getJailBlocks(uuid) + amount);
        else
            jailServer.sendData(ADD_CHANNEL, new JailData(uuid, amount).encode());
        sender.sendMessage(new ComponentBuilder("Jail blocks updated!").color(GOLD).create());
    }

    private void setJailBlocks(CommandSender sender, String name, int amount, String reason) {
        UUID uuid = plugin.getUUID(name);
        if (uuid == null) {
            sender.sendMessage(comp(GOLD + "Player not found: " + name));
            return;
        }
        if (jailServer.getPlayers().isEmpty())
            setJailBlocksLocal(uuid, amount);
        else
            jailServer.sendData(SET_CHANNEL, new JailData(uuid, amount).encode());
        sender.sendMessage(new ComponentBuilder("Jail blocks updated!").color(GOLD).create());
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
            ps.setLong(1, uuid.getLeastSignificantBits());
            ps.setLong(2, uuid.getMostSignificantBits());
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
            ps.setLong(1, uuid.getLeastSignificantBits());
            ps.setLong(2, uuid.getMostSignificantBits());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sender.sendMessage(comp(GOLD + name + ": " + AQUA + rs.getInt(1)));
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
