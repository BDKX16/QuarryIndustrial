package org.example.pz.quarryindustrial.block.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Un cabezal de perforación: recorre su franja [minX..maxX]×[minZ..maxZ] fila a fila y, al terminar
 * la capa, baja una. Las quarries con varios cabezales reparten el área en franjas a lo largo de X.
 * El cursor apunta siempre al próximo bloque a revisar.
 */
public final class DrillHead {
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private int x;
    private int y;
    private int z;
    private boolean finished;

    public DrillHead(int minX, int maxX, int minZ, int maxZ, int startY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.x = minX;
        this.y = startY;
        this.z = minZ;
    }

    public BlockPos cursor() {
        return new BlockPos(x, y, z);
    }

    /** Avanza al siguiente bloque. Al pasar por debajo de {@code minY} el cabezal queda terminado. */
    public void advance(int minY) {
        x++;
        if (x > maxX) {
            x = minX;
            z++;
            if (z > maxZ) {
                z = minZ;
                y--;
                if (y < minY) {
                    finished = true;
                }
            }
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public int y() {
        return y;
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minZ() {
        return minZ;
    }

    public int maxZ() {
        return maxZ;
    }

    public void save(ValueOutput output) {
        output.putIntArray("Bounds", new int[]{minX, maxX, minZ, maxZ});
        output.putIntArray("Cursor", new int[]{x, y, z});
        output.putBoolean("Finished", finished);
    }

    public static DrillHead load(ValueInput input) {
        int[] bounds = input.getIntArray("Bounds").orElse(new int[4]);
        int[] cursor = input.getIntArray("Cursor").orElse(new int[3]);
        if (bounds.length != 4 || cursor.length != 3) {
            bounds = new int[4];
            cursor = new int[3];
        }
        DrillHead head = new DrillHead(bounds[0], bounds[1], bounds[2], bounds[3], cursor[1]);
        head.x = cursor[0];
        head.z = cursor[2];
        head.finished = input.getBooleanOr("Finished", false);
        return head;
    }
}
