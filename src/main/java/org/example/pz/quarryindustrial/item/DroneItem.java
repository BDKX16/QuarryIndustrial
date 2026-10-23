package org.example.pz.quarryindustrial.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.example.pz.quarryindustrial.drone.DroneKind;

import java.util.function.Consumer;

/** Un dron: se pone en un pad (clic derecho) y trabaja desde ahí. */
public class DroneItem extends Item {
    private final DroneKind kind;

    public DroneItem(DroneKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public DroneKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.quarryindustrial.drone." + kind.id())
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(kind.needsLink() ? "tooltip.quarryindustrial.drone.needs_link"
                : "tooltip.quarryindustrial.drone.works_alone").withStyle(ChatFormatting.DARK_GRAY));
    }
}
