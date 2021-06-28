package at.haha007.extrajail.bungee;

import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.logging.Level;

public class BungeeConfig {
    private static final String configName = "bungeeConfig.yml";

    private final Plugin plugin;

    private Configuration dataConfig = null;

    private File configFile = null;

    public BungeeConfig() {
        this.plugin = ExtraJailBungeePlugin.getInstance();
        reloadConfig();
    }

    @SneakyThrows
    public void reloadConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), configName);
        if (!configFile.exists()) saveDefaultConfig();
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        dataConfig = provider.load(configFile);
        InputStream defaultStream = this.plugin.getResourceAsStream(configName);
        if (defaultStream != null) {
            Configuration defaultConfig = provider.load(new InputStreamReader(defaultStream));
            setDefaults(defaultConfig, dataConfig);
            saveConfig();
        }
    }

    private void setDefaults(Configuration defaults, Configuration section) {
        for (String key : defaults.getKeys()) {
            Object val = defaults.get(key);
            if (val instanceof Configuration) {
                if (!section.contains(key)) {
                    section.set(key, val);
                } else {
                    setDefaults(defaults.getSection(key), section.getSection(key));
                }
                continue;
            }
            if (section.contains(key)) continue;
            section.set(key, val);
        }
    }

    public Configuration getConfig() {
        if (this.dataConfig == null)
            reloadConfig();
        return this.dataConfig;
    }

    public void saveConfig() {
        if (this.dataConfig == null || this.configFile == null)
            return;
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(dataConfig, configFile);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, e);
        }
    }

    public void saveDefaultConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), "config.yml");
        try {
            File folder = configFile.getParentFile();
            if (!folder.exists() && !folder.mkdirs())
                throw new IOException("Couldn't create Folder: " + folder.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!this.configFile.exists()) {
            try (InputStream is = this.plugin.getResourceAsStream(configName);
                 DataInputStream dis = new DataInputStream(is);
                 FileOutputStream fos = new FileOutputStream(configFile)) {
                byte[] bytes = dis.readAllBytes();
                if (!configFile.exists() && !configFile.createNewFile())
                    throw new IOException("Couldn't create File: " + configFile.getName());
                fos.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


