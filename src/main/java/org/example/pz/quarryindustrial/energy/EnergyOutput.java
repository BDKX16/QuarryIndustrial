package org.example.pz.quarryindustrial.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Como {@link #push}, pero parejo: cada vecino recibe la misma parte; lo que un vecino lleno no acepta
     * se reparte entre los demás. {@code excludedMask} tiene un bit por {@link Direction#ordinal()} de las
     * caras que no reciben nada, y {@code rotation} rota quién se queda con el resto cuando no divide exacto.
     */
    public static long pushEvenly(ServerLevel level, BlockPos pos, SimpleEnergyStorage buffer, long maxPerTick,
                                  int excludedMask, int rotation) {
        List<EnergyStorage> active = new ArrayList<>(6);
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[Math.floorMod(i + rotation, directions.length)];
            if ((excludedMask & (1 << direction.ordinal())) != 0) {
                continue;
            }
            EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (target != null && target.supportsInsertion()) {
                active.add(target);
            }
        }
        long left = Math.min(maxPerTick, buffer.amount);
        long moved = 0;
        while (left > 0 && !active.isEmpty()) {
            long share = Math.max(1, left / active.size());
            boolean progress = false;
            for (java.util.Iterator<EnergyStorage> it = active.iterator(); it.hasNext() && left > 0; ) {
                EnergyStorage target = it.next();
                long offer = Math.min(share, left);
                long accepted;
                try (Transaction transaction = Transaction.openOuter()) {
                    accepted = target.insert(offer, transaction);
                    transaction.commit();
                }
                buffer.amount -= accepted;
                left -= accepted;
                moved += accepted;
                progress |= accepted > 0;
                if (accepted < offer) {
                    it.remove();
                }
            }
            if (!progress) {
                break;
            }
        }
        return moved;
    }
}
