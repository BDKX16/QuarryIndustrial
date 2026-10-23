package org.example.pz.quarryindustrial.block.pumpjack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Volumen que ocupa una cigüeña: 3 de ancho × 7 de largo × 5 de alto. El controlador es la cabeza de pozo,
 * en la punta delantera (hacia {@code facing}) y al nivel del suelo; el resto se extiende hacia atrás.
 *
 * <p>Coordenadas locales: {@code lateral} −1..1, {@code height} 0..4, {@code back} 0..6 (0 = cabeza de pozo,
 * 6 = el motor).
 */
public final class PumpjackLayout {
    public static final int HALF_WIDTH = 1;
    public static final int HEIGHT = 5;
    public static final int LENGTH = 7;

    private PumpjackLayout() {
    }

    public static BlockPos toWorld(BlockPos wellhead, Direction facing, int lateral, int height, int back) {
        return wellhead.relative(facing.getOpposite(), back).relative(facing.getClockWise(), lateral).above(height);
    }

    /** Todas las posiciones de partes (sin el controlador). */
    public static List<BlockPos> partPositions(BlockPos wellhead, Direction facing) {
        List<BlockPos> positions = new ArrayList<>();
        for (int back = 0; back < LENGTH; back++) {
            for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
                for (int height = 0; height < HEIGHT; height++) {
                    if (back == 0 && lateral == 0 && height == 0) {
                        continue;
                    }
                    positions.add(toWorld(wellhead, facing, lateral, height, back));
                }
            }
        }
        return positions;
    }

    /** Vecinos de la base que quedan fuera de la estructura: por ahí se conectan caños y cables. */
    public static List<BlockPos> portNeighbours(BlockPos wellhead, Direction facing) {
        List<BlockPos> ports = new ArrayList<>();
        for (int back = 0; back < LENGTH; back++) {
            ports.add(toWorld(wellhead, facing, -HALF_WIDTH - 1, 0, back));
            ports.add(toWorld(wellhead, facing, HALF_WIDTH + 1, 0, back));
        }
        for (int lateral = -HALF_WIDTH; lateral <= HALF_WIDTH; lateral++) {
            ports.add(toWorld(wellhead, facing, lateral, 0, -1));
            ports.add(toWorld(wellhead, facing, lateral, 0, LENGTH));
        }
        return ports;
    }
}
