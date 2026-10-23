package org.example.pz.quarryindustrial.block.offshore;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import org.example.pz.quarryindustrial.config.QuarryConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometría de la plataforma offshore: una cubierta de 7×7 a nivel del mar (el controlador en el centro,
 * sobre el "moonpool" por donde baja la sarta) y cuatro patas en las esquinas, desde la cubierta hasta el
 * lecho marino. La torre de perforación (derrick) la dibuja el renderer sobre los 3×3 del centro.
 */
public final class OffshoreLayout {
    public static final int HALF = 3;
    /**
     * Punta de la antorcha de gas (relativa al centro de la cara de arriba del controlador): un brazo que
     * sale en diagonal desde la esquina (-x, +z) de la cubierta, hacia afuera y hacia arriba.
     */
    public static final double FLARE_TIP_X = -6.5;
    public static final double FLARE_TIP_Y = 7.0;
    public static final double FLARE_TIP_Z = 6.5;

    /** Por qué no se puede poner la plataforma acá (o null si se puede). */
    public enum Problem {
        NOT_OCEAN,
        NOT_ON_WATER,
        TOO_SHALLOW,
        NO_ROOM
    }

    private OffshoreLayout() {
    }

    /** Las 48 posiciones de la cubierta, sin el controlador. */
    public static List<BlockPos> deckPositions(BlockPos controller) {
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                if (dx != 0 || dz != 0) {
                    positions.add(controller.offset(dx, 0, dz));
                }
            }
        }
        return positions;
    }

    /** El bloque de cubierta de cada esquina, debajo del cual baja una pata. */
    public static List<BlockPos> corners(BlockPos controller) {
        return List.of(controller.offset(-HALF, 0, -HALF), controller.offset(HALF, 0, -HALF),
                controller.offset(-HALF, 0, HALF), controller.offset(HALF, 0, HALF));
    }

    /** Cuántos bloques de agua hay debajo de {@code pos} hasta el fondo (cortando en maxWaterDepth). */
    public static int waterDepth(Level level, BlockPos pos) {
        int max = QuarryConfig.get().offshore.maxWaterDepth;
        int depth = 0;
        BlockPos.MutableBlockPos cursor = pos.below().mutable();
        while (depth < max && level.isInsideBuildHeight(cursor.getY())
                && level.getFluidState(cursor).is(FluidTags.WATER)) {
            depth++;
            cursor.move(0, -1, 0);
        }
        return depth;
    }

    public static Problem check(Level level, BlockPos controller) {
        if (QuarryConfig.get().offshore.requireOceanBiome && !level.getBiome(controller).is(BiomeTags.IS_OCEAN)) {
            return Problem.NOT_OCEAN;
        }
        if (!level.getFluidState(controller.below()).is(FluidTags.WATER)) {
            return Problem.NOT_ON_WATER;
        }
        int min = QuarryConfig.get().offshore.minWaterDepth;
        if (waterDepth(level, controller) < min) {
            return Problem.TOO_SHALLOW;
        }
        for (BlockPos corner : corners(controller)) {
            if (waterDepth(level, corner) < 1) {
                return Problem.TOO_SHALLOW;
            }
        }
        for (BlockPos deck : deckPositions(controller)) {
            if (!level.isInsideBuildHeight(deck.getY()) || !level.getBlockState(deck).canBeReplaced()) {
                return Problem.NO_ROOM;
            }
        }
        return null;
    }
}
