package at.haha007.extrajail.spigot;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerFreeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    private final JailPlayer jailPlayer;

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public PlayerFreeEvent(Player player, JailPlayer jailPlayer) {
        super(player);
        this.jailPlayer = jailPlayer;
    }
}
