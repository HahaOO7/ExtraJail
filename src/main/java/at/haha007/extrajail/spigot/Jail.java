package at.haha007.extrajail.spigot;

import lombok.ToString;
import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.*;


@ToString
public class Jail implements Listener {
    private final AxisAlignedBB box;
    private final Location spawn;
    private final String freeCommand;
    private final ItemStack jailTool = new ItemStack(Material.STONE_PICKAXE);
    private final Queue<JailBlock> blocks = new LinkedList<>();
    private final String title;
    private final String subtitle;
    private final long respawnTime;

    private record JailBlock(Vector block, long t) {
    }


    public Jail() {
        ExtraJailSpigotPlugin plugin = ExtraJailSpigotPlugin.getInstance();
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::respawnJailBlocks, 20, 20);

        FileConfiguration cfg = plugin.getConfig();
        freeCommand = cfg.getString("freeCommand");
        title = cfg.getString("title");
        subtitle = cfg.getString("subtitle");
        respawnTime = cfg.getLong("respawnTime");
        box = getBox(Objects.requireNonNull(cfg.getConfigurationSection("jail")));
        spawn = getLocation(Objects.requireNonNull(cfg.getConfigurationSection("jailLocation")));
    }

    private void respawnJailBlocks() {
        Iterator<JailBlock> iter = blocks.iterator();
        long t = System.currentTimeMillis() - respawnTime;
        while (iter.hasNext()) {
            JailBlock b = iter.next();
            if (b.t() > t) break;
            resetBlock(b.block().toLocation(spawn.getWorld()).getBlock());
            iter.remove();
        }
    }

    public void respawnAllBlocks() {
        blocks.stream().map(JailBlock::block).map(b -> b.toLocation(spawn.getWorld()).getBlock()).forEach(this::resetBlock);
        blocks.clear();
    }

    private void resetBlock(Block b) {
        Chunk chunk = b.getChunk();
        if (!chunk.isLoaded())
            chunk.load();
        b.setType(Material.COBBLESTONE);
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

    public void checkJail(Player player, Location location) {
        if (player.hasPermission("extrajail.bypass")) return;
        JailPlayer jp = JailPlayer.get(player.getUniqueId());
        if (!jp.isInJail()) {
            if (jp.isInventorySaved()) {
                freePlayer(player, jp);
            }
            return;
        }
        jailPlayer(player, jp, location);
    }

    private void jailPlayer(Player player, JailPlayer jp, Location loc) {
        Bukkit.getServer().getPluginManager().callEvent(new PlayerJailEvent(player, jp));
        if (loc.getWorld() != spawn.getWorld() || !box.e(loc.getX(), loc.getY(), loc.getZ())) {
            player.teleport(spawn);
        }
        PlayerInventory inv = player.getInventory();
        if (!jp.isInventorySaved()) {
            List<ItemStack> items = new ArrayList<>(Arrays.asList(inv.getContents()));
            items.removeIf(Objects::isNull);
            jp.setItems(items);
        }
        inv.clear();
        inv.setItem(0, jailTool);
        inv.setHeldItemSlot(0);
    }

    private void freePlayer(Player player, JailPlayer jp) {
        jp.saveBlocks();
        Bukkit.getServer().getPluginManager().callEvent(new PlayerFreeEvent(player, jp));
        giveItems(player, jp.getItems());
        jp.setItems(null);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), freeCommand.replace("%player%", player.getName()));
    }

    private void giveItems(Player player, List<ItemStack> items) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        for (ItemStack item : items) {
            if (item == null) continue;
            inv.addItem(item);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        JailPlayer jp = JailPlayer.get(player.getUniqueId());
        if (jp.isInJail()) breakBlock(jp, event);
        checkJail(player, player.getLocation());
    }

    void breakBlock(JailPlayer jp, BlockBreakEvent event) {
        Block block = event.getBlock();
        if (event.getBlock().getType() != Material.COBBLESTONE ||
                !box.e(block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            return;
        }
        blocks.add(new JailBlock(block.getLocation().toVector(), System.currentTimeMillis()));
        jp.setAmount(jp.getAmount() - 1);
        event.getPlayer().sendTitle(title.replace("%blocks%", Integer.toString(jp.getAmount())),
                subtitle.replace("%blocks%", Integer.toString(jp.getAmount())), 0, 20, 10);
    }

    @EventHandler
    void onItemPickup(PlayerAttemptPickupItemEvent event) {
        if (JailPlayer.get(event.getPlayer().getUniqueId()).isInJail())
            event.setCancelled(true);
    }

    @EventHandler
    void onItemDrop(PlayerDropItemEvent event) {
        if (JailPlayer.get(event.getPlayer().getUniqueId()).isInJail())
            event.setCancelled(true);
    }

    @EventHandler
    void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        event.setCancelled(true);
        p.setHealth(20);
        p.teleport(spawn);
    }

    @EventHandler
    void onPlayerMove(PlayerMoveEvent event) {
        checkJail(event.getPlayer(), event.getTo());
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        checkJail(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    void onTeleport(PlayerTeleportEvent event) {
        checkJail(event.getPlayer(), event.getTo());
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        JailPlayer.deCache(event.getPlayer().getUniqueId());
    }
}
