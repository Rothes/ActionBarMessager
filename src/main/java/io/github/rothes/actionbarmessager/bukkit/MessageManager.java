package io.github.rothes.actionbarmessager.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.rothes.actionbarmessager.bukkit.user.User;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class MessageManager implements Listener {

    private final Cache<Object, Object> weakCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    private static final int SHOWING_INTERAL_KEEP = 2000;
    private final short SERVER_VERSION = Short.parseShort(Bukkit.getServer().getBukkitVersion().split("\\.")[1]);
    private ActionBarMessager plugin;

    void start(ActionBarMessager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (SERVER_VERSION >= 19) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.SYSTEM_CHAT) {
                public void onPacketSending(PacketEvent e) {
                    User user = getEventUser(e);
                    if (user == null) return;
                    PacketContainer packet = e.getPacket();
                    if (weakCache.asMap().containsKey(packet)) {
                        weakCache.invalidate(packet);
                        return;
                    }
                    if (packet.getIntegers().read(0) == EnumWrappers.ChatType.GAME_INFO.getId()) {
                        user.setLastOtherActionBar(System.currentTimeMillis());
                        user.setCache(null);
                    }
                }
            });
        }

        if (SERVER_VERSION >= 17) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.SET_ACTION_BAR_TEXT) {
                public void onPacketSending(PacketEvent e) {
                    User user = getEventUser(e);
                    if (user == null) return;
                    user.setLastOtherActionBar(System.currentTimeMillis());
                    user.setCache(null);
                }
            });

        } else {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.TITLE) {
                @Override
                public void onPacketSending(PacketEvent e) {
                    User user = getEventUser(e);
                    if (user == null) return;
                    PacketContainer packet = e.getPacket();
                    if (packet.getTitleActions().read(0) == EnumWrappers.TitleAction.ACTIONBAR) {
                        user.setLastOtherActionBar(System.currentTimeMillis());
                        user.setCache(null);
                    }
                }
            });
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent e) {
                User user = getEventUser(e);
                if (user == null) return;
                PacketContainer packet = e.getPacket();
                if (weakCache.asMap().containsKey(packet)) {
                    weakCache.invalidate(packet);
                    return;
                }
                if (packet.getChatTypes().read(0) == EnumWrappers.ChatType.GAME_INFO
                        || (packet.getBytes().size() >= 1 && packet.getBytes().read(0) == 2)) {
                    WrappedChatComponent read = packet.getChatComponents().read(0);
                    // If null, must not be messages from ActionBarMessager.
                    if (read == null) {
                        user.setLastOtherActionBar(System.currentTimeMillis());
                        user.setCache(null);
                        return;
                    }
                    user.setLastOtherActionBar(System.currentTimeMillis());
                    user.setCache(null);
                }
            }
        });


        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = plugin.getUserManager().getUser(player);
                if (user == null || !user.isReceiveMessages()) return;

                boolean send = true;
                user.setCurrentInterval(user.getCurrentInterval() + 1);
                if (plugin.getConfigManager().compromise
                        && System.currentTimeMillis() - user.getLastOtherActionBar() <= plugin.getConfigManager().compromiseInterval)
                    send = false;
                AbmMessage message = getMessages()[user.getCurrentIndex()];

                if (message.getInterval() > user.getCurrentInterval()) return;

                String toSend = PlaceholderAPI.setPlaceholders(player, message.getMessage());
                String cache = user.getCache();
                if (cache != null && cache.equals(toSend) && System.currentTimeMillis() - user.getCacheTime() < SHOWING_INTERAL_KEEP) {
                    send = false;
                }

                if (send) {
                    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
                    WrappedChatComponent component;
                    switch (message.getType()) {
                        case PLAIN:
                            component = WrappedChatComponent.fromLegacyText(toSend);
                            break;
                        case JSON:
                            component = WrappedChatComponent.fromJson(toSend);
                            break;
                        default:
                            throw new AssertionError();
                    }
                    PacketContainer packet;
                    if (SERVER_VERSION >= 19) {
                        packet = protocolManager.createPacket(PacketType.Play.Server.SYSTEM_CHAT);
                        packet.getIntegers().write(0, (int) EnumWrappers.ChatType.GAME_INFO.getId());
                    } else {
                        packet = protocolManager.createPacket(PacketType.Play.Server.CHAT);
                        packet.getChatTypes().write(0, EnumWrappers.ChatType.GAME_INFO);
                        if (packet.getBytes().size() == 1)
                            packet.getBytes().write(0, (byte) 2);
                    }
                    weakCache.put(packet, null);
                    packet.getChatComponents().write(0, component);
                    protocolManager.sendServerPacket(player, packet);
                    user.setCache(toSend);
                    user.setCacheTime(System.currentTimeMillis());
                }

                if (user.getCurrentTimes() == message.getTimes() - 1) {
                    int length = getMessages().length;
                    int i = user.getCurrentIndex();
                    for (int i1 = 0; i1 < length; i1++) {
                        if (++i >= length) {
                            i = 0;
                        }

                        if ((getMessages()[i].getPermission() == null || getMessages()[i].getPermission().isEmpty())
                                || player.hasPermission(getMessages()[i].getPermission())) {
                            user.setCurrentIndex(i);
                            break;
                        }
                    }
                    user.setCurrentTimes(0);

                } else {
                    user.setCurrentTimes(user.getCurrentTimes() + 1);
                }
                user.setCurrentInterval(0);
            }

        }, 0L, 1L);
    }

    private User getEventUser(@Nonnull PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        return player instanceof Player ? plugin.getUserManager().getUser(player) : null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getUserManager().removeUser(e.getPlayer());
    }

    private AbmMessage[] getMessages() {
        return plugin.getConfigManager().messages;
    }
}
