package io.github.rothes.actionbarmessager.bukkit;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConfigManager {

    public final AbmMessage[] messages;
    public final boolean compromise;
    public final long compromiseInterval;

    ConfigManager(ActionBarMessager plugin) {
        FileConfiguration config = plugin.getConfig();

        List<AbmMessage> messageList = new ArrayList<>();
        for (Map<?, ?> m : config.getMapList("Options.Messages")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) m;
            AbmMessage.Type type = null;
            String typeString = String.valueOf(map.getOrDefault("Type", "Plain"));
            for (AbmMessage.Type t : AbmMessage.Type.values()) {
                if (t.name().equalsIgnoreCase(typeString)) {
                    type = t;
                }
            }
            if (type == null) {
                ActionBarMessager.warn(I18nHelper.getLocaledMessage("Console-Sender.Messages.Initialize.Invalid-Type"));
                type = AbmMessage.Type.PLAIN;
            }

            Object message = map.get("Message");
            if (message == null) break;
            String permission = map.containsKey("Permission") ? String.valueOf(map.get("Permission")) : null;
            long interval = Long.parseLong(String.valueOf(map.getOrDefault("Interval", "1")));
            long times = Long.parseLong(String.valueOf(map.getOrDefault("Times", "20")));
            messageList.add(new AbmMessage(type, String.valueOf(message), permission, times, interval));
        }
        messages = messageList.toArray(new AbmMessage[0]);

        compromise = config.getBoolean("Options.Compromise.Enable", true);
        compromiseInterval = config.getLong("Options.Compromise.Interval", 2400);
    }

}
