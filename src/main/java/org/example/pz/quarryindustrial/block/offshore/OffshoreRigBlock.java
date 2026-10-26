package org.example.pz.quarryindustrial.block.offshore;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.BlockHitResult;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.DrillBitItem;
import org.example.pz.quarryindustrial.item.UpgradeItem;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * Controlador de la plataforma offshore: la mesa rotativa en el centro de la cubierta. Se coloca sobre el
 * agua (como un nenúfar); si el lugar sirve, arma la cubierta de 7×7 y baja las cuatro patas hasta el fondo.
 */
public class OffshoreRigBlock extends BaseEntityBlock {
    public OffshoreRigBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        OffshoreLayout.Problem problem = OffshoreLayout.check(level, context.getClickedPos());
        if (problem != null) {
            if (context.getPlayer() != null && !level.isClientSide()) {
                // Los mensajes usan el lado de la cubierta y la profundidad mínima
                context.getPlayer().sendOverlayMessage(Component.translatable(
                        "message.quarryindustrial.offshore." + problem.name().toLowerCase(),
                        OffshoreLayout.HALF * 2 + 1, QuarryConfig.get().offshore.minWaterDepth));
            }
            return null;
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            build(level, pos);
        }
    }

    /** Arma la cubierta y las patas. */
    public static void build(Level level, BlockPos pos) {
        for (BlockPos deck : OffshoreLayout.deckPositions(pos)) {
            level.setBlock(deck, ModBlocks.OFFSHORE_DECK.defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(deck) instanceof OffshoreDeckBlockEntity part) {
                part.setController(pos);
            }
        }
        for (BlockPos corner : OffshoreLayout.corners(pos)) {
            int depth = OffshoreLayout.waterDepth(level, corner);
            for (int i = 1; i <= depth; i++) {
                level.setBlock(corner.below(i), ModBlocks.OFFSHORE_LEG.defaultBlockState()
                        .setValue(OffshoreLegBlock.WATERLOGGED, true), Block.UPDATE_ALL);
            }
        }
        if (level.getBlockEntity(pos) instanceof OffshoreRigBlockEntity rig) {
            rig.onBuilt();
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OffshoreRigBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof OffshoreRigBlockEntity rig) {
                    rig.clientTick(lvl, pos);
                }
            };
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof OffshoreRigBlockEntity rig) {
                rig.serverTick(lvl, pos);
            }
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return use(stack, level, pos, player, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return showStatus(level, pos, player);
    }

    /**
     * Clic derecho (en el controlador o en cualquier bloque de la cubierta): con una broca la instala, con
     * un upgrade de velocidad lo pone, con un balde de combustible lo carga.
     */
    static InteractionResult use(ItemStack stack, Level level, BlockPos controller, Player player,
                                 InteractionHand hand) {
        if (!(level.getBlockEntity(controller) instanceof OffshoreRigBlockEntity rig)) {
            return InteractionResult.PASS;
        }
        if (stack.getItem() instanceof DrillBitItem) {
            if (!level.isClientSide()) {
                ItemStack previous = rig.swapBit(stack.split(1));
                if (!previous.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(previous, Prediction.SERVER_ONLY);
                }
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.bit.installed",
                        rig.bit().getHoverName()));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof UpgradeItem upgrade) {
            if (!level.isClientSide()) {
                if (upgrade.type() == UpgradeType.SPEED && rig.installSpeedUpgrade()) {
                    stack.consume(1, player);
                    player.sendOverlayMessage(Component.translatable("message.quarryindustrial.upgrade.installed",
                            stack.getHoverName()));
                } else {
                    player.sendOverlayMessage(Component.translatable("message.quarryindustrial.pumpjack.upgrade_rejected"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (FluidStorageUtil.interactWithFluidStorage(rig.fuelInput(), player, hand)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Sin item: estado. Agachado: saca un upgrade de velocidad. */
    static InteractionResult showStatus(Level level, BlockPos controller, Player player) {
        if (level.isClientSide() || !(level.getBlockEntity(controller) instanceof OffshoreRigBlockEntity rig)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (rig.removeSpeedUpgrade()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.upgrade(UpgradeType.SPEED)),
                        Prediction.SERVER_ONLY);
            }
            return InteractionResult.SUCCESS;
        }
        player.openMenu(rig);
        return InteractionResult.SUCCESS;
    }
}
