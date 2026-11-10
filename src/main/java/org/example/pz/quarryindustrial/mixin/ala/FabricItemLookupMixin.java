package org.example.pz.quarryindustrial.mixin.ala;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.example.pz.quarryindustrial.filter.FilteredItemStorage;
import org.example.pz.quarryindustrial.filter.PipeFilterData;
import org.example.pz.quarryindustrial.filter.PipeFilterHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * La red de ítems de Ala pide el inventario de cada extremo con {@code find(nivel, posición del inventario, cara del
 * inventario que mira al caño)}. Si el caño de esa cara tiene un filtro, el inventario se entrega envuelto en él:
 * así filtra tanto lo que el caño saca como lo que mete.
 */
@Pseudo
@Mixin(targets = "dev.alaindustrial.core.fabric.FabricItemLookup")
public abstract class FabricItemLookupMixin {
    /** Qué inventario se está pidiendo: el HEAD de find lo anota y el ModifyArg (en la misma llamada) lo usa. */
    @Unique
    private static final ThreadLocal<Object[]> QUARRY_REQUEST = new ThreadLocal<>();

    @Inject(method = "find", at = @At("HEAD"), require = 0)
    private void quarryindustrial$request(Level level, BlockPos pos, Direction side, CallbackInfoReturnable<?> cir) {
        QUARRY_REQUEST.set(new Object[]{level, pos, side});
    }

    @Inject(method = "find", at = @At("RETURN"), require = 0)
    private void quarryindustrial$done(Level level, BlockPos pos, Direction side, CallbackInfoReturnable<?> cir) {
        QUARRY_REQUEST.remove();
    }

    @ModifyArg(method = "find",
            at = @At(value = "INVOKE",
                    target = "Ldev/alaindustrial/core/fabric/FabricItemPort;of(Lnet/fabricmc/fabric/api/transfer/v1/storage/Storage;)Ldev/alaindustrial/core/item/ItemPort;"),
            index = 0, require = 0)
    private Storage<ItemVariant> quarryindustrial$filter(Storage<ItemVariant> storage) {
        Object[] request = QUARRY_REQUEST.get();
        if (storage == null || request == null) {
            return storage;
        }
        Level level = (Level) request[0];
        BlockPos pos = (BlockPos) request[1];
        Direction side = (Direction) request[2];
        BlockEntity pipe = level.getBlockEntity(pos.relative(side));
        if (pipe instanceof PipeFilterHolder holder) {
            PipeFilterData filter = holder.quarry$getFilter(side.getOpposite());
            if (filter != null) {
                return new FilteredItemStorage(storage, filter);
            }
        }
        return storage;
    }
}
