package org.example.pz.quarryindustrial;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.example.pz.quarryindustrial.registry.ModMenus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Quarryindustrial implements ModInitializer {
    public static final String MOD_ID = "quarryindustrial";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        QuarryConfig.load();
        // El orden importa: los block entities y la pestaña creativa necesitan que los bloques ya existan
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModMenus.init();
    }
}
