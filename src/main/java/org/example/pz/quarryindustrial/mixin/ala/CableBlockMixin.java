package org.example.pz.quarryindustrial.mixin.ala;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Los cables de Ala Industrial solo dibujan brazo hacia sus propias máquinas. Esto les suma los bloques de
 * nuestro tag {@code quarryindustrial:cable_connectable} (pads, quarrys, distribuidor…), que ya reciben
 * energía por la API de Team Reborn pero no se veían conectados.
 */
@Pseudo
@Mixin(targets = "dev.alaindustrial.block.CableBlock")
public abstract class CableBlockMixin {
    private static final TagKey<Block> CONNECTABLE =
            TagKey.create(Registries.BLOCK, Quarryindustrial.id("cable_connectable"));

    @Inject(method = "connectsTo", at = @At("RETURN"), cancellable = true, require = 0)
    private static void quarryindustrial$connectsTo(LevelReader level, BlockPos pos, Direction direction,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && level.getBlockState(pos.relative(direction)).is(CONNECTABLE)) {
            cir.setReturnValue(true);
        }
    }
}
