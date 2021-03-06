package at.haha007.extrajail.spigot;

import at.haha007.extrajail.common.JailData;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static at.haha007.extrajail.common.PluginVariables.ADD_CHANNEL;
import static at.haha007.extrajail.common.PluginVariables.SET_CHANNEL;

public class JailPluginMessageHandler {

    public JailPluginMessageHandler() {
        ExtraJailSpigotPlugin plugin = ExtraJailSpigotPlugin.getInstance();
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, SET_CHANNEL, this::onSetMessageReceived);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, ADD_CHANNEL, this::onAddMessageReceived);
    }

    @SneakyThrows
    private void onAddMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        JailData jd = JailData.decode(message);
        if (jd == null) throw new IOException("JailPlayer couldn't be decoded!");
        JailPlayer jp = JailPlayer.get(jd.getUuid());
        jp.setAmount(jp.getAmount() + jd.getBlocks());
        Player p = Bukkit.getPlayer(jd.getUuid());
        if (p != null && p.isOnline())
            ExtraJailSpigotPlugin.getInstance().getJail().checkJail(p, p.getLocation());
    }

    @SneakyThrows
    private void onSetMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        JailData jd = JailData.decode(message);
        if (jd == null) throw new IOException("JailPlayer couldn't be decoded!");
        JailPlayer jp = JailPlayer.get(jd.getUuid());
        jp.setAmount(jd.getBlocks());
        Player p = Bukkit.getPlayer(jd.getUuid());
        if (p != null && p.isOnline())
            ExtraJailSpigotPlugin.getInstance().getJail().checkJail(p, p.getLocation());
    }
}
