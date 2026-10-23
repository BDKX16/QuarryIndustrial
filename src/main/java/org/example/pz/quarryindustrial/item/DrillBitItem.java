package org.example.pz.quarryindustrial.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Broca de quarry. Se gasta con cada bloque minado (más con los bloques duros, la mitad con refrigerante)
 * y al llegar al final de su vida se rompe. Se instala en la GUI, con clic derecho sobre la quarry o con
 * un dron de mantenimiento.
 */
public class DrillBitItem extends Item {
    private final BitTier tier;

    public DrillBitItem(BitTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public BitTier tier() {
        return tier;
    }

    /** Desgaste de 0 (nueva) a 1 (rota). */
    public static float wear(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? 0 : Math.clamp(stack.getDamageValue() / (float) max, 0, 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.quarryindustrial.bit.speed",
                Math.round(tier.settings().speedMultiplier * 100)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.quarryindustrial.bit.life",
                stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
    }
}
