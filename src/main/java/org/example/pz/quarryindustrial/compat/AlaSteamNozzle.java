package org.example.pz.quarryindustrial.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.example.pz.quarryindustrial.Quarryindustrial;

import java.lang.reflect.Field;

/**
 * Vapor de la Steam Nozzle de Ala Industrial. La nozzle no deja sacar vapor por la API de fluidos (solo
 * entra), y lo ventea por su boca si el bloque de enfrente está libre. Una turbina puesta justo en la boca
 * la tapa (la nozzle deja de ventear) y le saca el vapor del tanque directamente: el campo público
 * {@code tank.amount} (en mB). Por reflexión, para no compilar contra Ala; si su API cambia, no hace nada.
 */
public final class AlaSteamNozzle {
    private static final String NOZZLE_CLASS = "dev.alaindustrial.block.entity.SteamNozzleBlockEntity";
    private static Field tankField;
    private static Field amountField;
    private static boolean broken;

    private AlaSteamNozzle() {
    }

    /**
     * Si en {@code pos.relative(side)} hay una nozzle con la boca hacia {@code pos}, le saca hasta
     * {@code maxMb} de vapor. Devuelve los mB sacados.
     */
    public static long drain(Level level, BlockPos pos, Direction side, long maxMb) {
        if (broken || maxMb <= 0) {
            return 0;
        }
        BlockPos nozzlePos = pos.relative(side);
        BlockEntity nozzle = level.getBlockEntity(nozzlePos);
        if (nozzle == null || !nozzle.getClass().getName().equals(NOZZLE_CLASS)
                || facing(nozzle.getBlockState()) != side.getOpposite()) {
            return 0;
        }
        try {
            if (tankField == null) {
                tankField = nozzle.getClass().getField("tank");
            }
            Object tank = tankField.get(nozzle);
            if (amountField == null) {
                amountField = tank.getClass().getField("amount");
            }
            long amount = amountField.getLong(tank);
            long taken = Math.min(amount, maxMb);
            if (taken > 0) {
                amountField.setLong(tank, amount - taken);
                nozzle.setChanged();
            }
            return taken;
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            Quarryindustrial.LOGGER.warn("No se pudo leer el vapor de la Steam Nozzle de Ala Industrial", e);
            return 0;
        }
    }

    private static Direction facing(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("facing") && state.getValue(property) instanceof Direction direction) {
                return direction;
            }
        }
        return null;
    }
}
