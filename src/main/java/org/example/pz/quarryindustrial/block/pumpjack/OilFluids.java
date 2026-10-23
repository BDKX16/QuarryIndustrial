package org.example.pz.quarryindustrial.block.pumpjack;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * El petróleo se busca por el tag de convención {@code #c:oil}, no por las clases de Ala Industrial: así la
 * cigüeña no depende de Ala para compilar y sirve con cualquier mod que registre su crudo en ese tag.
 */
public final class OilFluids {
    public static final TagKey<Fluid> OIL = TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "oil"));

    private OilFluids() {
    }

    /** El fluido fuente del tag (el primero que sea una fuente), o null si ningún mod registró petróleo. */
    public static @Nullable Fluid sourceFluid() {
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(OIL)) {
            Fluid fluid = holder.value();
            if (fluid != Fluids.EMPTY && fluid.defaultFluidState().isSource()) {
                return fluid;
            }
        }
        return null;
    }

    public static boolean isOil(BlockState state) {
        return !state.getFluidState().isEmpty() && state.getFluidState().is(OIL);
    }

    public static boolean isOilSource(BlockState state) {
        return isOil(state) && state.getFluidState().isSource();
    }

    /** "Abierto" para el pozo: aire o petróleo (el géiser recién generado está lleno; vaciado, es aire). */
    public static boolean isOpen(BlockState state) {
        return state.isAir() || isOil(state);
    }
}
