package nl.mtvehicles.core.commands.vehiclesubs;

import nl.mtvehicles.core.infrastructure.enums.Message;
import nl.mtvehicles.core.infrastructure.models.MTVSubCommand;
import nl.mtvehicles.core.infrastructure.vehicle.Vehicle;
import nl.mtvehicles.core.infrastructure.vehicle.VehicleUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

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

        Vehicle vehicle = VehicleUtils.getVehicle(licensePlate);
        assert vehicle != null;

        if (!vehicle.isOwner(player)) {
            sendMessage(Message.NOT_YOUR_CAR);
            return true;
        }

        vehicle.setRiders(new ArrayList<>());
        vehicle.setMembers(new ArrayList<>());
        vehicle.setOwner(argPlayer.getUniqueId());
        vehicle.save();

        sendMessage(Message.MEMBER_CHANGE);

        return true;
    }
}
