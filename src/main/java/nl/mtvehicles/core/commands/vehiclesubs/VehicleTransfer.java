package nl.mtvehicles.core.commands.vehiclesubs;

import nl.mtvehicles.core.infrastructure.dataconfig.VehicleDataConfig;
import nl.mtvehicles.core.infrastructure.enums.Message;
import nl.mtvehicles.core.infrastructure.models.MTVSubCommand;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import nl.mtvehicles.core.infrastructure.vehicle.VehicleUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

/**
 * <b>/vehicle transfer %player%</b> - transfer held vehicle to another player (owner only).
 */
public class VehicleTransfer extends MTVSubCommand {
    public VehicleTransfer() {
        this.setPlayerCommand(true);
    }

    @Override
    public boolean execute() {
        if (!isHoldingVehicle()) return true;

        if (arguments.length != 2) {
            sendMessage(Message.USE_TRANSFER);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        String licensePlate = VehicleUtils.getLicensePlate(item);

        if (!VehicleUtils.existsByLicensePlate(licensePlate)) {
            sendMessage(Message.VEHICLE_NOT_FOUND);
            return true;
        }

        Player argPlayer = Bukkit.getPlayer(arguments[1]);
        if (argPlayer == null) {
            sendMessage(Message.PLAYER_NOT_FOUND);
            return true;
        }

        UUID ownerUUID = VehicleUtils.getOwnerUUID(licensePlate);
        if (ownerUUID == null) {
            sendMessage(Message.VEHICLE_NOT_FOUND);
            return true;
        }

        if (!ownerUUID.equals(player.getUniqueId())) {
            sendMessage(Message.NOT_YOUR_CAR);
            return true;
        }

        ConfigModule.vehicleDataConfig.set(licensePlate, VehicleDataConfig.Option.OWNER, argPlayer.getUniqueId().toString());
        ConfigModule.vehicleDataConfig.set(licensePlate, VehicleDataConfig.Option.RIDERS, new ArrayList<>());
        ConfigModule.vehicleDataConfig.set(licensePlate, VehicleDataConfig.Option.MEMBERS, new ArrayList<>());

        sendMessage(Message.MEMBER_CHANGE);

        return true;
    }
}
