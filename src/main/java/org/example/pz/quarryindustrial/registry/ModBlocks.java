package org.example.pz.quarryindustrial.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.example.pz.quarryindustrial.Quarryindustrial;
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

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
