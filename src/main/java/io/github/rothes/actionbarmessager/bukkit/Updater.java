package io.github.rothes.actionbarmessager.bukkit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Updater {

    private static final String VERSION_CHANNEL = "Stable";
    private static final int VERSION_NUMBER = 14;
    private final HashMap<String, Integer> msgTimesMap = new HashMap<>();

    public void start() {
        Runnable runnable = () -> {
            try {
                checkJson(getJson((I18n.getLocale().equals("zh-CN") ? "mirror.ghproxy.com/https://" : "") + "raw.githubusercontent.com"));
            } catch (IllegalStateException | NullPointerException ignored) {
//                warn("§c无法正常解析版本信息 Json, 请更新您的插件至最新版本: " + e);
            }
        };
        AbmScheduler.runUpdater(runnable);
    }

    private String getJson(String link) {
        try (
                InputStream stream = new URL("https://" + link + "/Rothes/ActionBarMessager/master/Version%20Infos.json").openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
        ){
            StringBuilder jsonBuilder = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                jsonBuilder.append(line).append("\n");
            }
            return jsonBuilder.toString();
        } catch (IOException e) {
            // e.printStackTrace();
            if (!link.equals("raw.githubusercontent.com")) {
                return getJson("raw.githubusercontent.com");
            }
            return null;
        }
    }

    private void checkJson(String json) {
        JsonElement element = new JsonParser().parse(json);
        JsonObject root = element.getAsJsonObject();
        JsonObject channels = root.getAsJsonObject("Version_Channels");
        if (channels.has(VERSION_CHANNEL)) {
            JsonObject channel = channels.getAsJsonObject(VERSION_CHANNEL);
            if (channel.has("Message")
                    && Integer.parseInt(channel.getAsJsonPrimitive("Latest_Version_Number").getAsString()) > VERSION_NUMBER) {
                sendJsonMessage(channel, "updater");
            }
        } else {
            ActionBarMessager.warn(I18n.getLocaledMessage("Console-Sender.Messages.Updater.Invalid-Channel", VERSION_CHANNEL));
        }

        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("Version_Actions").entrySet()) {
            String[] split = entry.getKey().split("-");
            if (Integer.parseInt(split[1]) > VERSION_NUMBER
                    && VERSION_NUMBER > Integer.parseInt(split[0])) {
                JsonObject message = (JsonObject) entry.getValue();
                if (message.has("Message"))
                    sendJsonMessage(message, entry.getKey());
            }
        }

    }

    private void sendJsonMessage(JsonObject json, String id) {
        JsonObject msgJson = json.getAsJsonObject("Message");
        String msg = msgJson.get("zh-CN").getAsString();

        int msgTimes = json.has("Message_Times") ? json.get("Message_Times").getAsInt() : -1;
        int curTimes = msgTimesMap.get(id) == null? 0 : msgTimesMap.get(id);

        if (msgTimes == -1 || curTimes < msgTimes) {

            String logLevel = json.has("Log_Level") ? json.get("Log_Level").getAsString() : "default maybe";

            for (String s : msg.split("\n")) {
                switch (logLevel) {
                    case "Error":
                        ActionBarMessager.error(s);
                        break;
                    case "Warn":
                        ActionBarMessager.warn(s);
                        break;
                    case "Info":
                    default:
                        ActionBarMessager.info(s);
                        break;
                }
            }
            msgTimesMap.put(id, ++curTimes);
        }

        for (JsonElement action : json.getAsJsonArray("Actions")) {
            if (action.getAsString().equals("Prohibit")) {
                Bukkit.getPluginManager().disablePlugin(ActionBarMessager.getInstance());
            }
        }

    }

}
