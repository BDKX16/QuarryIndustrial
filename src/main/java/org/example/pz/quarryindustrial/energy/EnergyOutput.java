package org.example.pz.quarryindustrial.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/** Generadores: empujan su buffer a cualquier cable o máquina pegada, como hacen los generadores de Ala. */
public final class EnergyOutput {
    private EnergyOutput() {
    }

    /** Reparte hasta {@code maxPerTick} EU entre los vecinos. Devuelve cuánto salió. */
    public static long push(ServerLevel level, BlockPos pos, SimpleEnergyStorage buffer, long maxPerTick) {
        long left = Math.min(maxPerTick, buffer.amount);
        long moved = 0;
        for (Direction direction : Direction.values()) {
            if (left <= 0) {
                break;
            }
            EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long accepted = target.insert(left, transaction);
                buffer.amount -= accepted;
                left -= accepted;
                moved += accepted;
                transaction.commit();
            }
        }
        return moved;
    }
}
