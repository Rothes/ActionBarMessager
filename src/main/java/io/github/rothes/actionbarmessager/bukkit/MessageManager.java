package io.github.rothes.actionbarmessager.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.github.rothes.actionbarmessager.bukkit.user.User;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class MessageManager implements Listener {

    private static final int SHOWING_INTERNAL_KEEP = 2000;
    private final short SERVER_VERSION = Short.parseShort(Bukkit.getServer().getBukkitVersion().split("\\.")[1].split("-")[0]);
    private final short SERVER_VERSION_MINOR = Short.parseShort(Bukkit.getServer().getBukkitVersion().split("\\.")[2].split("-")[0]);
    private ActionBarMessager plugin;

    void start(ActionBarMessager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (SERVER_VERSION >= 19) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.SYSTEM_CHAT) {
                public void onPacketSending(PacketEvent e) {
                    User user = getEventUser(e);
                    if (user == null) return;
                    PacketContainer packet = e.getPacket();
                    if (packet.getMeta("abm_filtered_packet").isPresent()) {
                        return;
                    }
                    StructureModifier<Integer> integers = packet.getIntegers();
                    if (integers.size() == 1) {
                        if (integers.read(0) == EnumWrappers.ChatType.GAME_INFO.getId()) {
                            user.setLastOtherActionBar(System.currentTimeMillis());
                            user.setCache(null);
                        }
                    } else if (packet.getBooleans().read(0)){
                        user.setLastOtherActionBar(System.currentTimeMillis());
                        user.setCache(null);
                    }
                }
            });
        }

        if (SERVER_VERSION >= 17) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.SET_ACTION_BAR_TEXT) {
                public void onPacketSending(PacketEvent e) {
                    User user = getEventUser(e);
                    if (user == null) return;
                    user.setLastOtherActionBar(System.currentTimeMillis());
                    user.setCache(null);
                }
            });

        } else {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.TITLE) {
                @Override
                public void onPacketSending(PacketEvent e) {
                    User user = getEventUser(e);
                    if (user == null) return;
                    PacketContainer packet = e.getPacket();
                    if (packet.getMeta("abm_filtered_packet").isPresent()) {
                        return;
                    }
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
                if (packet.getMeta("abm_filtered_packet").isPresent()) {
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


        Runnable runnable = () -> {
            try {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    User user = plugin.getUserManager().getUser(player);
                    if (user == null || !user.isReceiveMessages()) continue;

                    MessageEntry[] messages = user.getCurrentMessages();
                    if (messages.length == 0) continue;

                    user.setCurrentInterval(user.getCurrentInterval() + 1);

                    MessageEntry message = null;

                    if (!(plugin.getConfigManager().compromise
                            && System.currentTimeMillis() - user.getLastOtherActionBar() <= plugin.getConfigManager().compromiseInterval)) {
                        message = messages[user.getCurrentIndex()];

                        if (message.getInterval() > user.getCurrentInterval()) continue;

                        if (checkPermission(player, message.getPermission())) {
                            String toSend = message.getMessage();
                            if (ActionBarMessager.hasPapi) {
                                toSend = PlaceholderAPI.setPlaceholders(player, toSend);
                            }
                            String cache = user.getCache();
                            if (cache == null || !cache.equals(toSend) || System.currentTimeMillis() - user.getCacheTime() >= SHOWING_INTERNAL_KEEP) {
                                ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
                                WrappedChatComponent component;
                                switch (message.getType()) {
                                    case TEXT:
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
                                    StructureModifier<Integer> integers = packet.getIntegers();
                                    if (integers.size() == 1) {
                                        integers.write(0, (int) EnumWrappers.ChatType.GAME_INFO.getId());
                                    } else {
                                        packet.getBooleans().write(0, true);
                                    }
                                    if (SERVER_VERSION >= 20 && SERVER_VERSION_MINOR >= 3) {
                                        packet.getChatComponents().write(0, component);
                                    } else {
                                        packet.getStrings().write(0, component.getJson());
                                    }
                                } else if (SERVER_VERSION > 16 || SERVER_VERSION < 11) {
                                    packet = protocolManager.createPacket(PacketType.Play.Server.CHAT);
                                    packet.getChatTypes().write(0, EnumWrappers.ChatType.GAME_INFO);
                                    if (packet.getBytes().size() == 1)
                                        packet.getBytes().write(0, (byte) 2);
                                    packet.getChatComponents().write(0, component);
                                } else {
                                    // Have to use this packet, for json color rendering.
                                    packet = protocolManager.createPacket(PacketType.Play.Server.TITLE);
                                    packet.getTitleActions().write(0, EnumWrappers.TitleAction.ACTIONBAR);
                                    packet.getChatComponents().write(0, component);
                                }
                                packet.setMeta("abm_filtered_packet", true);
                                protocolManager.sendServerPacket(player, packet);
                                user.setCache(toSend);
                                user.setCacheTime(System.currentTimeMillis());
                            }
                        } else {
                            // skip
                            message = null;
                        }
                    }

                    if (message == null || user.getCurrentTimes() == message.getTimes() - 1) {
                        int length = messages.length;
                        int i = user.getCurrentIndex();
                        for (int i1 = 0; i1 < length; i1++) {
                            if (++i >= length) {
                                i = 0;
                            }

                            MessageEntry entry = messages[i];
                            if (checkPermission(player, entry.getPermission())) {
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
            } catch (Throwable t) {
                t.printStackTrace();
            }

        };
        AbmScheduler.runMessager(runnable);
    }

    private boolean checkPermission(Player player, String permission) {
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    void stop() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
    }

    private User getEventUser(@NotNull PacketEvent packetEvent) {
        return packetEvent.isPlayerTemporary() ? null : plugin.getUserManager().getUser(packetEvent.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getUserManager().removeUser(e.getPlayer());
    }

}
