package nl.mtvehicles.core.infrastructure.dependencies.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import nl.mtvehicles.core.Main;
import nl.mtvehicles.core.infrastructure.vehicle.Vehicle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Condition - Is player a rider of a vehicle")
@Description("Check if a player is a rider of an MTV Vehicle")
@Examples({
        "if player is a vehicle rider of {_car}:"
})
public class CondIsRider extends Condition {

    static {
        Skript.registerCondition(CondIsRider.class,
                "[player] %player% is [a] [mtv] vehicle rider of %object%",
                "[player] %player% (isn't|is not) [a] [mtv] vehicle rider of %object%"
        );
    }

    @SuppressWarnings("null")
    private Expression<Object> vehicle;

    private Expression<Player> player;

    @SuppressWarnings({"NullableProblems", "unchecked"})
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        setNegated(matchedPattern == 1);
        this.vehicle = (Expression<Object>) exprs[1];
        this.player = (Expression<Player>) exprs[0];
        return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean check(Event event) {
        if (!(vehicle.getSingle(event) instanceof Vehicle) || vehicle.getSingle(event) == null) {
            Main.logSevere("Skript error: Provided variable is not a vehicle (\"if %player% is the [mtv] vehicle owner of %vehicle%\").");
            return false;
        }

        boolean check = ((Vehicle) vehicle.getSingle(event)).canRide(player.getSingle(event));
        if (!isNegated()) return check;
        else return !check;
    }

    @Override
    public String toString(@Nullable Event e, boolean d) {
        final String neg = isNegated() ? " not" : "";
        return "Check if player is " + neg + " the owner of an MTV vehicle.";
    }

}
