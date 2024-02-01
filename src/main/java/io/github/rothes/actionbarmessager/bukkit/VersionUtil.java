package io.github.rothes.actionbarmessager.bukkit;

import org.bukkit.Bukkit;

public class VersionUtil {

    static short getServerVersionMajor() {
        String[] versionParts = Bukkit.getServer().getBukkitVersion().split("\\.");
        String majorVersionString = versionParts[1].split("-")[0];

        return Short.parseShort(majorVersionString);
    }

    static short getServerVersionMinor() {
        String[] versionParts = Bukkit.getServer().getBukkitVersion().split("\\.");

        if (versionParts.length > 2) {
            String minorVersionString = versionParts[2].split("-")[0];
            return Short.parseShort(minorVersionString);
        }

        return 0;
    }

}
