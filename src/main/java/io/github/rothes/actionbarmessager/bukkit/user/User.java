package io.github.rothes.actionbarmessager.bukkit.user;

import io.github.rothes.actionbarmessager.bukkit.ActionBarMessager;
import io.github.rothes.actionbarmessager.bukkit.ConfigManager;
import io.github.rothes.actionbarmessager.bukkit.MessageEntry;
import me.clip.placeholderapi.libs.kyori.adventure.platform.facet.Facet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public final class User {

    private final Player player;
    private final YamlConfiguration data;
    private boolean receiveMessages;
    private long lastOtherActionBar;
    private String cache;
    private long cacheTime;
    private MessageEntry[] currentMessages;
    private int currentIndex;
    private long currentTimes;
    private long currentInterval;

    User(Player player) {
        this.player = player;
        updateWorldMessages();
        File file = new File(ActionBarMessager.getInstance().getDataFolder(), "Datas/Players/" + player.getUniqueId() + "/Data.yml");
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        data = YamlConfiguration.loadConfiguration(file);
        receiveMessages = data.getBoolean("Options.Receive-Messages", true);
    }

    public void saveData() {
        try {
            data.save(new File(ActionBarMessager.getInstance().getDataFolder(), "Datas/Players/" + player.getUniqueId() + "/Data.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isReceiveMessages() {
        return receiveMessages;
    }

    public void setReceiveMessages(boolean receiveMessages) {
        data.set("Options.Receive-Messages", receiveMessages);
        this.receiveMessages = receiveMessages;
    }

    public void setLastOtherActionBar(long lastOtherActionBar) {
        this.lastOtherActionBar = lastOtherActionBar;
    }

    public long getLastOtherActionBar() {
        return lastOtherActionBar;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getCache() {
        return cache;
    }

    public void setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public void updateWorldMessages() {
        ConfigManager configManager = ActionBarMessager.getInstance().getConfigManager();
        MessageEntry[] now = configManager.messages.getOrDefault(player.getWorld().getName(), configManager.defaultMessages);
        if (now != getCurrentMessages()) {
            setCurrentMessages(now);
            resetMessage();
        }
    }

    public MessageEntry[] getCurrentMessages() {
        return currentMessages;
    }

    public void setCurrentMessages(MessageEntry[] currentMessages) {
        this.currentMessages = currentMessages;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentTimes(long currentTimes) {
        this.currentTimes = currentTimes;
    }

    public long getCurrentTimes() {
        return currentTimes;
    }

    public void setCurrentInterval(long currentInterval) {
        this.currentInterval = currentInterval;
    }

    public long getCurrentInterval() {
        return currentInterval;
    }

    public void resetMessage() {
        setLastOtherActionBar(0);
        setCache(null);
        setCacheTime(0);
        setCurrentIndex(0);
        setCurrentTimes(0);
        setCurrentInterval(0);
    }

}
