package org.example.pz.quarryindustrial.block.splitter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;

/** Reparte la energía que le llega a partes iguales entre todos los cables y máquinas pegados. */
public class EnergySplitterBlock extends BaseEntityBlock {
    public EnergySplitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergySplitterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.ENERGY_SPLITTER) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof EnergySplitterBlockEntity splitter && lvl instanceof ServerLevel server) {
                splitter.serverTick(server, pos);
            }
        };
    }
}
