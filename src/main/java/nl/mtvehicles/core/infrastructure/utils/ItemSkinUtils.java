package nl.mtvehicles.core.infrastructure.utils;

import nl.mtvehicles.core.infrastructure.dataconfig.DefaultConfig;
import nl.mtvehicles.core.infrastructure.enums.ItemSkinMode;
import nl.mtvehicles.core.infrastructure.enums.ServerVersion;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

import static nl.mtvehicles.core.infrastructure.modules.VersionModule.getServerVersion;

/**
 * Applies and reads vehicle skin values on items (legacy damage and/or CustomModelData).
 */
public final class ItemSkinUtils {

    private ItemSkinUtils() {}

    public static boolean supportsCustomModelData() {
        return getServerVersion().isNewerOrEqualTo(ServerVersion.v1_14_R1);
    }

    public static ItemSkinMode getMode() {
        Object value = ConfigModule.defaultConfig.get(DefaultConfig.Option.ITEM_SKIN_MODE);
        if (value == null) return ItemSkinMode.DUAL;

        switch (value.toString().toLowerCase(Locale.ROOT)) {
            case "legacy":
            case "damage":
                return ItemSkinMode.LEGACY;
            case "custommodeldata":
            case "cmd":
            case "modeldata":
                return ItemSkinMode.CUSTOM_MODEL_DATA;
            default:
                return ItemSkinMode.DUAL;
        }
    }

    public static void applySkinValue(ItemStack item, int value) {
        if (getServerVersion() == ServerVersion.v1_12_R1) {
            item.setDurability((short) value);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        applySkinValue(meta, value);
        item.setItemMeta(meta);
    }

    public static void applySkinValue(ItemMeta meta, int value) {
        ItemSkinMode mode = getMode();

        if (mode.usesLegacy() && meta instanceof Damageable) {
            ((Damageable) meta).setDamage(value);
        }
        if (mode.usesCustomModelData() && supportsCustomModelData()) {
            meta.setCustomModelData(value);
        }
    }

    /**
     * Read the skin value from an item. Tries CustomModelData first, then legacy damage.
     */
    public static int getSkinValue(ItemStack item) {
        if (getServerVersion() == ServerVersion.v1_12_R1) {
            return item.getDurability();
        }

        if (!item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        if (supportsCustomModelData() && meta.hasCustomModelData()) {
            return meta.getCustomModelData();
        }
        if (meta instanceof Damageable) {
            return ((Damageable) meta).getDamage();
        }
        return 0;
    }
}
