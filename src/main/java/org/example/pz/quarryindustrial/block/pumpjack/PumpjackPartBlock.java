package org.example.pz.quarryindustrial.block.pumpjack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Parte invisible de la cigüeña. Reserva el volumen (nada se puede poner donde se mueve el balancín) y
 * reenvía al controlador la energía, el fluido y los clics. Las de la base son el patín: se pisan y se
 * pueden romper; romper cualquiera desarma la cigüeña entera. Las de arriba no tienen colisión ni contorno.
 */
public class PumpjackPartBlock extends BaseEntityBlock {
    public static final BooleanProperty BASE = BooleanProperty.create("base");
    private static final VoxelShape SKID = Block.box(0, 0, 0, 16, 4, 16);

    public PumpjackPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BASE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BASE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BASE) ? SKID : Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpjackPartBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof PumpjackPartBlockEntity part && part.controller() != null) {
            return PumpjackBlock.useUpgrade(stack, level, part.controller(), player);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof PumpjackPartBlockEntity part && part.controller() != null) {
            return PumpjackBlock.showStatus(level, part.controller(), player);
        }
        return InteractionResult.PASS;
    }
}
