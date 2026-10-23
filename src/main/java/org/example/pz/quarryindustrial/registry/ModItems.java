package org.example.pz.quarryindustrial.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.item.UpgradeItem;
import org.example.pz.quarryindustrial.item.UpgradeType;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    private static final Map<UpgradeType, UpgradeItem> UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        for (UpgradeType type : UpgradeType.values()) {
            String name = "upgrade_" + type.name().toLowerCase();
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Quarryindustrial.id(name));
            UPGRADES.put(type, Registry.register(BuiltInRegistries.ITEM, key,
                    new UpgradeItem(type, new Item.Properties().setId(key).stacksTo(16))));
        }
    }

    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Quarryindustrial.id("main"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.quarryindustrial.main"))
                    .icon(() -> new ItemStack(ModBlocks.QUARRY_HV))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.QUARRY_LV);
                        output.accept(ModBlocks.QUARRY_MV);
                        output.accept(ModBlocks.QUARRY_HV);
                        output.accept(ModBlocks.QUARRY_CORE);
                        output.accept(ModBlocks.QUARRY_CASING);
                        output.accept(ModBlocks.ENERGY_PORT);
                        output.accept(ModBlocks.PUMPJACK);
                        UPGRADES.values().forEach(output::accept);
                    })
                    .build());

    private ModItems() {
    }

    public static UpgradeItem upgrade(UpgradeType type) {
        return UPGRADES.get(type);
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
