package nl.mtvehicles.core.commands.vehiclesubs;

import nl.mtvehicles.core.events.inventory.VehicleMenuOpenEvent;
import nl.mtvehicles.core.infrastructure.dataconfig.DefaultConfig;
import nl.mtvehicles.core.infrastructure.enums.InventoryTitle;
import nl.mtvehicles.core.infrastructure.enums.Message;
import nl.mtvehicles.core.infrastructure.utils.ItemFactory;
import nl.mtvehicles.core.infrastructure.utils.ItemUtils;
import nl.mtvehicles.core.infrastructure.utils.MenuUtils;
import nl.mtvehicles.core.infrastructure.models.MTVSubCommand;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>/vehicle menu</b> - open a GUI menu of all the vehicles.
 */
public class VehicleMenu extends MTVSubCommand {
    public static HashMap<UUID, Inventory> beginMenu = new HashMap<>();
    public static HashMap<UUID, Integer> vehicleMenuPage = new HashMap<>();

    public VehicleMenu() {
        this.setPlayerCommand(true);
    }

    @Override
    public boolean execute() {
        if (!checkPermission("mtvehicles.menu")) return true;

        sendMessage(Message.MENU_OPEN);

        VehicleMenuOpenEvent api = new VehicleMenuOpenEvent(player);
        api.call();
        if (api.isCancelled()) return true;

        openVehicleMenu(player, 1);

        return true;
    }

    /**
     * Open the vehicle menu on a specific page.
     *
     * @param player Player opening the menu.
     * @param page   The page number (1-indexed).
     */
    public static void openVehicleMenu(Player player, int page) {
        int menuRows = (int) ConfigModule.defaultConfig.get(DefaultConfig.Option.VEHICLE_MENU_SIZE);
        final int menuSize = (menuRows >= 3 && menuRows <= 6) ? menuRows * 9 : 27;

        Inventory inv = Bukkit.createInventory(null, menuSize, InventoryTitle.VEHICLE_MENU.getStringTitle());

        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        int totalVehicles = vehicles.size();

        boolean hasPages = totalVehicles > menuSize;
        int itemsPerPage = hasPages ? (menuSize - 9) : menuSize;

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, totalVehicles);

        for (int i = start; i < end; i++) {
            Map<?, ?> vehicle = vehicles.get(i);
            int itemDamage = (Integer) vehicle.get("itemDamage");
            String name = (String) vehicle.get("name");
            String skinItem = (String) vehicle.get("skinItem");
            ItemStack itemStack = ItemUtils.getMenuVehicle(ItemUtils.getMaterial(skinItem), itemDamage, name);

            if (vehicle.get("nbtValue") == null) {
                inv.addItem(itemStack);
                continue;
            }
            inv.addItem(new ItemFactory(itemStack).setNBT((String) vehicle.get("nbtKey"), (String) vehicle.get("nbtValue")).toItemStack());
        }

        if (hasPages) {
            inv.setItem(menuSize - 9, ItemUtils.getMenuItem(Material.SPECTRAL_ARROW, 1, "&c" + ConfigModule.messagesConfig.getMessage(Message.PREVIOUS_PAGE), "&c"));
            inv.setItem(menuSize - 1, ItemUtils.getMenuItem(Material.SPECTRAL_ARROW, 1, "&c" + ConfigModule.messagesConfig.getMessage(Message.NEXT_PAGE), "&c"));
            for (int i = menuSize - 8; i <= menuSize - 2; i++) {
                if (i == menuSize - 5) {
                    inv.setItem(i, MenuUtils.getCloseItem());
                } else {
                    inv.setItem(i, ItemUtils.getMenuItem(ItemUtils.getStainedGlassPane(), 1, "&c", "&c"));
                }
            }
        }

        vehicleMenuPage.put(player.getUniqueId(), page);
        beginMenu.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }
}
