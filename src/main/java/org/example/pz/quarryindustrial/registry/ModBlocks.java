package org.example.pz.quarryindustrial.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.drone.DroneConsoleBlock;
import org.example.pz.quarryindustrial.block.drone.RadarBlock;
import org.example.pz.quarryindustrial.block.drone.DronePadBlock;
import org.example.pz.quarryindustrial.block.generator.GeneratorBlock;
import org.example.pz.quarryindustrial.block.offshore.OffshoreDeckBlock;
import org.example.pz.quarryindustrial.block.offshore.OffshoreLegBlock;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackPartBlock;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryFrameBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryTier;

import java.util.function.Function;

public final class ModBlocks {
    public static final QuarryBlock QUARRY_LV = registerQuarry("quarry_lv", QuarryTier.LV);
    public static final QuarryBlock QUARRY_MV = registerQuarry("quarry_mv", QuarryTier.MV);
    public static final QuarryBlock QUARRY_HV = registerQuarry("quarry_hv", QuarryTier.HV);
    /** Controlador de la quarry nuclear: va en el centro de la cara delantera del cubo de 3×3×3. */
    public static final QuarryBlock QUARRY_CORE = registerQuarry("quarry_core", QuarryTier.NUCLEAR);
    public static final Block QUARRY_CASING = register("quarry_casing", Block::new,
            BlockBehaviour.Properties.of()
                    .strength(5.0f, 12.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK));
    /** Entrada HV de la quarry nuclear: se pone en lugar de una carcasa, uno por reactor. */
    public static final EnergyPortBlock ENERGY_PORT = register("energy_port", EnergyPortBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(5.0f, 12.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK));
    /** Sin loot table: el marco lo pone la quarry gratis (con EU), no tiene que poder farmearse. */
    public static final QuarryFrameBlock QUARRY_FRAME = register("quarry_frame", QuarryFrameBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.METAL));

    /** Cabeza de pozo + controlador de la cigüeña. El modelo lo dibuja el renderer. */
    public static final PumpjackBlock PUMPJACK = register("pumpjack", PumpjackBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.METAL));
    /** Partes invisibles: sin item propio en la pestaña y sin drop (el item lo suelta el controlador). */
    public static final PumpjackPartBlock PUMPJACK_PART = register("pumpjack_part", PumpjackPartBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.METAL));

    /** Generador diésel (MV): quema los combustibles líquidos de Ala. */
    public static final GeneratorBlock DIESEL_GENERATOR = register("diesel_generator",
            properties -> new GeneratorBlock(GeneratorBlock.Kind.DIESEL, properties),
            BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(GeneratorBlock.LIT) ? 7 : 0)
                    .sound(SoundType.METAL));
    /** Turbina de vapor: se pega a la Steam Nozzle de Ala y convierte el vapor del reactor en EU. */
    public static final GeneratorBlock STEAM_TURBINE = register("steam_turbine",
            properties -> new GeneratorBlock(GeneratorBlock.Kind.STEAM_TURBINE, properties),
            BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    /** Plataforma offshore: el controlador se pone sobre el agua y arma cubierta y patas. */
    public static final OffshoreRigBlock OFFSHORE_RIG = registerOnWater("offshore_rig", OffshoreRigBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(5.0f, 12.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));
    /** Cubierta de rejilla (sin drop: la plataforma devuelve su item al desarmarse). */
    public static final OffshoreDeckBlock OFFSHORE_DECK = register("offshore_deck", OffshoreDeckBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(5.0f, 12.0f)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.METAL));
    public static final OffshoreLegBlock OFFSHORE_LEG = register("offshore_leg", OffshoreLegBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3_600_000.0f)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.METAL));

    /** Pad de drones: donde aterrizan, cargan y descargan. */
    public static final DronePadBlock DRONE_PAD = register("drone_pad", DronePadBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.METAL));
    /** Centralita: mapa y lista de todos los pads y drones de la zona. La pantalla brilla un poco. */
    public static final DroneConsoleBlock DRONE_CONSOLE = register("drone_console", DroneConsoleBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 6)
                    .sound(SoundType.METAL));

    /** Radar repetidor: extiende hasta dónde llega la centralita. */
    public static final RadarBlock RADAR = register("drone_radar", RadarBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.METAL));

    private ModBlocks() {
    }

    private static QuarryBlock registerQuarry(String name, QuarryTier tier) {
        return register(name, properties -> new QuarryBlock(tier, properties),
                BlockBehaviour.Properties.of()
                        .strength(3.5f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL));
    }

    /** Registra el bloque y su BlockItem. En 26.x las Properties necesitan setId antes de construir. */
    private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
                                                BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Quarryindustrial.id(name));
        T block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Quarryindustrial.id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    /** Como {@link #register}, pero el item se coloca sobre el agua (como un nenúfar). */
    private static <T extends Block> T registerOnWater(String name, Function<BlockBehaviour.Properties, T> factory,
                                                       BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Quarryindustrial.id(name));
        T block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Quarryindustrial.id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new PlaceOnWaterBlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
