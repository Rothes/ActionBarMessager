package io.github.rothes.actionbarmessager.bukkit.user;

import org.bukkit.entity.Player;

import java.util.HashMap;

public final class UserManager {

    HashMap<Player, User> userMap = new HashMap<>();

    public User getUser(Player player) {
        return userMap.computeIfAbsent(player, User::new);
    }

    public User removeUser(Player player) {
        if (userMap.containsKey(player))
            userMap.get(player).saveData();
        return userMap.remove(player);
    }

    public void saveAllData() {
        for (User user : userMap.values()) {
            user.saveData();
        }
    }

    public void updateWorldAll() {
        for (User user : userMap.values()) {
            user.updateWorldMessages();
        }
    }

    public void reload() {
        for (User user : userMap.values()) {
            user.resetMessage();
        }

    }

}
