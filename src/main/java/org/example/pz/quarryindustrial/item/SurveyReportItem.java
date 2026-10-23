package org.example.pz.quarryindustrial.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.example.pz.quarryindustrial.registry.ModComponents;

import java.util.List;
import java.util.function.Consumer;

/** Informe del dron explorador: las coordenadas de los géiseres de petróleo que vio desde el aire. */
public class SurveyReportItem extends Item {
    public static final int MAX_SHOWN = 8;

    public SurveyReportItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        List<BlockPos> sites = stack.getOrDefault(ModComponents.SURVEY_SITES, List.of());
        if (sites.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.quarryindustrial.survey.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.quarryindustrial.survey.found", sites.size())
                .withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < Math.min(MAX_SHOWN, sites.size()); i++) {
            BlockPos site = sites.get(i);
            tooltip.accept(Component.translatable("tooltip.quarryindustrial.survey.site", site.getX(), site.getY(),
                    site.getZ()).withStyle(ChatFormatting.GRAY));
        }
        if (sites.size() > MAX_SHOWN) {
            tooltip.accept(Component.translatable("tooltip.quarryindustrial.survey.more", sites.size() - MAX_SHOWN)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
