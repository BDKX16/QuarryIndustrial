package org.example.pz.quarryindustrial.client.compat;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.world.item.ItemStack;

/** REI: igual que en JEI, una página de información por máquina. */
public class ReiCompat implements REIClientPlugin {
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (MachineGuide.Entry entry : MachineGuide.entries()) {
            registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(entry.item()),
                    new ItemStack(entry.item()).getHoverName())
                    .line(entry.text()));
        }
    }
}
