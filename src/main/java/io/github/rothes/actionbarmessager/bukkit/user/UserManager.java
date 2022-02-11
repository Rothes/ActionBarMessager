package io.github.rothes.actionbarmessager.bukkit.user;

import org.bukkit.entity.Player;

import java.util.HashMap;

public final class UserManager {

    HashMap<Player, User> userMap = new HashMap<>();

    public User getUser(Player player) {
        return userMap.computeIfAbsent(player, User::new);
    }

    public User removeUser(Player player) {
        return userMap.remove(player);
    }

    public void reload() {
        for (User user : userMap.values()) {
            user.setLastOtherActionBar(0);
            user.setCache(null);
            user.setCacheTime(0);
            user.setCurrentIndex(0);
            user.setCurrentTimes(0);
            user.setCurrentInterval(0);
        }

    }

}
