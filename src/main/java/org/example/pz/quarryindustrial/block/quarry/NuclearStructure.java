package org.example.pz.quarryindustrial.block.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.example.pz.quarryindustrial.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * El cuerpo de la quarry nuclear: un cubo de 3×3×3 detrás del core. El core ocupa el centro de la cara
 * delantera; las otras 26 posiciones tienen que ser carcasa o puertos de energía.
 */
public final class NuclearStructure {
    public static final int DEPTH = 3;

    /** Resultado de revisar el cubo: los puertos encontrados y las posiciones que faltan. */
    public record Scan(List<BlockPos> ports, List<BlockPos> missing) {
        public boolean formed(int maxPorts) {
            return missing.isEmpty() && !ports.isEmpty() && ports.size() <= maxPorts;
        }
    }

    private NuclearStructure() {
    }

    /** Las 26 posiciones del cubo, sin el core. */
    public static List<BlockPos> positions(BlockPos core, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        List<BlockPos> positions = new ArrayList<>();
        for (int depth = 0; depth < DEPTH; depth++) {
            for (int lateral = -1; lateral <= 1; lateral++) {
                for (int height = -1; height <= 1; height++) {
                    if (depth == 0 && lateral == 0 && height == 0) {
                        continue;
                    }
                    positions.add(core.relative(back, depth).relative(right, lateral).above(height));
                }
            }
        }
        return positions;
    }

    public static Scan scan(BlockGetter level, BlockPos core, Direction facing) {
        List<BlockPos> ports = new ArrayList<>();
        List<BlockPos> missing = new ArrayList<>();
        for (BlockPos pos : positions(core, facing)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.ENERGY_PORT)) {
                ports.add(pos);
            } else if (!state.is(ModBlocks.QUARRY_CASING)) {
                missing.add(pos);
            }
        }
        return new Scan(ports, missing);
    }
}
