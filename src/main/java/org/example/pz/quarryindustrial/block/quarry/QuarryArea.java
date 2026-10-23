package org.example.pz.quarryindustrial.block.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometría de una quarry: el cuadrado a minar [minX..maxX]×[minZ..maxZ] y el pórtico que lo rodea.
 *
 * <p>El marco va un bloque por fuera del área: cuatro patas en las esquinas desde {@code baseY} (la altura
 * de la quarry) hasta {@code topY}, y un anillo de vigas a la altura {@code topY} sobre el que corre el puente.
 * Se mina desde {@code topY - 1} hacia abajo, así no queda terreno chocando con el puente.
 */
public record QuarryArea(int minX, int maxX, int minZ, int maxZ, int baseY, int topY) {

    /**
     * El área queda detrás de la quarry (del lado opuesto a su frente), centrada respecto de ella y
     * empezando {@code offset} bloques atrás (1 para las quarries simples, 3 para el multibloque).
     */
    public static QuarryArea behind(BlockPos pos, Direction facing, int side, int frameHeight, int offset) {
        Direction back = facing.getOpposite();
        Direction right = back.getClockWise();
        BlockPos near = pos.relative(back, offset).relative(right, -(side / 2));
        BlockPos far = near.relative(back, side - 1).relative(right, side - 1);
        return new QuarryArea(Math.min(near.getX(), far.getX()), Math.max(near.getX(), far.getX()),
                Math.min(near.getZ(), far.getZ()), Math.max(near.getZ(), far.getZ()),
                pos.getY(), pos.getY() + frameHeight);
    }

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int startY() {
        return topY - 1;
    }

    /** Posiciones del marco en orden de construcción: primero las patas de abajo hacia arriba, después el anillo. */
    public List<BlockPos> framePositions() {
        List<BlockPos> positions = new ArrayList<>();
        int x0 = minX - 1;
        int x1 = maxX + 1;
        int z0 = minZ - 1;
        int z1 = maxZ + 1;
        for (int y = baseY; y < topY; y++) {
            positions.add(new BlockPos(x0, y, z0));
            positions.add(new BlockPos(x1, y, z0));
            positions.add(new BlockPos(x0, y, z1));
            positions.add(new BlockPos(x1, y, z1));
        }
        for (int x = x0; x <= x1; x++) {
            positions.add(new BlockPos(x, topY, z0));
            positions.add(new BlockPos(x, topY, z1));
        }
        for (int z = z0 + 1; z < z1; z++) {
            positions.add(new BlockPos(x0, topY, z));
            positions.add(new BlockPos(x1, topY, z));
        }
        return positions;
    }

    public void save(ValueOutput output) {
        output.putIntArray("Area", new int[]{minX, maxX, minZ, maxZ, baseY, topY});
    }

    public static QuarryArea load(ValueInput input) {
        int[] a = input.getIntArray("Area").orElse(null);
        if (a == null || a.length != 6) {
            return null;
        }
        return new QuarryArea(a[0], a[1], a[2], a[3], a[4], a[5]);
    }
}
