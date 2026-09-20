package nl.mtvehicles.core.infrastructure.enums;

/**
 * How vehicle skin values are applied to and read from items.
 */
public enum ItemSkinMode {
    /**
     * Legacy item damage only ({@link org.bukkit.inventory.meta.Damageable#setDamage(int)}).
     */
    LEGACY,
    /**
     * CustomModelData only ({@link org.bukkit.inventory.meta.ItemMeta#setCustomModelData(Integer)}).
     */
    CUSTOM_MODEL_DATA,
    /**
     * Both damage and CustomModelData (recommended during migration).
     */
    DUAL;

    public boolean usesLegacy() {
        return this == LEGACY || this == DUAL;
    }

    public boolean usesCustomModelData() {
        return this == CUSTOM_MODEL_DATA || this == DUAL;
    }
}
