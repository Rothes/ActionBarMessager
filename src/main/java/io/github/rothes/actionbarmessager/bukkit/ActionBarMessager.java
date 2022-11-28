package io.github.rothes.actionbarmessager.bukkit;

import io.github.rothes.actionbarmessager.bukkit.user.UserManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class ActionBarMessager extends JavaPlugin {

    private static ActionBarMessager instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private UserManager userManager;

    private YamlConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        config = YamlConfiguration.loadConfiguration(new File(instance.getDataFolder() + "/Config.yml"));
        I18nHelper.init(this);
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        new Updater().start();
        userManager = new UserManager();
        messageManager = new MessageManager();
        messageManager.start(this);

        Bukkit.getPluginManager().registerEvents(new Listeners(), this);

        CommandHandler commandHandler = new CommandHandler();
        PluginCommand command = this.getCommand("actionbarmessager");
        //noinspection ConstantConditions
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);
        new Metrics(this, 14275);
    }

    @Override
    public void onDisable() {
        messageManager.stop();
        userManager.saveAllData();
    }

    @Override
    public void saveDefaultConfig() {
        if (!new File(getDataFolder(), "Config.yml").exists()) {
            config = I18nHelper.getDefaultLocaledConfig();
            try {
                config.save(new File(getDataFolder(), "Config.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "Config.yml"));
        }
    }

    @Override
    public YamlConfiguration getConfig() {
        return config;
    }

    public static ActionBarMessager getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(new File(instance.getDataFolder() + "/Config.yml"));
        configManager = new ConfigManager(this);
        userManager.reload();
    }

    public static void info(String msg) {
        instance.getLogger().info(msg);
    }

    public static void warn(String msg) {
        instance.getLogger().warning(msg);
    }

    public static void error(String msg) {
        instance.getLogger().severe(msg);
    }

}
