package org.example.pz.quarryindustrial.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.client.render.PumpjackRenderer;
import org.example.pz.quarryindustrial.client.render.QuarryRenderer;
import org.example.pz.quarryindustrial.client.screen.QuarryScreen;
import org.example.pz.quarryindustrial.client.sound.QuarrySounds;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModMenus;

public class QuarryindustrialClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.QUARRY, QuarryRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PUMPJACK, PumpjackRenderer::new);
        QuarryBlockEntity.clientTickHook = QuarrySounds::tick;
        MenuScreens.register(ModMenus.QUARRY, QuarryScreen::new);
    }
}
