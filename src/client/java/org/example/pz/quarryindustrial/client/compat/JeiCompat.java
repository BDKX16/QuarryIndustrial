package org.example.pz.quarryindustrial.client.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.Quarryindustrial;

/** JEI: las recetas (todas de mesa de trabajo) las muestra solo; acá se suman las páginas de información. */
@JeiPlugin
public class JeiCompat implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Quarryindustrial.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (MachineGuide.Entry entry : MachineGuide.entries()) {
            registration.addItemStackInfo(new ItemStack(entry.item()), entry.text());
        }
    }
}
