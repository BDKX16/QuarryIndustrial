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
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.item.DrillBitItem;
import org.example.pz.quarryindustrial.item.DroneItem;
import org.example.pz.quarryindustrial.item.DroneLinkerItem;
import org.example.pz.quarryindustrial.item.SurveyReportItem;
import org.example.pz.quarryindustrial.item.ManualItem;
import org.example.pz.quarryindustrial.item.UpgradeItem;
import org.example.pz.quarryindustrial.item.UpgradeType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    private static final Map<UpgradeType, UpgradeItem> UPGRADES = new EnumMap<>(UpgradeType.class);
    private static final Map<BitTier, DrillBitItem> BITS = new EnumMap<>(BitTier.class);

    static {
        for (UpgradeType type : UpgradeType.values()) {
            String name = "upgrade_" + type.name().toLowerCase();
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Quarryindustrial.id(name));
            UPGRADES.put(type, Registry.register(BuiltInRegistries.ITEM, key,
                    new UpgradeItem(type, new Item.Properties().setId(key).stacksTo(16))));
        }
        for (BitTier tier : BitTier.values()) {
            String name = "drill_bit_" + tier.name().toLowerCase();
            Item.Properties properties = new Item.Properties().durability(tier.settings().durability);
            if (tier == BitTier.NETHERITE || tier == BitTier.PDC) {
                properties.fireResistant();
            }
            BITS.put(tier, register(name, p -> new DrillBitItem(tier, p), properties));
        }
    }

    private static final Map<DroneKind, DroneItem> DRONES = new EnumMap<>(DroneKind.class);

    static {
        for (DroneKind kind : DroneKind.values()) {
            DRONES.put(kind, register(kind.id() + "_drone", p -> new DroneItem(kind, p),
                    new Item.Properties().stacksTo(1)));
        }
    }

    public static final DroneLinkerItem DRONE_LINKER = register("drone_linker", DroneLinkerItem::new,
            new Item.Properties().stacksTo(1));
    public static final Item PAD_CHUNK_LOADER = register("pad_chunk_loader", Item::new,
            new Item.Properties().stacksTo(16));
    public static final Item SURVEY_RANGE_UPGRADE = register("survey_range_upgrade", Item::new,
            new Item.Properties().stacksTo(16));
    public static final SurveyReportItem SURVEY_REPORT = register("survey_report", SurveyReportItem::new,
            new Item.Properties().stacksTo(1));
    public static final ManualItem MANUAL = register("manual", ManualItem::new, new Item.Properties().stacksTo(1));

    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Quarryindustrial.id("main"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.quarryindustrial.main"))
                    .icon(() -> new ItemStack(ModBlocks.QUARRY_HV))
                    .displayItems((parameters, output) -> {
                        output.accept(MANUAL);
                        output.accept(ModBlocks.QUARRY_LV);
                        output.accept(ModBlocks.QUARRY_MV);
                        output.accept(ModBlocks.QUARRY_HV);
                        output.accept(ModBlocks.QUARRY_CORE);
                        output.accept(ModBlocks.QUARRY_CASING);
                        output.accept(ModBlocks.ENERGY_PORT);
                        output.accept(ModBlocks.PUMPJACK);
                        output.accept(ModBlocks.OFFSHORE_RIG);
                        output.accept(ModBlocks.DIESEL_GENERATOR);
                        output.accept(ModBlocks.STEAM_TURBINE);
                        output.accept(ModBlocks.DRONE_PAD);
                        output.accept(ModBlocks.DRONE_CONSOLE);
                        output.accept(DRONE_LINKER);
                        output.accept(PAD_CHUNK_LOADER);
                        output.accept(SURVEY_RANGE_UPGRADE);
                        output.accept(ModBlocks.RADAR);
                        DRONES.values().forEach(output::accept);
                        BITS.values().forEach(output::accept);
                        UPGRADES.values().forEach(output::accept);
                    })
                    .build());

    private ModItems() {
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory,
                                               Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Quarryindustrial.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    public static DroneItem drone(DroneKind kind) {
        return DRONES.get(kind);
    }

    public static DrillBitItem bit(BitTier tier) {
        return BITS.get(tier);
    }

    public static UpgradeItem upgrade(UpgradeType type) {
        return UPGRADES.get(type);
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
