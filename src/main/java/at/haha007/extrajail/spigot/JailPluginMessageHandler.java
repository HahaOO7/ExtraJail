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
        System.out.println("AddMessageReceived: Tag: " + channel + "  UUID: " + jd.getUuid() + "   Blocks: " + jd.getBlocks());
    }

    @SneakyThrows
    private void onSetMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        JailData jd = JailData.decode(message);
        if (jd == null) throw new IOException("JailPlayer couldn't be decoded!");
        JailPlayer jp = JailPlayer.get(jd.getUuid());
        jp.setAmount(jd.getBlocks());
        System.out.println("SetMessageReceived: Tag: " + channel + "  UUID: " + jd.getUuid() + "   Blocks: " + jd.getBlocks());
    }
}
