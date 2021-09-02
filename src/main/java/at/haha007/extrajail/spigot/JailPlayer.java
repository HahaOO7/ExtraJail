package at.haha007.extrajail.spigot;

import at.haha007.extrajail.common.MySqlDatabase;
import at.haha007.extrajail.common.PluginVariables;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@AllArgsConstructor
public class JailPlayer {
    private static MySqlDatabase database;
    private final UUID uuid;
    @Getter
    @Setter
    private int amount;
    @Getter
    @Setter
    private List<ItemStack> items;


    private static final Map<UUID, JailPlayer> cache = new HashMap<>();
    private static final Set<UUID> loading = new HashSet<>();
    private static final int taskID;

    static {
        taskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(ExtraJailSpigotPlugin.getInstance(), () -> {
            cache.values().parallelStream().forEach(JailPlayer::saveBlocks);
            cleanCache();
        }, 1000, 1000);
    }

    static JailPlayer get(UUID uuid) {
        if (!cache.containsKey(uuid))
            loadJailPlayer(uuid);
        return cache.get(uuid);
    }

    public static void clearCache() {
        Bukkit.getScheduler().cancelTask(taskID);
        cache.values().forEach(id -> {
            id.saveBlocks();
            id.saveInventory();
        });
        cache.clear();
    }

    private static void cleanCache() {
        cache.keySet().stream().filter(id -> Bukkit.getPlayer(id) == null).collect(Collectors.toSet()).forEach(JailPlayer::deCache);
    }

    public static void deCache(UUID uuid) {
        if (!cache.containsKey(uuid)) return;
        cache.get(uuid).saveBlocks();
        cache.get(uuid).saveInventory();
        cache.remove(uuid);
    }

    private synchronized static void loadJailPlayer(UUID uuid) {
        if (loading.contains(uuid)) return;
        loading.add(uuid);
        ExtraJailSpigotPlugin pl = ExtraJailSpigotPlugin.getInstance();
        int amount = 0;
        List<ItemStack> items = null;
        database = pl.getDatabase();
        try (PreparedStatement blockStatement = database.prepareStatement("SELECT BLOCKS from " + PluginVariables.blockTable + " WHERE UUID = ?");
             PreparedStatement inventoryStatement = database.prepareStatement("SELECT INVENTORY from " + PluginVariables.inventoryTable + " WHERE UUID = ?")) {
            blockStatement.setString(1, uuid.toString());
            inventoryStatement.setString(1, uuid.toString());
            ResultSet blockResult = blockStatement.executeQuery();
            ResultSet inventoryResult = inventoryStatement.executeQuery();
            if (blockResult.next())
                amount = blockResult.getInt(1);
            if (inventoryResult.next()) {
                byte[] b = inventoryResult.getBytes(1);
                YamlConfiguration i = new YamlConfiguration();
                i.loadFromString(Objects.requireNonNull(decode(b)));
                items = Objects.requireNonNull(i.getList("items")).stream().map(v -> (ItemStack) v).collect(Collectors.toList());
                items = new ArrayList<>(items);
            }

        } catch (SQLException | InvalidConfigurationException | NullPointerException e) {
            e.printStackTrace();
        }
        cache.put(uuid, new JailPlayer(uuid, amount, items));
        loading.remove(uuid);
    }

    public void saveBlocks() {
        if (amount > 0) {
            try (PreparedStatement ps = database.prepareStatement("REPLACE INTO " + PluginVariables.blockTable + " (UUID, BLOCKS) values(?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try (PreparedStatement ps = database.prepareStatement("DELETE FROM " + PluginVariables.blockTable + " WHERE UUID = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveInventory() {
        if (items == null) {
            try (PreparedStatement ps = database.prepareStatement("DELETE FROM " + PluginVariables.inventoryTable + " WHERE UUID = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("items", items);
            String inventoryString = cfg.saveToString();
            try (PreparedStatement ps = database.prepareStatement("REPLACE INTO " + PluginVariables.inventoryTable + " (UUID, INVENTORY) values(?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setBytes(2, encode(inventoryString));
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private static byte[] encode(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
             GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(bytes);
            gos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String decode(byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gis = new GZIPInputStream(bis)) {
            return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isInventorySaved() {
        return items != null;
    }

    public boolean isInJail() {
        return amount > 0;
    }
}
