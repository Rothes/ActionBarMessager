package io.github.rothes.actionbarmessager.bukkit;

import io.github.rothes.actionbarmessager.bukkit.exception.MissingInitialResourceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public final class I18n {

    private static ActionBarMessager plugin;
    private static String systemLocale = null;
    private static String locale = null;
    private static HashMap<String, String> localedMessages = null;
    private static final String[] replaceHolders = new String[10];

    public static void init(@NotNull ActionBarMessager plugin) {
        I18n.plugin = plugin;

        systemLocale = System.getProperty("user.language", Locale.getDefault().getLanguage());
        systemLocale += '-';
        systemLocale += System.getProperty("user.country", Locale.getDefault().getCountry());

        locale = plugin.getConfig().getString("Options.Locale", systemLocale);

        localedMessages = new HashMap<>();
        loadLocale();

        for (byte i = 0; i < 10; i ++) {
            replaceHolders[i] = "%" + i + '%';
        }

    }

    public static String getLocale() {
        return locale;
    }

    @NotNull
    public static YamlConfiguration getDefaultLocaledConfig() {
        InputStream resource = getLocaledResource("/Configs/Config.yml");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
    }

    @NotNull
    public static String getLocaledMessage(@NotNull String key, @NotNull String... replacements) {
        Validate.notNull(key, "Key cannot be null");
        Validate.notNull(replacements, "Replacements Array cannot be null");

        String result = localedMessages.getOrDefault(key, "§cMissing localization key: " + key);
        byte length = (byte) replacements.length;
        return length > 0 ? StringUtils.replaceEach(result, Arrays.copyOf(replaceHolders, length), replacements) : result;
    }

    @NotNull
    public static String getPrefixedLocaledMessage(@NotNull String key, @NotNull String... replacements) {
        Validate.notNull(key, "Key cannot be null");
        Validate.notNull(replacements, "Replacements Array cannot be null");

        return getLocaledMessage("Sender.Prefix") + getLocaledMessage(key, replacements);
    }

    @NotNull
    public static YamlConfiguration getDefaultLocale() {
        InputStream resource = getLocaledResource("/Locales/Locale.yml");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void loadLocale() {
        File localeFile = new File(plugin.getDataFolder() + "/Locale/" + locale + ".yml");
        YamlConfiguration locale;
        if (localeFile.exists()) {
            checkLocaleKeys(localeFile);
            locale = YamlConfiguration.loadConfiguration(localeFile);
        } else {
            locale = getDefaultLocale();
            try {
                localeFile.getParentFile().mkdirs();
                localeFile.createNewFile();
                locale.save(localeFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (String key : locale.getKeys(true)) {
            //noinspection ConstantConditions // translateAlternateColorCodes will do the check.
            localedMessages.put(key, ChatColor.translateAlternateColorCodes('&', locale.getString(key)));
        }
    }

    private static void checkLocaleKeys(@NotNull File localeFile) {
        YamlConfiguration defaultLocale = getDefaultLocale();
        YamlConfiguration locale = YamlConfiguration.loadConfiguration(localeFile);
        boolean checked = false;
        for (String key : defaultLocale.getKeys(true)) {
            if (!locale.contains(key)) {
                locale.set(key, defaultLocale.get(key));
                checked = true;
            }
        }
        if (checked) {
            try {
                locale.save(localeFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @NotNull
    private static InputStream getLocaledResource(@NotNull String file) {
        InputStream resource = plugin.getResource("Languages/" + locale + file);
        if (resource == null) {
            resource = plugin.getResource("Languages/" + systemLocale + file);
            if (resource == null) {
                resource = plugin.getResource("Languages/" + "en-US" + file);
                if (resource == null) {
                    throw new MissingInitialResourceException("Languages/" + "en-US" + file);
                }
            }
        }
        return resource;
    }


}
