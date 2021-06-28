package at.haha007.extrajail.spigot;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerJailEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Getter
    private final JailPlayer jailPlayer;

    public PlayerJailEvent(Player player, JailPlayer jailPlayer) {
        super(player);
        this.jailPlayer = jailPlayer;
    }
}
