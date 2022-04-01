package nl.mtvehicles.core.movement;

import nl.mtvehicles.core.Main;
import nl.mtvehicles.core.infrastructure.annotations.ToDo;
import nl.mtvehicles.core.infrastructure.dataconfig.DefaultConfig;
import nl.mtvehicles.core.infrastructure.dataconfig.VehicleDataConfig;
import nl.mtvehicles.core.infrastructure.enums.VehicleType;
import nl.mtvehicles.core.infrastructure.helpers.BossBarUtils;
import nl.mtvehicles.core.infrastructure.helpers.VehicleData;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static nl.mtvehicles.core.infrastructure.modules.VersionModule.getServerVersion;
import static nl.mtvehicles.core.movement.PacketHandler.isObjectPacket;

public class VehicleMovement {
    protected Player player;
    protected String license;

    public void vehicleMovement(Player player, Object packet) {

        //Do not continue if the correct packet has not been given
        if (!isObjectPacket(packet)) return;

        this.player = player;
        long lastUsed = 0L;

        if (player.getVehicle() == null) return;
        final Entity vehicle = player.getVehicle();

        if (!(vehicle instanceof ArmorStand)) return;
        if (vehicle.getCustomName() == null) return;

        if (vehicle.getCustomName().replace("MTVEHICLES_MAINSEAT_", "").isEmpty()) return; //Not sure what this line is supposed to be doing here but I'm keeping it, just in case
        this.license = vehicle.getCustomName().replace("MTVEHICLES_MAINSEAT_", "");

        if (VehicleData.autostand.get("MTVEHICLES_MAIN_" + license) == null) return;

        if (VehicleData.speed.get(license) == null) {
            VehicleData.speed.put(license, 0.0);
            return;
        }

        final VehicleType vehicleType = VehicleType.valueOf(VehicleData.type.get(license));
        if (vehicleType == null) return;

        boolean falling = false;
        boolean extremeFalling = false;
        if (VehicleData.fuel.get(license) < 1) {
            BossBarUtils.setBossBarValue(0 / 100.0D, license);
            if (vehicleType.canFly()) {
                falling = true;
                extremeFalling = vehicleType.isHelicopter() && (boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.EXTREME_HELICOPTER_FALL);
            }
            else return;
        }

        BossBarUtils.setBossBarValue(VehicleData.fuel.get(license) / 100.0D, license);

        final ArmorStand standMain = VehicleData.autostand.get("MTVEHICLES_MAIN_" + license);
        final ArmorStand standSkin = VehicleData.autostand.get("MTVEHICLES_SKIN_" + license);
        final ArmorStand standMainSeat = VehicleData.autostand.get("MTVEHICLES_MAINSEAT_" + license);
        final ArmorStand standRotors = VehicleData.autostand.get("MTVEHICLES_WIEKENS_" + license);

        if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.DAMAGE_ENABLED) && ConfigModule.vehicleDataConfig.getHealth(license) == 0) { //Vehicle is broken
            standMain.getWorld().spawnParticle(Particle.SMOKE_NORMAL, standMain.getLocation(), 2);
            return;
        }

        schedulerRun(() -> standSkin.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw(), standMain.getLocation().getPitch())));

        final int RotationSpeed = VehicleData.RotationSpeed.get(license);
        final double MaxSpeed = VehicleData.MaxSpeed.get(license);
        final double AccelerationSpeed = VehicleData.AccelerationSpeed.get(license);
        final double BrakingSpeed = VehicleData.BrakingSpeed.get(license);
        final double MaxSpeedBackwards = VehicleData.MaxSpeedBackwards.get(license);
        final double FrictionSpeed = VehicleData.FrictionSpeed.get(license);

        boolean space = !falling && steerIsJumping(packet);
        updateStand(standMain, space, extremeFalling);
        if (!vehicleType.canFly()) slabCheck(standMain);
        mainSeat(standMain, standMainSeat);

        if (VehicleData.seatsize.get(license + "addon") != null) {
            for (int i = 1; i <= VehicleData.seatsize.get(license + "addon"); i++) {
                ArmorStand standAddon = VehicleData.autostand.get("MTVEHICLES_ADDON" + i + "_" + license);
                schedulerRun(() -> standAddon.teleport(standMain.getLocation()));
            }
        }

        if (vehicleType.isHelicopter()) rotors(standMain, standRotors, license, falling, extremeFalling);

        // Horn
        if (ConfigModule.vehicleDataConfig.isHornEnabled(license) && steerIsJumping(packet) && !falling) {
            if (VehicleData.lastUsage.containsKey(player.getName())) lastUsed = VehicleData.lastUsage.get(player.getName());

            if (System.currentTimeMillis() - lastUsed >= Long.parseLong(ConfigModule.defaultConfig.get(DefaultConfig.Option.HORN_COOLDOWN).toString()) * 1000L) {
                standMain.getWorld().playSound(standMain.getLocation(), Objects.requireNonNull(ConfigModule.defaultConfig.get(DefaultConfig.Option.HORN_TYPE).toString()), 0.9f, 1f);
                VehicleData.lastUsage.put(player.getName(), System.currentTimeMillis());
            }
        }

        //Tank
        if (vehicleType.isTank() && steerIsJumping(packet)) {
            if (VehicleData.lastUsage.containsKey(player.getName())) lastUsed = VehicleData.lastUsage.get(player.getName());

            if (System.currentTimeMillis() - lastUsed >= Long.parseLong(ConfigModule.defaultConfig.get(DefaultConfig.Option.TANK_COOLDOWN).toString()) * 1000L) {
                standMain.getWorld().playEffect(standMain.getLocation(), Effect.BLAZE_SHOOT, 1, 1);
                standMain.getWorld().playEffect(standMain.getLocation(), Effect.GHAST_SHOOT, 1, 1);
                standMain.getWorld().playEffect(standMain.getLocation(), Effect.WITHER_BREAK_BLOCK, 1, 1);
                double xOffset = 4;
                double yOffset = 1.6;
                double zOffset = 0;
                Location locvp = standMain.getLocation().clone();
                Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
                float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
                float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
                Location loc = new Location(standMain.getWorld(), xvp, standMain.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
                spawnParticles(standMain, loc);
                spawnTNT(standMain, loc);
                VehicleData.lastUsage.put(player.getName(), System.currentTimeMillis());
            }
        }

        final float xxa = (falling && vehicleType.isHelicopter()) ? 0.0f : steerGetXxa(packet);
        final float zza = (falling && vehicleType.isHelicopter()) ? 0.0f : steerGetZza(packet);
        final Location locBelow = new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY() - 0.2, standMain.getLocation().getZ(), standMain.getLocation().getYaw(), standMain.getLocation().getPitch());
        if (xxa > 0.0) {
            if (!vehicleType.isHelicopter() || locBelow.getBlock().getType().equals(Material.AIR)) { //Do not rotate if you're in a helicopter on ground
                final int rotation = (VehicleData.speed.get(license) < 0.1) ? RotationSpeed / 3 : RotationSpeed;
                schedulerRun(() -> {
                    if (getServerVersion().is1_12()) {
                        standMain.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw() - rotation, standMain.getLocation().getPitch()));
                        standMainSeat.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw() - rotation, standMain.getLocation().getPitch()));
                        standSkin.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw() - rotation, standMain.getLocation().getPitch()));
                    } else {
                        standMain.setRotation(standMain.getLocation().getYaw() - rotation, standMain.getLocation().getPitch());
                        standMainSeat.setRotation(standMain.getLocation().getYaw() - rotation, standMain.getLocation().getPitch());
                        standSkin.setRotation(standMain.getLocation().getYaw() - rotation, standMain.getLocation().getPitch());
                    }
                });
            }
        } else if (xxa < 0.0) {
            if (!vehicleType.isHelicopter() || locBelow.getBlock().getType().equals(Material.AIR)) { //Do not rotate if you're in a helicopter on ground
                final int rotation = (VehicleData.speed.get(license) < 0.1) ? RotationSpeed / 3 : RotationSpeed;
                schedulerRun(() -> {
                    if (getServerVersion().is1_12()) {
                        standMain.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw() + rotation, standMain.getLocation().getPitch()));
                        standMainSeat.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw() + rotation, standMain.getLocation().getPitch()));
                        standSkin.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw() + rotation, standMain.getLocation().getPitch()));
                    } else {
                        standMain.setRotation(standMain.getLocation().getYaw() + rotation, standMain.getLocation().getPitch());
                        standMainSeat.setRotation(standMain.getLocation().getYaw() + rotation, standMain.getLocation().getPitch());
                        standSkin.setRotation(standMain.getLocation().getYaw() + rotation, standMain.getLocation().getPitch());
                    }
                });
            }
        }
        if (zza > 0.0) {
            if (VehicleData.speed.get(license) < 0) {
                VehicleData.speed.put(license, VehicleData.speed.get(license) + BrakingSpeed);
                return;
            }
            if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.FUEL_ENABLED) && (boolean) ConfigModule.vehicleDataConfig.get(license, VehicleDataConfig.Option.FUEL_ENABLED)) {
                putFuelUsage(license);
            }
            if (VehicleData.speed.get(license) > MaxSpeed - AccelerationSpeed) return;
            VehicleData.speed.put(license, VehicleData.speed.get(license) + AccelerationSpeed);
        }
        if (zza < 0.0) {
            if (VehicleData.speed.get(license) > 0) {
                VehicleData.speed.put(license, VehicleData.speed.get(license) - BrakingSpeed);
                return;
            }
            if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.FUEL_ENABLED) && (boolean) ConfigModule.vehicleDataConfig.get(license, VehicleDataConfig.Option.FUEL_ENABLED)) {
                putFuelUsage(license);
            }
            if (VehicleData.speed.get(license) < -MaxSpeedBackwards) return;
            VehicleData.speed.put(license, VehicleData.speed.get(license) - AccelerationSpeed);
        }
        if (zza == 0.0) {
            BigDecimal round = BigDecimal.valueOf(VehicleData.speed.get(license)).setScale(1, BigDecimal.ROUND_DOWN);
            if (Double.parseDouble(String.valueOf(round)) == 0.0) {
                VehicleData.speed.put(license, 0.0);
                return;
            }
            if (Double.parseDouble(String.valueOf(round)) > 0.01) {
                VehicleData.speed.put(license, VehicleData.speed.get(license) - FrictionSpeed);
                return;
            }
            if (Double.parseDouble(String.valueOf(round)) < 0.01) {
                VehicleData.speed.put(license, VehicleData.speed.get(license) + FrictionSpeed);
            }
        }
    }

    @ToDo(comment = "Moving on snow.")
    protected boolean slabCheck(ArmorStand mainStand) { //Returns true if is moving upwards (in any way)
        final Location loc = getLocationOfBlockAhead(mainStand);
        final String locY = String.valueOf(mainStand.getLocation().getY());
        final Location locBlockAbove = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        final Location locBlockBelow = new Location(loc.getWorld(), loc.getX(), loc.getY() - 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        final String drivingOnY = locY.substring(locY.length() - 2);

        final boolean isOnGround = drivingOnY.contains(".0");
        final boolean isOnSlab = drivingOnY.contains(".5");
        final boolean isPassable = isPassable(loc.getBlock());
        final boolean isAbovePassable = isPassable(locBlockAbove.getBlock());

        final double difference = Double.parseDouble("0." + locY.split("\\.")[1]);
        final BlockData blockData = loc.getBlock().getBlockData();
        final BlockData blockDataBelow = locBlockBelow.getBlock().getBlockData();

        if (loc.getBlock().getType().toString().contains("CARPET")){
            if (!(boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.DRIVE_ON_CARPETS)){ //if carpets are turned off in config
                VehicleData.speed.put(license, 0.0);
                return false;
            }

            if (!isAbovePassable) {
                VehicleData.speed.put(license, 0.0);
                return false;
            }

            if (isOnGround) pushVehicleUp(mainStand, 0.0625);
            return true;
        }

        if (blockData instanceof Snow){
            //Reserved for future update concerning Snow. Just stop for now.
            //Does not include snow block - that's considered a full block.
            VehicleData.speed.put(license, 0.0);
            return false;
        }

        if (blockData instanceof Fence || loc.getBlock().getType().toString().contains("WALL") || blockData instanceof TrapDoor){
            VehicleData.speed.put(license, 0.0);
            return false;
        }

        if (ConfigModule.defaultConfig.driveUpSlabs().isSlabs()){
            if (isOnSlab) {
                if (isPassable) {
                    pushVehicleDown(mainStand, 0.5);
                    return false; //Vehicle will go down
                }

                if (blockData instanceof Slab) {
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)) {
                        return false; //Vehicle will continue on the slabs
                    }
                }

                if (!isAbovePassable) {
                    VehicleData.speed.put(license, 0.0);
                    return false; //Vehicle won't continue if there's a barrier above
                }


                pushVehicleUp(mainStand, 0.5); //Vehicle will go up if there's a full block or a top/double slab
                return true;
            }

            if (!isPassable) {
                if (blockData instanceof Slab) {
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)) {

                        if (!isAbovePassable) {
                            VehicleData.speed.put(license, 0.0);
                            return false; //Vehicle won't go up the slab if there's a barrier above
                        }

                        if (isOnGround) {
                            pushVehicleUp(mainStand, 0.5);
                        } else { //Maybe they're on a carpet
                            if ((0.5 - difference) > 0) pushVehicleUp(mainStand, 0.5 - difference);
                        }
                    }
                }

                VehicleData.speed.put(license, 0.0);
                return false; //If you're on the ground and there isn't bottom slab or a passable block, stop
            }

        } else if (ConfigModule.defaultConfig.driveUpSlabs().isBlocks()) {

            if (!isOnSlab) {
                if (!isPassable) {
                    if (blockData instanceof Slab){
                        Slab slab = (Slab) blockData;
                        if (slab.getType().equals(Slab.Type.BOTTOM)){
                            VehicleData.speed.put(license, 0.0);
                            return false; //If it's a bottom slab, stop.
                        }
                    }

                    if (!isAbovePassable) { //if more than 1 block high
                        VehicleData.speed.put(license, 0.0);
                        return false;
                    }

                    if (isOnGround) {
                        pushVehicleUp(mainStand, 1);
                    } else { //Maybe they're on a carpet
                        if ((1 - difference) > 0) pushVehicleUp(mainStand, 1 - difference);
                    }

                    return true;
                }
            }

            //If it's on a slab (might have been placed there)
            if (isPassable) {
                pushVehicleDown(mainStand, 0.5);
                return false; //Vehicle will go down
            }

            if (blockData instanceof Slab) {
                Slab slab = (Slab) blockData;
                if (slab.getType().equals(Slab.Type.BOTTOM)) {
                    return false; //Vehicle will continue on the slabs
                }
            }

            if (!isAbovePassable) {
                VehicleData.speed.put(license, 0.0);
                return false; //Vehicle won't continue if there's a barrier above
            }

            pushVehicleUp(mainStand, 0.5); //Vehicle will go up if there's a full block or a top/double slab
            return true;

        } else if (ConfigModule.defaultConfig.driveUpSlabs().isBoth()) {

            if (isOnSlab) {
                if (isPassable) {
                    pushVehicleDown(mainStand, 0.5);
                    return false; //Vehicle will go down
                }

                if (blockData instanceof Slab) {
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)) {
                        return false; //Vehicle will continue on the slabs
                    }
                }

                if (!isAbovePassable) {
                    VehicleData.speed.put(license, 0.0);
                    return false; //Vehicle won't continue if there's a barrier above
                }

                pushVehicleUp(mainStand, 0.5); //Vehicle will go up if there's a full block or a top/double slab
                return true;
            }

            if (!isPassable) {

                if (!isAbovePassable) { //If more than 1 block high
                    VehicleData.speed.put(license, 0.0);
                    return false;
                }

                if (blockData instanceof Slab){
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)){ //If it's a bottom slab
                        if (isOnGround) {
                            pushVehicleUp(mainStand, 0.5);
                        } else { //Maybe they're on a carpet
                            if ((0.5 - difference) > 0) pushVehicleUp(mainStand, 0.5 - difference);
                        }
                        return true;
                    }
                }

                //If it's another block or a top/double slab
                if (isOnGround) {
                    pushVehicleUp(mainStand, 1);
                } else { //Maybe they're on a carpet
                    if ((1 - difference) > 0) pushVehicleUp(mainStand, 1 - difference);
                }

                return true;

            }

            if (blockDataBelow instanceof Slab){ //If on a block, in front of it is air and below is bottom slab
                Slab slab = (Slab) blockDataBelow;
                if (slab.getType().equals(Slab.Type.BOTTOM)) { //If it's a bottom slab
                    pushVehicleDown(mainStand, 0.5);
                    return false; //Vehicle will go down
                }
            }

        }

        return false;
    }

    protected void mainSeat(ArmorStand mainStand, ArmorStand mainSeat) {
        if (VehicleData.seatsize.get(license) != null) {
            for (int i = 2; i <= VehicleData.seatsize.get(license); i++) {
                ArmorStand seatas = VehicleData.autostand.get("MTVEHICLES_SEAT" + i + "_" + license);
                double xOffset = VehicleData.seatx.get("MTVEHICLES_SEAT" + i + "_" + license);
                double yOffset = VehicleData.seaty.get("MTVEHICLES_SEAT" + i + "_" + license);
                double zOffset = VehicleData.seatz.get("MTVEHICLES_SEAT" + i + "_" + license);
                Location locvp = mainStand.getLocation().clone();
                Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
                float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
                float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
                Location loc = new Location(mainStand.getWorld(), xvp, mainStand.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
                teleportSeat(seatas, loc);
            }
        }
        double xOffset = VehicleData.mainx.get("MTVEHICLES_MAINSEAT_" + license);
        double yOffset = VehicleData.mainy.get("MTVEHICLES_MAINSEAT_" + license);
        double zOffset = VehicleData.mainz.get("MTVEHICLES_MAINSEAT_" + license);
        Location locvp = mainStand.getLocation().clone();
        Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
        float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
        float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
        Location loc = new Location(mainStand.getWorld(), xvp, mainStand.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
        teleportSeat(mainSeat, loc);
    }

    protected void teleportSeat(ArmorStand seat, Location loc){
        if (getServerVersion().is1_12()) teleportSeat(((org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity) seat).getHandle(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        else if (getServerVersion().is1_13()) teleportSeat(((org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity) seat).getHandle(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        else if (getServerVersion().is1_15()) teleportSeat(((org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity) seat).getHandle(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        else if (getServerVersion().is1_16()) teleportSeat(((org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity) seat).getHandle(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        else if (getServerVersion().is1_17()) teleportSeat(((org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity) seat).getHandle(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        else if (getServerVersion().is1_18()) teleportSeat(((org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity) seat).getHandle(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    protected static String getTeleportMethod(){
        if (getServerVersion().is1_18()) return "a";
        else return "setLocation";
    }

    protected void teleportSeat(Object seat, double x, double y, double z, float yaw, float pitch){
        schedulerRun(() -> {
            try {
                Method method = seat.getClass().getSuperclass().getSuperclass().getDeclaredMethod(getTeleportMethod(), double.class, double.class, double.class, float.class, float.class);
                method.invoke(seat, x, y, z, yaw, pitch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @ToDo(comment = "Moving forwards when landing with no fuel - for more realistic movement.")
    protected void updateStand(ArmorStand mainStand, boolean space, boolean extremeFalling) {
        final Location loc = mainStand.getLocation();
        final Location locBlockAhead = getLocationOfBlockAhead(mainStand);
        final Location locBlockAheadAndBelow = new Location(locBlockAhead.getWorld(), locBlockAhead.getX(), locBlockAhead.getY() - 1, locBlockAhead.getZ(), locBlockAhead.getPitch(), locBlockAhead.getYaw());
        final Location locBelow = new Location(loc.getWorld(), loc.getX(), loc.getY() - 0.2, loc.getZ(), loc.getYaw(), loc.getPitch());

        if (VehicleData.type.get(license) == null) return;
        final VehicleType vehicleType = VehicleType.valueOf(VehicleData.type.get(license));
        if (vehicleType == null) return;
        final Material block = locBelow.getBlock().getType();
        final String blockName = block.toString();

        if (vehicleType.canFly()) {

            if ((vehicleType.isHelicopter() && !isPassable(locBelow.getBlock()))
                    || (vehicleType.isAirplane() && VehicleData.fuel.get(license) < 1 && !block.equals(Material.AIR))
            ) VehicleData.speed.put(license, 0.0);

            if (space) {
                final double takeOffSpeed = ((double) ConfigModule.defaultConfig.get(DefaultConfig.Option.TAKE_OFF_SPEED) > 0) ? (double) ConfigModule.defaultConfig.get(DefaultConfig.Option.TAKE_OFF_SPEED) : 0.4;
                if (vehicleType.isAirplane() && VehicleData.speed.get(license) < takeOffSpeed) {
                    double y = (isPassable(locBelow.getBlock())) ? -0.2 : 0;
                    mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), y, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                    return;
                }

                putFuelUsage(license);

                if (loc.getY() > (int) ConfigModule.defaultConfig.get(DefaultConfig.Option.MAX_FLYING_HEIGHT)) return;
                mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.2, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }

            if (extremeFalling){
                if (mainStand.isOnGround()){
                    if (VehicleData.fallDamage.get(license) == null){
                        double damageAmount = ((double) ConfigModule.defaultConfig.get(DefaultConfig.Option.HELICOPTER_FALL_DAMAGE) <= 0) ?
                                (double) DefaultConfig.Option.HELICOPTER_FALL_DAMAGE.getDefaultValue() : (double) ConfigModule.defaultConfig.get(DefaultConfig.Option.HELICOPTER_FALL_DAMAGE);
                        schedulerRun(() -> {
                            player.damage(damageAmount);
                            if (VehicleData.seatsize.get(license) != null) { //Damage all passengers too
                                for (int i = 2; i <= VehicleData.seatsize.get(license); i++) {
                                    ArmorStand seat = VehicleData.autostand.get("MTVEHICLES_SEAT" + i + "_" + license);
                                    List<Entity> passengers = seat.getPassengers();
                                    for (Entity p: passengers) {
                                        if (p instanceof LivingEntity){
                                            ((LivingEntity) p).damage(damageAmount);
                                        }
                                    }
                                }
                            }
                        });
                        VehicleData.fallDamage.put(license, true);
                    }
                }
                mainStand.setGravity(true);
                return;
            }

            putFuelUsage(license);
            mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.2, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
            return;
        }

        if (vehicleType.isHover()) {
            if (block.equals(Material.AIR)) {
                mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.8, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }
            mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.00001, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
            return;
        }

        if (blockName.contains("WATER")) {
            mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.8, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
            return;
        }

        if (isPassable(locBlockAhead.getBlock()) && isPassable(locBlockAheadAndBelow.getBlock())){
            if (isPassable(locBelow.getBlock())){
                mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.8, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }

            if (blockName.contains("CARPET")){
                mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.7375, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }
        }

        mainStand.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.0, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
    }

    protected void putFuelUsage(String license) {
        double fuelMultiplier = Double.parseDouble(ConfigModule.defaultConfig.get(DefaultConfig.Option.FUEL_MULTIPLIER).toString());
        if (fuelMultiplier < 0.1 || fuelMultiplier > 10) fuelMultiplier = 1; //Must be between 0.1 and 10. Default: 1
        final double newFuel = VehicleData.fuel.get(license) - (fuelMultiplier * VehicleData.fuelUsage.get(license));
        if (newFuel < 0) VehicleData.fuel.put(license, 0.0);
        else VehicleData.fuel.put(license, newFuel);
    }

    protected boolean isPassable(Block block){
        return block.isPassable();
    }

    protected void rotors(ArmorStand main, ArmorStand seatas, String license, boolean slowDown, boolean extremeFalling) {
        double xOffset = VehicleData.wiekenx.get("MTVEHICLES_WIEKENS_" + license);
        double yOffset = VehicleData.wiekeny.get("MTVEHICLES_WIEKENS_" + license);
        double zOffset = VehicleData.wiekenz.get("MTVEHICLES_WIEKENS_" + license);
        final Location locvp = main.getLocation().clone();
        final Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
        final float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(seatas.getLocation().getYaw())));
        final float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(seatas.getLocation().getYaw())));
        float yawAdd = (slowDown) ? 5 : 15;
        if (extremeFalling) yawAdd = 0;
        final Location loc = new Location(main.getWorld(), xvp, main.getLocation().getY() + yOffset, zvp, seatas.getLocation().getYaw() + yawAdd, seatas.getLocation().getPitch());
        schedulerRun(() -> seatas.teleport(loc));
    }

    protected void pushVehicleUp(ArmorStand mainStand, double plus){
        final Location newLoc = new Location(mainStand.getLocation().getWorld(), mainStand.getLocation().getX(), mainStand.getLocation().getY() + plus, mainStand.getLocation().getZ(), mainStand.getLocation().getYaw(), mainStand.getLocation().getPitch());
        schedulerRun(() -> mainStand.teleport(newLoc));
    }

    protected void pushVehicleDown(ArmorStand mainStand, double minus){
        pushVehicleUp(mainStand, -minus);
    }

    protected Location getLocationOfBlockAhead(ArmorStand mainStand){
        double xOffset = 0.7;
        double yOffset = 0.4;
        double zOffset = 0.0;
        Location locvp = mainStand.getLocation().clone();
        Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
        float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
        float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
        return new Location(mainStand.getWorld(), xvp, mainStand.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
    }

    protected boolean steerIsJumping(Object packet){
        if (!isObjectPacket(packet)) return false;

        boolean isJumping = false;
        try {
            Method method = packet.getClass().getDeclaredMethod("d");
            isJumping = (Boolean) method.invoke(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isJumping;
    }

    protected float steerGetXxa(Object packet){
        if (!isObjectPacket(packet)) return 0;

        float Xxa = 0;
        try {
            Method method = packet.getClass().getDeclaredMethod("b");
            Xxa = (float) method.invoke(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Xxa;
    }

    protected float steerGetZza(Object packet){
        if (!isObjectPacket(packet)) return 0;

        float Zza = 0;
        try {
            Method method = packet.getClass().getDeclaredMethod("c");
            Zza = (float) method.invoke(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Zza;
    }

    protected void spawnParticles(ArmorStand stand, Location loc){
        stand.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 2);
        stand.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2);
        stand.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 5);
        if (!getServerVersion().isOld())
            stand.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 5);
    }

    protected void spawnTNT(ArmorStand stand, Location loc){
        if (!(boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.TANK_TNT)) return;

        schedulerRun(() -> {
            TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
            tnt.setFuseTicks(20);
            tnt.setVelocity(stand.getLocation().getDirection().multiply(3.0));
        });
    }

    protected static void schedulerRun(Runnable task){
        Bukkit.getScheduler().runTask(Main.instance, task);
    }

}

