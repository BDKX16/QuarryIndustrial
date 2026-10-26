package org.example.pz.quarryindustrial.block.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;

/**
 * Centralita de drones: clic derecho abre el mapa con los pads, los drones en vuelo y los géiseres encontrados,
 * y la lista para vincular pads sin ir hasta ellos. No guarda nada: lo que muestra sale de
 * {@link org.example.pz.quarryindustrial.drone.DroneNetwork}.
 */
public class DroneConsoleBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Lo engancha el cliente para abrir la pantalla; en el servidor no hace nada. */
    public static Consumer<BlockPos> openScreen = pos -> {
    };

    public DroneConsoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            openScreen.accept(pos.immutable());
        }
        return InteractionResult.SUCCESS;
    }
}
