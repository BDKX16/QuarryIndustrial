package org.example.pz.quarryindustrial.block.quarry;

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
import org.example.pz.quarryindustrial.item.UpgradeItem;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public class QuarryBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final QuarryTier tier;

    public QuarryBlock(QuarryTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public QuarryTier tier() {
        return tier;
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
            quarry.setOwner(player.getUUID());
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuarryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            // En el cliente solo se interpola la posición de los cabezales para el renderer
            return (lvl, pos, st, be) -> {
                if (be instanceof QuarryBlockEntity quarry) {
                    QuarryBlockEntity.clientTick(lvl, pos, st, quarry);
                }
            };
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof QuarryBlockEntity quarry) {
                QuarryBlockEntity.serverTick(lvl, pos, st, quarry);
            }
        };
    }

    /** Clic derecho con un upgrade en la mano: lo instala. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof UpgradeItem upgrade)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
            if (quarry.installUpgrade(upgrade.type())) {
                stack.consume(1, player);
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.upgrade.installed",
                        stack.getHoverName()));
            } else {
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.upgrade.rejected"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Clic derecho sin item: abre la GUI. Agachado: saca el último upgrade instalado. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            UpgradeType removed = quarry.removeLastUpgrade();
            if (removed != null) {
                // Lo mete en el inventario o, si está lleno, lo tira a los pies del jugador
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.upgrade(removed)),
                        Prediction.SERVER_ONLY);
            }
            return InteractionResult.SUCCESS;
        }
        player.openMenu(quarry);
        return InteractionResult.SUCCESS;
    }
}
