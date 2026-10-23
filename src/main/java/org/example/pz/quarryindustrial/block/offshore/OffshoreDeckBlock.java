package org.example.pz.quarryindustrial.block.offshore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cubierta de rejilla de la plataforma (se ve el mar a través). Se camina encima; por cualquier bloque de la
 * cubierta sale el petróleo y entra la energía. Romper uno desarma la plataforma entera.
 */
public class OffshoreDeckBlock extends BaseEntityBlock {
    public OffshoreDeckBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OffshoreDeckBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof OffshoreDeckBlockEntity deck && deck.controller() != null) {
            return OffshoreRigBlock.use(stack, level, deck.controller(), player, hand);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof OffshoreDeckBlockEntity deck && deck.controller() != null) {
            return OffshoreRigBlock.showStatus(level, deck.controller(), player);
        }
        return InteractionResult.PASS;
    }
}
