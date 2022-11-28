package io.github.rothes.actionbarmessager.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class Listeners implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        ActionBarMessager.getInstance().getUserManager().getUser(event.getPlayer()).updateWorldMessages();
    }

}
