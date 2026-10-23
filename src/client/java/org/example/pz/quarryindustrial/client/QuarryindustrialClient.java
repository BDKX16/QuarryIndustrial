package org.example.pz.quarryindustrial.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import org.example.pz.quarryindustrial.ClientEffects;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.client.manual.ManualScreen;
import org.example.pz.quarryindustrial.item.ManualItem;
import org.example.pz.quarryindustrial.client.render.DronePadRenderer;
import org.example.pz.quarryindustrial.client.render.DroneRenderer;
import org.example.pz.quarryindustrial.client.render.OffshoreRenderer;
import org.example.pz.quarryindustrial.client.render.PumpjackRenderer;
import org.example.pz.quarryindustrial.client.render.QuarryRenderer;
import org.example.pz.quarryindustrial.client.screen.QuarryScreen;
import org.example.pz.quarryindustrial.client.sound.MachineSounds;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModEntities;
import org.example.pz.quarryindustrial.registry.ModMenus;

public class QuarryindustrialClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.QUARRY, QuarryRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PUMPJACK, PumpjackRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.OFFSHORE_RIG, OffshoreRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DRONE_PAD, DronePadRenderer::new);
        EntityRendererRegistry.register(ModEntities.DRONE, DroneRenderer::new);
        QuarryBlockEntity.clientTickHook = MachineSounds::tickQuarry;
        ClientEffects.machineTick = MachineSounds::tickMachine;
        ClientEffects.pumpjackStroke = MachineSounds::pumpjackStroke;
        ClientTickEvents.END_CLIENT_TICK.register(MachineSounds::tickDrones);
        MenuScreens.register(ModMenus.QUARRY, QuarryScreen::new);
        ManualItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new ManualScreen());
    }
}
