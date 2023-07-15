package io.github.rothes.actionbarmessager.bukkit;

import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class AbmScheduler {

    public static void runUpdater(Runnable runnable) {
        if (ActionBarMessager.IS_FOLIA) {
            Bukkit.getAsyncScheduler().runAtFixedRate(ActionBarMessager.getInstance(), val -> runnable.run(), 0L, 1, TimeUnit.HOURS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(ActionBarMessager.getInstance(), runnable, 0L, 72000L);
        }
    }

    public static void runMessager(Runnable runnable) {
        if (ActionBarMessager.IS_FOLIA) {
            Bukkit.getAsyncScheduler().runAtFixedRate(ActionBarMessager.getInstance(), val -> runnable.run(), 0L, 50L, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(ActionBarMessager.getInstance(), runnable, 0L, 1L);
        }
    }

}
