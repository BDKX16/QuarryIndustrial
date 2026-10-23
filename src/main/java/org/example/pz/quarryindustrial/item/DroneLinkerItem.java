package org.example.pz.quarryindustrial.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.registry.ModComponents;

import java.util.function.Consumer;

/**
 * Vinculador de drones. Agachado + clic en un pad: lo elige como origen. Clic en otro pad: el dron del origen
 * va a llevar la carga a ese pad. Agachado + clic en el mismo origen: le borra el destino.
 */
public class DroneLinkerItem extends Item {
    public DroneLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof DronePadBlockEntity pad)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos source = stack.get(ModComponents.LINK_SOURCE);
        if (player.isShiftKeyDown()) {
            if (pos.equals(source)) {
                pad.setLink(null);
                stack.remove(ModComponents.LINK_SOURCE);
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.linker.cleared"));
            } else {
                stack.set(ModComponents.LINK_SOURCE, pos.immutable());
                player.sendOverlayMessage(Component.translatable("message.quarryindustrial.linker.source",
                        pos.getX(), pos.getY(), pos.getZ()));
            }
            return InteractionResult.SUCCESS;
        }
        if (source == null) {
            player.sendOverlayMessage(Component.translatable("message.quarryindustrial.linker.no_source"));
            return InteractionResult.SUCCESS;
        }
        int max = QuarryConfig.get().drones.maxLinkDistance;
        if (source.equals(pos)) {
            player.sendOverlayMessage(Component.translatable("message.quarryindustrial.linker.same"));
        } else if (source.distSqr(pos) > (double) max * max) {
            player.sendOverlayMessage(Component.translatable("message.quarryindustrial.linker.too_far", max));
        } else if (level.getBlockEntity(source) instanceof DronePadBlockEntity sourcePad) {
            sourcePad.setLink(pos.immutable());
            stack.remove(ModComponents.LINK_SOURCE);
            player.sendOverlayMessage(Component.translatable("message.quarryindustrial.linker.linked",
                    source.getX(), source.getY(), source.getZ(), pos.getX(), pos.getY(), pos.getZ(),
                    (int) Math.sqrt(source.distSqr(pos))));
        } else {
            stack.remove(ModComponents.LINK_SOURCE);
            player.sendOverlayMessage(Component.translatable("message.quarryindustrial.linker.source_gone"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        BlockPos source = stack.get(ModComponents.LINK_SOURCE);
        if (source != null) {
            tooltip.accept(Component.translatable("tooltip.quarryindustrial.linker.source", source.getX(),
                    source.getY(), source.getZ()).withStyle(ChatFormatting.YELLOW));
        }
        tooltip.accept(Component.translatable("tooltip.quarryindustrial.linker.help").withStyle(ChatFormatting.GRAY));
    }
}
