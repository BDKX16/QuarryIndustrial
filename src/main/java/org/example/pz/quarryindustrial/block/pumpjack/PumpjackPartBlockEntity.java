package org.example.pz.quarryindustrial.block.pumpjack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/** Recuerda a qué controlador pertenece la parte. */
public class PumpjackPartBlockEntity extends BlockEntity {
    private @Nullable BlockPos controller;

    public PumpjackPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMPJACK_PART, pos, state);
    }

    public @Nullable BlockPos controller() {
        return controller;
    }

    public void setController(BlockPos controller) {
        this.controller = controller;
        setChanged();
    }

    /** El controlador vivo, o null si la parte quedó huérfana. */
    public @Nullable PumpjackBlockEntity controllerEntity() {
        if (controller == null || level == null) {
            return null;
        }
        return level.getBlockEntity(controller) instanceof PumpjackBlockEntity pumpjack ? pumpjack : null;
    }

    /** Romper una parte desarma toda la cigüeña: rompe el controlador, que suelta el item. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        PumpjackBlockEntity pumpjack = controllerEntity();
        if (pumpjack != null && !pumpjack.isDismantling() && level != null && !level.isClientSide()) {
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
