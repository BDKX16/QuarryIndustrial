package org.example.pz.quarryindustrial.mixin.ala;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.filter.PipeFilterData;
import org.example.pz.quarryindustrial.filter.PipeFilterHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;

/** Le da al caño de ítems de Ala un filtro opcional por cara, que se guarda con el resto de sus datos. */
@Pseudo
@Mixin(targets = "dev.alaindustrial.block.entity.ItemPipeBlockEntity")
public abstract class ItemPipeBlockEntityMixin implements PipeFilterHolder {
    @Unique
    private static final Codec<Map<Direction, PipeFilterData>> FILTERS =
            Codec.unboundedMap(Direction.CODEC, PipeFilterData.CODEC);

    @Unique
    private final Map<Direction, PipeFilterData> quarryindustrial$filters = new EnumMap<>(Direction.class);

    @Override
    public @Nullable PipeFilterData quarry$getFilter(Direction face) {
        return quarryindustrial$filters.get(face);
    }

    @Override
    public void quarry$setFilter(Direction face, @Nullable PipeFilterData data) {
        if (data == null) {
            quarryindustrial$filters.remove(face);
        } else {
            quarryindustrial$filters.put(face, data);
            data.setListener(((BlockEntity) (Object) this)::setChanged);
        }
        BlockEntity self = (BlockEntity) (Object) this;
        self.setChanged();
        // El caño de Ala manda al cliente todo lo que guarda: se lo repite para que dibuje (o quite) el filtro
        Level level = self.getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(self.getBlockPos(), self.getBlockState(), self.getBlockState(), 3);
        }
    }

    @Override
    public Map<Direction, PipeFilterData> quarry$filters() {
        return quarryindustrial$filters;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), require = 0)
    private void quarryindustrial$save(ValueOutput output, CallbackInfo ci) {
        if (!quarryindustrial$filters.isEmpty()) {
            output.store("QuarryFilters", FILTERS, quarryindustrial$filters);
        }
    }

    @Inject(method = "loadMachineData", at = @At("TAIL"), require = 0)
    private void quarryindustrial$load(ValueInput input, CallbackInfo ci) {
        quarryindustrial$filters.clear();
        input.read("QuarryFilters", FILTERS).ifPresent(map -> map.forEach((face, data) -> {
            quarryindustrial$filters.put(face, data);
            data.setListener(((BlockEntity) (Object) this)::setChanged);
        }));
    }
}
