package io.github.rothes.actionbarmessager.bukkit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class ConfigManager {

    public final ActionBarMessager plugin;
    public final HashMap<String, MessageEntry[]> messages = new HashMap<>();
    public MessageEntry[] defaultMessages;
    public final boolean compromise;
    public final long compromiseInterval;

    ConfigManager(ActionBarMessager plugin) {
        this.plugin = plugin;
        updateConfig();
        FileConfiguration config = plugin.getConfig();

        List<MessageEntry> messageList = new ArrayList<>();
        config.createSection("Options.Groups");
        for (String group : config.getConfigurationSection("Options.Groups").getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection("Options.Groups." + group);
            assert section != null;

            for (Map<?, ?> m : section.getMapList("Messages")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) m;
                MessageEntry.Type type = null;
                String typeString = String.valueOf(map.getOrDefault("Type", "Text"));
                for (MessageEntry.Type t : MessageEntry.Type.values()) {
                    if (t.name().equalsIgnoreCase(typeString)) {
                        type = t;
                    }
                }
                if (type == null) {
                    ActionBarMessager.warn(I18n.getLocaledMessage("Console-Sender.Messages.Initialize.Invalid-Type"));
                    type = MessageEntry.Type.TEXT;
                }

                Object message = map.get("Message");
                if (message == null) break;
                String permission = map.containsKey("Permission") ? String.valueOf(map.get("Permission")) : null;
                long interval = Long.parseLong(String.valueOf(map.getOrDefault("Interval", "1")));
                long times = Long.parseLong(String.valueOf(map.getOrDefault("Times", "20")));
                messageList.add(new MessageEntry(type, String.valueOf(message), permission, times, interval));
            }

            if (group.equals("Default") || group.equals("default")) {
                defaultMessages = messageList.toArray(new MessageEntry[0]);
            } else {
                MessageEntry[] msg = messageList.toArray(new MessageEntry[0]);
                for (String world : section.getStringList("Worlds")) {
                    MessageEntry[] put = messages.put(world, msg);
                    if (put != null) {
                        ActionBarMessager.warn(I18n.getLocaledMessage("Console-Sender.Messages.Initialize.Duplicate-World-Messages", world, group));
                    }
                }

            }

        }

        if (defaultMessages == null) {
            defaultMessages = new MessageEntry[0];
        }
        compromise = config.getBoolean("Options.Compromise.Enable", true);
        compromiseInterval = config.getLong("Options.Compromise.Interval", 2400);
    }

    private void updateConfig() {
        YamlConfiguration config = plugin.getConfig();
        int version = config.getInt("Config-Version", 1);
        if (version == 1) {
            ActionBarMessager.info(I18n.getLocaledMessage("Console-Sender.Messages.Initialize.Upgrading-Config", "v1", "v2"));
            File file = new File(plugin.getDataFolder(), "Config.yml.backup");
            try {
                file.createNewFile();
                plugin.getConfig().save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to backup Config.yml", e);
            }
            config.set("Config-Version", 2);
            List<Map<?, ?>> msg = config.getMapList("Options.Messages");
            for (Map<?, ?> m : msg) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) m;
                String type = (String) map.getOrDefault("Type", "Text");
                if (type.equals("Plain")) {
                    map.put("Type", "Text");
                }
            }
            config.set("Options.Messages", null);

            config.set("Options.Groups.Default.Messages", msg);
            config.set("Options.Groups.Default.Worlds", I18n.getDefaultLocaledConfig().getStringList("Options.Groups.Default.Messages"));
            try {
                config.save(new File(plugin.getDataFolder(), "Config.yml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
