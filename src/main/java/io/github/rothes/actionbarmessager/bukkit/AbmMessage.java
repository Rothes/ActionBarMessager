package io.github.rothes.actionbarmessager.bukkit;

import org.bukkit.ChatColor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AbmMessage {

    public enum Type {
        PLAIN,
        JSON
    }

    @Nonnull private final Type type;
    @Nonnull private final String message;
    @Nullable private final String permission;
    private final long times;
    private final long interval;

    public AbmMessage(@Nonnull Type type, @Nonnull String message, @Nullable String permission, long times, long interval) {
        this.type = type;
        this.message = ChatColor.translateAlternateColorCodes('&', message);
        this.permission = permission;
        this.times = times;
        this.interval = interval;
    }

    @Nonnull
    public Type getType() {
        return type;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    public long getTimes() {
        return times;
    }

    public long getInterval() {
        return interval;
    }

    @Override
    public String toString() {
        return "AbmMessage{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", permission='" + permission + '\'' +
                ", times=" + times +
                ", interval=" + interval +
                '}';
    }

}
