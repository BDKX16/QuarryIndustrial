package org.example.pz.quarryindustrial.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.example.pz.quarryindustrial.filter.PipeFilterData;
import org.example.pz.quarryindustrial.filter.PipeFilterHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Al romperse un caño de Ala, los filtros que tenía puestos caen como ítems (con su configuración). El caño de Ala no
 * sobreescribe este método, por eso el mixin va en la clase base.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
    private void quarryindustrial$dropFilters(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this instanceof PipeFilterHolder holder) {
            Level level = ((BlockEntity) (Object) this).getLevel();
            if (level != null && !level.isClientSide()) {
                for (PipeFilterData data : holder.quarry$filters().values()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, data.toStack());
                }
                holder.quarry$filters().clear();
            }
        }
    }
}
