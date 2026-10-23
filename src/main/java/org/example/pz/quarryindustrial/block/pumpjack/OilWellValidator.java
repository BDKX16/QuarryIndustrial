package org.example.pz.quarryindustrial.block.pumpjack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Reconoce el pozo de un géiser de Ala Industrial solo por su forma, sin tocar clases de Ala.
 * El géiser ({@code OilGeyserPlacer}) es:
 * <ul>
 *   <li>un caño vertical de 1×1 desde la superficie, de al menos 24 bloques, con roca alrededor
 *       (Ala lo sella con piedra donde perdería);</li>
 *   <li>un domo esférico de radio 8–11 centrado cerca de Y=-45, donde termina el caño.</li>
 * </ul>
 * Recién generado está todo lleno de oil; vaciado con la bomba de Ala, es aire. Las dos cosas cuentan
 * como "abierto" ({@link OilFluids#isOpen}), así la cigüeña funciona sobre un géiser lleno o vacío.
 */
public final class OilWellValidator {
    /** Distancia a la que se buscan celdas abiertas para decidir que el caño entró en el domo. */
    private static final int CAVITY_PROBE = 2;

    /** Lo que se sabe del pozo: dónde arranca el caño, dónde está el domo y su tamaño. */
    public record Well(BlockPos shaftTop, BlockPos cavityEntry, BlockPos cavityCenter, int radius) {
    }

    private OilWellValidator() {
    }

    /**
     * Valida el pozo que baja desde justo debajo de {@code wellhead}. Devuelve null si no es un géiser:
     * caño muy corto, demasiado ancho (un agujero cualquiera), o una cavidad chica o muy alta.
     */
    public static @Nullable Well validate(Level level, BlockPos wellhead) {
        QuarryConfig.Pumpjack config = QuarryConfig.get().pumpjack;
        BlockPos.MutableBlockPos cursor = wellhead.below().mutable();
        if (!OilFluids.isOpen(level.getBlockState(cursor))) {
            return null;
        }

        int shaftLength = 0;
        int sealedLevels = 0;
        int minY = Math.max(level.getMinY(), wellhead.getY() - config.maxScanDepth);
        while (cursor.getY() > minY) {
            if (!level.isLoaded(cursor) || !OilFluids.isOpen(level.getBlockState(cursor))) {
                return null;
            }
            // Un caño de géiser es de 1×1 casi todo el recorrido; un pozo cavado a mano o una cueva no
            boolean shaftOk = shaftLength >= config.minShaftLength && sealedLevels >= shaftLength * 0.8;
            if (shaftOk && opensIntoCavity(level, cursor)) {
                Well well = measureCavity(level, wellhead, cursor.immutable(), config);
                if (well != null) {
                    return well;
                }
                // Era una cueva al costado del caño, no el domo: seguir bajando
            }
            int solidSides = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!OilFluids.isOpen(level.getBlockState(cursor.relative(direction)))) {
                    solidSides++;
                }
            }
            if (solidSides >= 3) {
                sealedLevels++;
            }
            shaftLength++;
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    /** Mide la cavidad bajando por su eje desde {@code entry} hasta tocar el fondo. */
    private static @Nullable Well measureCavity(Level level, BlockPos wellhead, BlockPos entry,
                                                QuarryConfig.Pumpjack config) {
        BlockPos.MutableBlockPos cursor = entry.mutable();
        while (cursor.getY() > level.getMinY() && OilFluids.isOpen(level.getBlockState(cursor.below()))) {
            cursor.move(Direction.DOWN);
        }
        int bottomY = cursor.getY();
        int radius = (entry.getY() - bottomY + 1) / 2;
        BlockPos center = new BlockPos(entry.getX(), (entry.getY() + bottomY) / 2, entry.getZ());
        if (radius < config.minCavityRadius || center.getY() > config.maxCavityCenterY) {
            return null;
        }
        // Que sea una cámara y no un caño que sigue: tiene que estar abierta hacia los cuatro lados
        int reach = Math.max(1, radius - 2);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos probe = center.relative(direction, reach);
            if (!level.isLoaded(probe) || !OilFluids.isOpen(level.getBlockState(probe))) {
                return null;
            }
        }
        return new Well(wellhead.below(), entry, center, radius);
    }

    /** El caño entró al domo cuando a dos bloques hacia al menos tres lados ya está abierto. */
    private static boolean opensIntoCavity(Level level, BlockPos pos) {
        int open = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos probe = pos.relative(direction, CAVITY_PROBE);
            if (level.isLoaded(probe) && OilFluids.isOpen(level.getBlockState(probe))) {
                open++;
            }
        }
        return open >= 3;
    }
}
