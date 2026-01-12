package nl.mtvehicles.core.movement;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import nl.mtvehicles.core.Main;
import nl.mtvehicles.core.infrastructure.annotations.VersionSpecific;
import nl.mtvehicles.core.infrastructure.enums.ServerVersion;
import nl.mtvehicles.core.movement.versions.VehicleMovement1_12;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static nl.mtvehicles.core.infrastructure.modules.VersionModule.getServerVersion;

/**
 * Packet handling system in different minecraft versions.
 */
@VersionSpecific
public class PacketHandler {

    /**
     * Generic packet handler using reflection.
     * @param player Player whose steering is being regarded
     * @param useRepeatingTask Whether to use repeating task or not (for 1.21.2+)
     * @param playerConnectionFieldName Name of the field to get PlayerConnection from EntityPlayer
     * @param networkManagerFieldName Name of the field to get NetworkManager from PlayerConnection
     * @param channelFieldName Name of the field to get Channel from NetworkManager
     */
    private static void setupPacketHandler(Player player, boolean useRepeatingTask, 
                                          String playerConnectionFieldName, 
                                          String networkManagerFieldName, 
                                          String channelFieldName) {
        
        final Class<?> packetClass;
        try {
            String version = getServerVersion().name();
            if (getServerVersion().isNewerOrEqualTo(ServerVersion.v1_17_R1)) {
                packetClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayInSteerVehicle");
            } else {
                packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayInSteerVehicle");
            }
        } catch (ClassNotFoundException e) {
            unexpectedException(e);
            return;
        }
        ChannelDuplexHandler channelDuplexHandler;
        if (useRepeatingTask) {
            // Repeating task pattern for 1.21.2+
            channelDuplexHandler = new ChannelDuplexHandler() {
                private Object lastPacket = null;
                private int taskId = -1;

                @Override
                public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
                    super.channelRead(channelHandlerContext, packet);
                    if (packetClass.isInstance(packet)) {
                        lastPacket = packet;

                        if (taskId != -1) {
                            Bukkit.getScheduler().cancelTask(taskId);
                        }

                        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, () -> {
                            if (player.isInsideVehicle()) {
                                VehicleMovement movement = new VehicleMovement();
                                movement.vehicleMovement(player, lastPacket);
                            } else {
                                Bukkit.getScheduler().cancelTask(taskId);
                                taskId = -1;
                            }
                        }, 0L, 1L);
                    }
                }
            };
        } else {
            channelDuplexHandler = new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
                    super.channelRead(channelHandlerContext, packet);
                    if (packetClass.isInstance(packet)) {
                        // Special handling for 1.12
                        if (getServerVersion().name().equals("v1_12_R1")) {
                            VehicleMovement1_12 movement = new VehicleMovement1_12();
                            movement.vehicleMovement(player, packet);
                        } else {
                            VehicleMovement movement = new VehicleMovement();
                            movement.vehicleMovement(player, packet);
                        }
                    }
                }
            };
        }
        Channel channel = null;
        try {
            // Build versioned CraftPlayer class name
            String version = getServerVersion().name();
            String craftPlayerClassName = "org.bukkit.craftbukkit." + version + ".entity.CraftPlayer";
            Class<?> craftPlayerClass = Class.forName(craftPlayerClassName);

            // Get EntityPlayer handle
            Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(player);

            // Get PlayerConnection
            Field playerConnectionField = entityPlayer.getClass().getDeclaredField(playerConnectionFieldName);
            playerConnectionField.setAccessible(true);
            Object playerConnection = playerConnectionField.get(entityPlayer);

            // Get NetworkManager
            Field networkManagerField;
            if (getServerVersion().isNewerOrEqualTo(ServerVersion.v1_20_R2)) {
                networkManagerField = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl")
                        .getDeclaredField(networkManagerFieldName);
            } else {
                networkManagerField = playerConnection.getClass().getDeclaredField(networkManagerFieldName);
            }
            networkManagerField.setAccessible(true);
            Object networkManager = networkManagerField.get(playerConnection);

            Field channelField = networkManager.getClass().getDeclaredField(channelFieldName);
            channelField.setAccessible(true);
            channel = (Channel) channelField.get(networkManager);

            channel.pipeline().addBefore("packet_handler", player.getName(), channelDuplexHandler);

        } catch (IllegalArgumentException e) {
            // In case of plugin reload, prevent duplicate handler name exception
            if (channel == null) {
                unexpectedException(e);
                return;
            }
            if (!channel.pipeline().names().contains(player.getName()))
                return;
            channel.pipeline().remove(player.getName());
            setupPacketHandler(player, useRepeatingTask, playerConnectionFieldName, networkManagerFieldName,
                    channelFieldName);
        } catch (Exception e) {
            unexpectedException(e);
        }
    }
    
    /**
     * Packet handler for vehicle steering in 1.21.11
     * 
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_21_R7(Player player) {
        setupPacketHandler(player, true, "g", "e", "k");
    }

    /**
     * Packet handler for vehicle steering in 1.21.9 and 1.21.10
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_21_R6(Player player) {
        setupPacketHandler(player, true, "g", "e", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.21.6, 1.21.7 and 1.21.8
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_21_R5(Player player) {
        setupPacketHandler(player, true, "g", "e", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.21.5
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_21_R4(Player player) {
        setupPacketHandler(player, true, "f", "e", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.21.4
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_21_R3(Player player) {
        setupPacketHandler(player, true, "f", "e", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.21.2 and 1.21.3
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_21_R2(Player player) {
        setupPacketHandler(player, true, "f", "e", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.21.1
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_21_R1(Player player) {
        setupPacketHandler(player, false, "c", "e", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.20.5 and 1.20.6
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_20_R4(Player player) {
        setupPacketHandler(player, false, "c", "e", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.20.3 and 1.20.4
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_20_R3(Player player) {
        setupPacketHandler(player, false, "c", "c", "n");
    }

    /**
     * Packet handler for vehicle steering in 1.20.2
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_20_R2(Player player) {
        setupPacketHandler(player, false, "c", "c", "n");
    }

    /**
      * Packet handler for vehicle steering in 1.20 and 1.20.1
      * @param player Player whose steering is being regarded
      */
    public static void movement_1_20_R1(Player player) {
        setupPacketHandler(player, false, "c", "h", "m");
    }

    /**
     * Packet handler for vehicle steering in 1.19.4
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_19_R3(Player player) {
        setupPacketHandler(player, false, "b", "h", "m");
    }

    /**
     * Packet handler for vehicle steering in 1.19.3
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_19_R2(Player player) {
        setupPacketHandler(player, false, "b", "b", "m");
    }

    /**
     * Packet handler for vehicle steering in 1.19-1.19.2
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_19_R1(Player player) {
        setupPacketHandler(player, false, "b", "b", "m");
    }

    /**
     * Packet handler for vehicle steering in 1.18.2
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_18_R2(Player player) {
        setupPacketHandler(player, false, "b", "a", "m");
    }

    /**
     * Packet handler for vehicle steering in 1.18 and 1.18.1
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_18_R1(Player player) {
        setupPacketHandler(player, false, "b", "a", "k");
    }

    /**
     * Packet handler for vehicle steering in 1.17 and 1.17.1
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_17_R1(Player player) {
        setupPacketHandler(player, false, "b", "a", "k");
    }

    /**
     * Packet handler for vehicle steering in 1.16.5 and 1.16.4 (NMS versions 1_16_R2 and 1_16_R1 are not supported)
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_16_R3(Player player) {
        setupPacketHandler(player, false, "playerConnection", "networkManager", "channel");
    }

    /**
     * Packet handler for vehicle steering in versions 1.15-1.15.2
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_15_R1(Player player) {
        setupPacketHandler(player, false, "playerConnection", "networkManager", "channel");
    }

    /**
     * Packet handler for vehicle steering in 1.13.2 and 1.13.1 (NMS version 1_13_R1 is not supported)
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_13_R2(Player player) {
        setupPacketHandler(player, false, "playerConnection", "networkManager", "channel");
    }

    /**
     * Packet handler for vehicle steering in versions 1.12-1.12.2
     * @param player Player whose steering is being regarded
     */
    public static void movement_1_12_R1(Player player) {
        setupPacketHandler(player, false, "playerConnection", "networkManager", "channel");
    }

    /**
     * Log an error message and disable the plugin, providing the exception.
     * @param e Exception
     */
    private static void unexpectedException(Exception e){
        Main.logSevere("An unexpected error occurred, disabling the plugin... Check the exception log:");
        e.printStackTrace();
        Main.disablePlugin();
    }

    /**
     * Check whether a given object is a valid steering packet (PacketPlayInSteerVehicle). If not, return false and send an error to the console.
     *
     * @param object Checked object (likely a packet)
     * @return True if the given object is an instance of the steering packet (PacketPlayInSteerVehicle).
     */
    @VersionSpecific
    public static boolean isObjectPacket(Object object) {
        final String errorMessage = "An unexpected error occurred (given object is not a valid steering packet). Try reinstalling the plugin or contact the developer: https://discord.gg/vehicle";

        if (getServerVersion() == ServerVersion.v1_12_R1) {
            if (!(object instanceof net.minecraft.server.v1_12_R1.PacketPlayInSteerVehicle)) {
                Main.logSevere(errorMessage);
                return false;
            }
        } else if (getServerVersion() == ServerVersion.v1_13_R2) {
            if (!(object instanceof net.minecraft.server.v1_13_R2.PacketPlayInSteerVehicle)){
                Main.logSevere(errorMessage);
                return false;
            }
        } else if (getServerVersion() == ServerVersion.v1_15_R1) {
            if (!(object instanceof net.minecraft.server.v1_15_R1.PacketPlayInSteerVehicle)){
                Main.logSevere(errorMessage);
                return false;
            }
        } else if (getServerVersion() == ServerVersion.v1_16_R3) {
            if (!(object instanceof net.minecraft.server.v1_16_R3.PacketPlayInSteerVehicle)){
                Main.logSevere(errorMessage);
                return false;
            }
        } else if (getServerVersion().isNewerOrEqualTo(ServerVersion.v1_17_R1)) {
            if (!(object instanceof net.minecraft.network.protocol.game.PacketPlayInSteerVehicle)){
                Main.logSevere(errorMessage);
                return false;
            }
        }
        return true;
    }

}
