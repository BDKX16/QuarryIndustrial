package org.example.pz.quarryindustrial.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * El Oil Slime de Ala Industrial: de vez en cuando sube uno por el pozo mientras se extrae petróleo. Se busca
 * por id, así que sin Ala no pasa nada.
 */
public final class AlaOilSlime {
    private static final Identifier OIL_SLIME = Identifier.fromNamespaceAndPath("alaindustrial", "oil_slime");

    private AlaOilSlime() {
    }

    /** Con probabilidad 1 en {@code chance}, suelta un Oil Slime en {@code spot} (si está libre y no es pacífico). */
    public static void maybeRelease(ServerLevel level, BlockPos spot, int chance) {
        if (chance <= 0 || level.getDifficulty() == Difficulty.PEACEFUL || level.getRandom().nextInt(chance) != 0) {
            return;
        }
        if (!level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()) {
            return;
        }
        BuiltInRegistries.ENTITY_TYPE.getOptional(OIL_SLIME)
                .ifPresent(type -> type.spawn(level, spot, EntitySpawnReason.TRIGGERED));
    }
}
