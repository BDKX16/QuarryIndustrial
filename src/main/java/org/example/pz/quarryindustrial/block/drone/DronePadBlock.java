package org.example.pz.quarryindustrial.block.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.DroneItem;
import org.example.pz.quarryindustrial.registry.ModItems;

/**
 * Pad de drones: una plataforma baja con una "H". Clic derecho con un dron lo pone; agachado y con la mano
 * vacía lo saca; con la mano vacía muestra el estado (y manda a explorar al dron explorador). Al romperlo
 * suelta el dron y lo que haya en los buzones.
 */
public class DronePadBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public DronePadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DronePadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof DronePadBlockEntity pad) {
                pad.serverTick(lvl, pos);
            }
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.PAD_CHUNK_LOADER)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DronePadBlockEntity pad) {
                if (pad.installChunkLoader()) {
                    stack.consume(1, player);
                    player.sendOverlayMessage(Component.translatable("message.quarryindustrial.drone_pad.chunk_loader"));
                } else {
                    player.sendOverlayMessage(
                            Component.translatable("message.quarryindustrial.drone_pad.chunk_loader.already"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.SURVEY_RANGE_UPGRADE)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DronePadBlockEntity pad) {
                if (pad.installSurveyUpgrade()) {
                    stack.consume(1, player);
                    player.sendOverlayMessage(Component.translatable("message.quarryindustrial.drone_pad.survey_upgrade",
                            pad.surveyUpgrades(), QuarryConfig.get().drones.surveyRangeMax));
                } else {
                    player.sendOverlayMessage(
                            Component.translatable("message.quarryindustrial.drone_pad.survey_upgrade.full"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (!(stack.getItem() instanceof DroneItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DronePadBlockEntity pad) {
            if (pad.insertDrone(stack.copyWithCount(1))) {
                stack.consume(1, player);
            } else {
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.drone_pad.occupied"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof DronePadBlockEntity pad)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            ItemStack drone = pad.takeDrone();
            if (!drone.isEmpty()) {
                player.getInventory().placeItemBackInInventory(drone, Prediction.SERVER_ONLY);
            }
            return InteractionResult.SUCCESS;
        }
        player.openMenu(pad);
        return InteractionResult.SUCCESS;
    }
}
