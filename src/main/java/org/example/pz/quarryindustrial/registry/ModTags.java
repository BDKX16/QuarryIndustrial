package org.example.pz.quarryindustrial.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.example.pz.quarryindustrial.Quarryindustrial;

public final class ModTags {
    /** Bloques que la quarry nunca rompe (las propias quarries, portales...). Ampliable por datapack. */
    public static final TagKey<Block> QUARRY_BLACKLIST =
            TagKey.create(Registries.BLOCK, Quarryindustrial.id("quarry_blacklist"));
    /** Items que el upgrade de void destruye en vez de guardarlos (piedra, tierra, grava...). */
    public static final TagKey<Item> VOID_FILTER =
            TagKey.create(Registries.ITEM, Quarryindustrial.id("void_filter"));

    private ModTags() {
    }
}
