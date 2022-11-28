package io.github.rothes.actionbarmessager.bukkit;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MessageEntry {

    public enum Type {
        TEXT,
        JSON
    }

    @NotNull private final Type type;
    @NotNull private final String message;
    @Nullable private final String permission;
    private final long times;
    private final long interval;

    public MessageEntry(@NotNull Type type, @NotNull String message, @Nullable String permission, long times, long interval) {
        this.type = type;
        this.message = type == Type.TEXT ? ChatColor.translateAlternateColorCodes('&', message) : message;
        this.permission = permission;
        this.times = times;
        this.interval = interval;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @NotNull
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
        return "MessageEntry{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", permission='" + permission + '\'' +
                ", times=" + times +
                ", interval=" + interval +
                '}';
    }

}
