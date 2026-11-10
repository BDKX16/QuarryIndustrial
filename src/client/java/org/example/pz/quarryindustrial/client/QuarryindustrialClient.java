package org.example.pz.quarryindustrial.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.client.render.PipeFilterRenderer;
import org.example.pz.quarryindustrial.ClientEffects;
import org.example.pz.quarryindustrial.block.drone.DroneConsoleBlock;
import org.example.pz.quarryindustrial.item.DroneLinkerItem;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.client.manual.ManualScreen;
import org.example.pz.quarryindustrial.item.ManualItem;
import org.example.pz.quarryindustrial.client.render.DronePadRenderer;
import org.example.pz.quarryindustrial.client.render.DroneRenderer;
import org.example.pz.quarryindustrial.client.render.OffshoreRenderer;
import org.example.pz.quarryindustrial.client.render.PumpjackRenderer;
import org.example.pz.quarryindustrial.client.render.QuarryRenderer;
import org.example.pz.quarryindustrial.client.screen.DroneConsoleScreen;
import org.example.pz.quarryindustrial.client.screen.InfoScreen;
import org.example.pz.quarryindustrial.client.screen.PipeFilterScreen;
import org.example.pz.quarryindustrial.client.screen.QuarryScreen;
import org.example.pz.quarryindustrial.client.sound.MachineSounds;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModEntities;
import org.example.pz.quarryindustrial.network.ConsolePayloads;
import org.example.pz.quarryindustrial.registry.ModMenus;

public class QuarryindustrialClient implements ClientModInitializer {

    /**
     * El caño de ítems de Ala no tiene renderer: se le suma el del filtro. Su tipo de block entity se busca por el
     * bloque ({@code alaindustrial:item_pipe}) para no depender de cómo lo registre Ala.
     */
    @SuppressWarnings("unchecked")
    private static void registerPipeFilterRenderer() {
        var block = BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath("alaindustrial", "item_pipe"));
        if (block.isEmpty()) {
            return;
        }
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            if (type.isValid(block.get().defaultBlockState())) {
                BlockEntityRenderers.register((BlockEntityType<BlockEntity>) type, PipeFilterRenderer::new);
                return;
            }
        }
    }

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.QUARRY, QuarryRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PUMPJACK, PumpjackRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.OFFSHORE_RIG, OffshoreRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DRONE_PAD, DronePadRenderer::new);
        EntityRendererRegistry.register(ModEntities.DRONE, DroneRenderer::new);
        registerPipeFilterRenderer();
        QuarryBlockEntity.clientTickHook = MachineSounds::tickQuarry;
        ClientEffects.machineTick = MachineSounds::tickMachine;
        ClientEffects.pumpjackStroke = MachineSounds::pumpjackStroke;
        ClientTickEvents.END_CLIENT_TICK.register(MachineSounds::tickDrones);
        ExploredTerrain.init();
        MenuScreens.register(ModMenus.QUARRY, QuarryScreen::new);
        MenuScreens.register(ModMenus.INFO, InfoScreen::new);
        MenuScreens.register(ModMenus.PIPE_FILTER, PipeFilterScreen::new);
        ManualItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new ManualScreen());
        DroneConsoleBlock.openScreen = pos -> Minecraft.getInstance().gui.setScreen(new DroneConsoleScreen(pos));
        DroneLinkerItem.openConsole = pos -> Minecraft.getInstance().gui.setScreen(new DroneConsoleScreen(pos, true));
        ClientPlayNetworking.registerGlobalReceiver(ConsolePayloads.Snapshot.TYPE, (payload, context) -> {
            if (context.client().gui.screen() instanceof DroneConsoleScreen screen) {
                screen.accept(payload);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(ConsolePayloads.Notice.TYPE, (payload, context) -> {
            if (context.client().gui.screen() instanceof DroneConsoleScreen screen) {
                screen.accept(payload);
            }
        });
    }
}
