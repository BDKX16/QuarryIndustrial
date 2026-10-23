package org.example.pz.quarryindustrial.block.offshore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/** Recuerda a qué plataforma pertenece el bloque de cubierta. */
public class OffshoreDeckBlockEntity extends BlockEntity {
    private @Nullable BlockPos controller;

    public OffshoreDeckBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OFFSHORE_DECK, pos, state);
    }

    public @Nullable BlockPos controller() {
        return controller;
    }

    public void setController(BlockPos controller) {
        this.controller = controller;
        setChanged();
    }

    public @Nullable OffshoreRigBlockEntity controllerEntity() {
        if (controller == null || level == null) {
            return null;
        }
        return level.getBlockEntity(controller) instanceof OffshoreRigBlockEntity rig ? rig : null;
    }

    /** Romper la cubierta desarma la plataforma: rompe el controlador, que suelta el item. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        OffshoreRigBlockEntity rig = controllerEntity();
        if (rig != null && !rig.isDismantling() && level != null && !level.isClientSide()) {
            level.destroyBlock(controller, true);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (controller != null) {
            output.store("Controller", BlockPos.CODEC, controller);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        controller = input.read("Controller", BlockPos.CODEC).orElse(null);
    }
}
