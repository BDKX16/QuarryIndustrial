package org.example.pz.quarryindustrial.registry;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Logros que dependen de lo que hacen las máquinas (no de tener un item). En {@code data/.../advancement}
 * tienen un único criterio "done" con el trigger {@code minecraft:impossible}; acá se otorgan a los jugadores
 * que estén cerca de la máquina (y al dueño de la quarry, esté donde esté).
 */
public final class ModAdvancements {
    public static final String FRAME_BUILT = "frame_built";
    public static final String ROCK_BOTTOM = "rock_bottom";
    public static final String BIT_BROKEN = "bit_broken";
    public static final String NUCLEAR_EIGHT = "nuclear_eight";
    public static final String BLACK_GOLD = "black_gold";
    public static final String BOTTOMLESS = "bottomless";
    public static final String SELF_SUFFICIENT = "self_sufficient";
    public static final String INTO_THE_DEEP = "into_the_deep";
    public static final String WASTE_NOT = "waste_not";
    public static final String AIR_MAIL = "air_mail";
    public static final String PIT_CREW = "pit_crew";
    public static final String PROSPECTOR = "prospector";

    private static final double NEARBY = 96;

    private ModAdvancements() {
    }

    public static void grant(ServerLevel level, BlockPos pos, String name) {
        grant(level, pos, name, null);
    }

    public static void grant(ServerLevel level, BlockPos pos, String name, @Nullable UUID owner) {
        AdvancementHolder advancement = level.getServer().getAdvancements().get(Quarryindustrial.id(name));
        if (advancement == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(center) <= NEARBY * NEARBY) {
                award(player, advancement);
            }
        }
        if (owner != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                award(player, advancement);
            }
        }
    }

    private static void award(ServerPlayer player, AdvancementHolder advancement) {
        if (!player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "done");
        }
    }
}
