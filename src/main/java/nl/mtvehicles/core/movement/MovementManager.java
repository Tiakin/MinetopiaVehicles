package nl.mtvehicles.core.movement;

import org.bukkit.entity.Player;

import static nl.mtvehicles.core.infrastructure.modules.VersionModule.getServerVersion;

/**
 * Movement selector depending on what version the server uses.
 */
public class MovementManager {
    /**
     * Select a packet handler for a player
     */
    public static void MovementSelector(Player player) {
        getServerVersion().getMovementHandler().handle(player);
    }
}
