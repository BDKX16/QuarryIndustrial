package org.example.pz.quarryindustrial.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import org.example.pz.quarryindustrial.registry.ModItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lo que configura un filtro de caño: 5 casilleros fantasma (solo 1 activo hasta instalar la mejora, que es el 6.º
 * slot), lista blanca o negra, y comparar por ítem o por tag. Es un contenedor para que la GUI lo use como cualquier
 * otro y los slots se sincronicen solos.
 */
public final class PipeFilterData extends SimpleContainer {
    public static final int CELLS = 5;
    /** El slot de la mejora de expansión: con ella instalada se activan los 5 casilleros. */
    public static final int UPGRADE_SLOT = CELLS;
    public static final int SIZE = CELLS + 1;

    public static final Codec<PipeFilterData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(d -> d.getItems()),
            Codec.BOOL.optionalFieldOf("allow", true).forGetter(d -> d.allow),
            Codec.BOOL.optionalFieldOf("tag", false).forGetter(d -> d.tagMatch),
            Codec.INT.listOf().optionalFieldOf("tags", List.of()).forGetter(d -> {
                List<Integer> list = new ArrayList<>();
                for (int tag : d.tagIndex) {
                    list.add(tag);
                }
                return list;
            })
    ).apply(i, PipeFilterData::fromParts));

    /** true = lista blanca (solo pasa lo marcado); false = lista negra (pasa todo menos lo marcado). */
    public boolean allow = true;
    /** true = compara por tag (la categoría elegida en cada casillero); false = por ítem. */
    public boolean tagMatch;
    /** El tag elegido de cada casillero, como índice en {@link #tagsOf}. */
    public final int[] tagIndex = new int[CELLS];
    private Runnable listener = () -> { };

    public PipeFilterData() {
        super(SIZE);
    }

    private static PipeFilterData fromParts(List<ItemStack> slots, boolean allow, boolean tag, List<Integer> tags) {
        PipeFilterData data = new PipeFilterData();
        for (int i = 0; i < Math.min(SIZE, slots.size()); i++) {
            data.setItem(i, slots.get(i).copyWithCount(Math.min(1, slots.get(i).getCount())));
        }
        data.allow = allow;
        data.tagMatch = tag;
        for (int i = 0; i < Math.min(CELLS, tags.size()); i++) {
            data.tagIndex[i] = Math.max(0, tags.get(i));
        }
        return data;
    }

    public PipeFilterData copy() {
        PipeFilterData copy = new PipeFilterData();
        for (int i = 0; i < SIZE; i++) {
            copy.setItem(i, getItem(i).copy());
        }
        copy.allow = allow;
        copy.tagMatch = tagMatch;
        System.arraycopy(tagIndex, 0, copy.tagIndex, 0, CELLS);
        return copy;
    }

    /** Quién se entera de que algo cambió (el caño, para guardarse). */
    public void setListener(Runnable listener) {
        this.listener = listener == null ? () -> { } : listener;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        listener.run();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    // ── Reglas ──

    public boolean upgraded() {
        return !getItem(UPGRADE_SLOT).isEmpty();
    }

    /** Casilleros que cuentan: 1 de base, 5 con la mejora. */
    public int activeCells() {
        return upgraded() ? CELLS : 1;
    }

    public boolean isConfigured() {
        for (int i = 0; i < activeCells(); i++) {
            if (!getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * ¿Pasa este ítem? Lista blanca sin nada marcado no deja pasar nada; lista negra sin nada marcado lo deja pasar
     * todo.
     */
    public boolean passes(ItemVariant variant) {
        boolean hit = false;
        for (int i = 0; i < activeCells() && !hit; i++) {
            ItemStack sample = getItem(i);
            if (!sample.isEmpty()) {
                hit = matches(i, sample.getItem(), variant.getItem());
            }
        }
        return allow == hit;
    }

    private boolean matches(int cell, Item sample, Item candidate) {
        if (tagMatch) {
            List<TagKey<Item>> tags = tagsOf(sample);
            if (!tags.isEmpty()) {
                TagKey<Item> tag = tags.get(Math.floorMod(tagIndex[cell], tags.size()));
                return BuiltInRegistries.ITEM.wrapAsHolder(candidate).is(tag);
            }
        }
        return sample == candidate;
    }

    /** Los tags de un ítem, en orden fijo: el casillero guarda un índice en esta lista. */
    public static List<TagKey<Item>> tagsOf(Item item) {
        List<TagKey<Item>> tags = new ArrayList<>(BuiltInRegistries.ITEM.wrapAsHolder(item).tags().toList());
        tags.sort(Comparator.comparing(tag -> tag.location().toString()));
        return tags;
    }

    /** El tag que usa el casillero (nulo si el ítem no tiene ninguno). */
    public TagKey<Item> tagOf(int cell) {
        ItemStack sample = getItem(cell);
        if (sample.isEmpty()) {
            return null;
        }
        List<TagKey<Item>> tags = tagsOf(sample.getItem());
        return tags.isEmpty() ? null : tags.get(Math.floorMod(tagIndex[cell], tags.size()));
    }

    public void cycleTag(int cell) {
        tagIndex[cell]++;
        setChanged();
    }

    public void setSample(int cell, ItemStack stack) {
        setItem(cell, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        tagIndex[cell] = 0;
        setChanged();
    }

    /** El filtro como ítem, con su configuración adentro (para devolverlo al sacarlo o romper el caño). */
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModItems.PIPE_FILTER);
        stack.set(org.example.pz.quarryindustrial.registry.ModComponents.PIPE_FILTER, copy());
        return stack;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PipeFilterData that) || allow != that.allow || tagMatch != that.tagMatch
                || !java.util.Arrays.equals(tagIndex, that.tagIndex)) {
            return false;
        }
        for (int i = 0; i < SIZE; i++) {
            if (!ItemStack.matches(getItem(i), that.getItem(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = (allow ? 1 : 0) + (tagMatch ? 2 : 0) + java.util.Arrays.hashCode(tagIndex);
        for (int i = 0; i < SIZE; i++) {
            hash = hash * 31 + getItem(i).getItem().hashCode();
        }
        return hash;
    }
}
