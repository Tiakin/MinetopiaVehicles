package nl.mtvehicles.core.infrastructure.dependencies.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import nl.mtvehicles.core.infrastructure.vehicle.Vehicle;
import nl.mtvehicles.core.infrastructure.vehicle.VehicleData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nl.mtvehicles.core.Main.isNotNull;

@Name("MTV Vehicle's vehicle max speed")
@Description("Get/set the vehicle's vehicle max speed")
@Examples({
        "set {_licensePlate} to {_car}'s vehicle max speed",
        "set mtv vehicle maximum speed of {_helicopter} to 3"
})
@Since("2.5.8")
public class ExprMaxSpeed extends SimplePropertyExpression<Vehicle, Double> {

    static {
        register(ExprMaxSpeed.class, Double.class, "[mtv] vehicle (max|maximum) speed", "mtvehicles");
    }

    @Override
    protected String getPropertyName() {
        return "[mtv] vehicle max speed";
    }

    @Override
    public @Nullable Double convert(Vehicle vehicle) {
        if (vehicle == null) return null;
        return VehicleData.getSpeed(VehicleData.DataSpeed.MAXSPEED, vehicle.getLicensePlate());
    }

    @Override
    public Class<? extends Double> getReturnType() {
        return Double.class;
    }

    @Override
    public @Nullable Class<?> @NotNull [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) return new Class[]{Double.class, Number.class};
        else return null;
    }

    @Override
    public void change(@NotNull Event event, @Nullable Object @NotNull [] delta, Changer.@NotNull ChangeMode changeMode) {
        Vehicle vehicle = getExpr().getSingle(event);

        if (!isNotNull(delta, delta[0], vehicle)) return;
        final String licensePlate = vehicle.getLicensePlate();
        double changeValue = ((Number) delta[0]).doubleValue();

        switch (changeMode) {
            case SET:
                VehicleData.setSpeed(VehicleData.DataSpeed.MAXSPEED, licensePlate, changeValue);
                break;
            case ADD:
                VehicleData.setSpeed(VehicleData.DataSpeed.MAXSPEED, licensePlate, VehicleData.getSpeed(VehicleData.DataSpeed.MAXSPEED, licensePlate) + changeValue);
                break;
            case REMOVE:
                VehicleData.setSpeed(VehicleData.DataSpeed.MAXSPEED, licensePlate, Math.max(0, VehicleData.getSpeed(VehicleData.DataSpeed.MAXSPEED, licensePlate) - changeValue));
                break;
            default:
                break;
        }
    }

}
