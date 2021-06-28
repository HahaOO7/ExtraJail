package at.haha007.extrajail.spigot;

import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Jail implements Listener {
    private final AxisAlignedBB box;
    private final Location spawn;
    private final String freeCommand;
    private final ItemStack jailTool = new ItemStack(Material.STONE_PICKAXE);


    public Jail() {
        Bukkit.getServer().getPluginManager().registerEvents(this, ExtraJailSpigotPlugin.getInstance());

        FileConfiguration cfg = ExtraJailSpigotPlugin.getInstance().getCfg().getConfig();
        freeCommand = cfg.getString("freeCommand");
        box = getBox(Objects.requireNonNull(cfg.getConfigurationSection("jail")));
        spawn = getLocation(Objects.requireNonNull(cfg.getConfigurationSection("jailLocation")));
    }

    private Location getLocation(ConfigurationSection cfg) {
        double x = cfg.getDouble("x");
        double y = cfg.getDouble("y");
        double z = cfg.getDouble("z");
        float yaw = (float) cfg.getDouble("yaw");
        float pitch = (float) cfg.getDouble("pitch");
        World world = Bukkit.getWorld(Objects.requireNonNull(cfg.getString("world")));
        return new Location(world, x, y, z, yaw, pitch);
    }

    private AxisAlignedBB getBox(ConfigurationSection cfg) {
        int minX = cfg.getInt("minX");
        int minY = cfg.getInt("minY");
        int minZ = cfg.getInt("minZ");
        int maxX = cfg.getInt("maxX");
        int maxY = cfg.getInt("maxY");
        int maxZ = cfg.getInt("maxZ");
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void checkJail(Player player) {
        JailPlayer jp = JailPlayer.get(player.getUniqueId());
        if (!jp.isInJail()) {
            if (jp.isInventorySaved()) {
                freePlayer(player, jp);
            }
            return;
        }
        jailPlayer(player, jp);
    }

    private void jailPlayer(Player player, JailPlayer jp) {
        Bukkit.getServer().getPluginManager().callEvent(new PlayerJailEvent(player, jp));
        Location loc = player.getLocation();
        if(!box.e(loc.getX(), loc.getY(), loc.getZ())){
            player.teleport(spawn);
        }
        PlayerInventory inv = player.getInventory();
        if(!jp.isInventorySaved()){
            jp.setItems(Arrays.asList(inv.getContents()));
        }
        inv.clear();
        inv.setItem(0, jailTool);
        inv.setHeldItemSlot(0);
    }

    private void freePlayer(Player player, JailPlayer jp) {
        Bukkit.getServer().getPluginManager().callEvent(new PlayerFreeEvent(player, jp));
        giveItems(player, jp.getItems());
    }

    private void giveItems(Player player, List<ItemStack> items) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        for (ItemStack item : items) {
            inv.addItem(item);
        }
    }

    @EventHandler
    void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        JailPlayer jp = JailPlayer.get(player.getUniqueId());
        jp.setAmount(jp.getAmount() - 1);
        checkJail(player);
    }

    @EventHandler
    void onItemPickup(PlayerAttemptPickupItemEvent event) {
        if (JailPlayer.get(event.getPlayer().getUniqueId()).isInJail()) event.setCancelled(true);
    }

    @EventHandler
    void onPlayerMove(PlayerMoveEvent event) {
        checkJail(event.getPlayer());
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        checkJail(event.getPlayer());
    }

    @EventHandler
    void onTeleport(PlayerTeleportEvent event) {
        checkJail(event.getPlayer());
    }
}
