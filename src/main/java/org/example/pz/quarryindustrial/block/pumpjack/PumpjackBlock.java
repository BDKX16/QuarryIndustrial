package org.example.pz.quarryindustrial.block.pumpjack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.example.pz.quarryindustrial.item.UpgradeItem;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * Cabeza de pozo de la cigüeña, que además es el controlador. Se coloca sobre el agujero del géiser,
 * mirando hacia él: el resto de la cigüeña (3×7×5) se arma hacia atrás, del lado del jugador. Si no hay
 * lugar libre para toda la estructura, no se coloca.
 * El modelo completo lo dibuja el renderer; el bloque en sí es invisible.
 */
public class PumpjackBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 12, 12);

    public PumpjackBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Level level = context.getLevel();
        for (BlockPos part : PumpjackLayout.partPositions(context.getClickedPos(), facing)) {
            if (!level.isInsideBuildHeight(part.getY()) || !level.getBlockState(part).canBeReplaced()) {
                if (context.getPlayer() != null && !level.isClientSide()) {
                    context.getPlayer().sendOverlayMessage(
                            Component.translatable("message.quarryindustrial.pumpjack.no_room"));
                }
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    /** Arma las partes invisibles que reservan el volumen de la cigüeña. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            build(level, pos, state.getValue(FACING));
        }
    }

    public static void build(Level level, BlockPos pos, Direction facing) {
        for (BlockPos part : PumpjackLayout.partPositions(pos, facing)) {
            boolean base = part.getY() == pos.getY();
            level.setBlock(part, ModBlocks.PUMPJACK_PART.defaultBlockState().setValue(PumpjackPartBlock.BASE, base),
                    Block.UPDATE_ALL);
            if (level.getBlockEntity(part) instanceof PumpjackPartBlockEntity partBe) {
                partBe.setController(pos);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpjackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof PumpjackBlockEntity pumpjack) {
                    PumpjackBlockEntity.clientTick(lvl, pos, st, pumpjack);
                }
            };
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof PumpjackBlockEntity pumpjack) {
                PumpjackBlockEntity.serverTick(lvl, pos, st, pumpjack);
            }
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return useUpgrade(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return showStatus(level, pos, player);
    }

    /** Clic derecho con un upgrade de velocidad (en la cabeza o en cualquier parte): lo instala. */
    static InteractionResult useUpgrade(ItemStack stack, Level level, BlockPos controller, Player player) {
        if (!(stack.getItem() instanceof UpgradeItem upgrade)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(controller) instanceof PumpjackBlockEntity pumpjack) {
            if (pumpjack.installUpgrade(upgrade.type())) {
                stack.consume(1, player);
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.upgrade.installed",
                        stack.getHoverName()));
            } else {
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.pumpjack.upgrade_rejected"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Clic derecho sin item: estado. Agachado: saca un upgrade. */
    static InteractionResult showStatus(Level level, BlockPos controller, Player player) {
        if (level.isClientSide() || !(level.getBlockEntity(controller) instanceof PumpjackBlockEntity pumpjack)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (pumpjack.removeUpgrade()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.upgrade(UpgradeType.SPEED)),
                        Prediction.SERVER_ONLY);
            }
            return InteractionResult.SUCCESS;
        }
        player.sendOverlayMessage(Component.translatable("message.quarryindustrial.pumpjack.status",
                pumpjack.status().label(), pumpjack.energy.getAmount(), pumpjack.energy.getCapacity(),
                pumpjack.oilMb(), pumpjack.tankCapacityMb(), pumpjack.speedUpgrades(),
                pumpjack.fuel.amountMb(), pumpjack.fuel.capacityMb()));
        return InteractionResult.SUCCESS;
    }
}
