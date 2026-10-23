package org.example.pz.quarryindustrial.energy;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Combustibles líquidos, por tags de convención (los registra Ala Industrial: diésel y fuel oil salen de su
 * columna de destilación, el biocombustible de su fermentador). Cuánta energía da cada mB está en la config.
 */
public final class CombustionFuels {
    public static final TagKey<Fluid> DIESEL = tag("diesel");
    public static final TagKey<Fluid> FUEL_OIL = tag("fuel_oil");
    public static final TagKey<Fluid> BIOFUEL = tag("biofuel");
    public static final TagKey<Fluid> STEAM = tag("steam");

    private CombustionFuels() {
    }

    private static TagKey<Fluid> tag(String path) {
        return TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", path));
    }

    /** EU por mB, o 0 si no es combustible. */
    public static long euPerMb(FluidVariant variant) {
        if (variant.isBlank()) {
            return 0;
        }
        QuarryConfig.Generators config = QuarryConfig.get().generators;
        var state = variant.getFluid().defaultFluidState();
        if (state.is(DIESEL)) {
            return config.dieselEuPerMb;
        }
        if (state.is(FUEL_OIL)) {
            return config.fuelOilEuPerMb;
        }
        if (state.is(BIOFUEL)) {
            return config.biofuelEuPerMb;
        }
        return 0;
    }

    public static boolean isFuel(FluidVariant variant) {
        return euPerMb(variant) > 0;
    }

    /** El vapor de algún mod (el primero del tag {@code #c:steam}), o null. */
    public static @Nullable Fluid steamFluid() {
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(STEAM)) {
            if (holder.value() != Fluids.EMPTY) {
                return holder.value();
            }
        }
        return null;
    }

    public static boolean isSteam(FluidVariant variant) {
        return !variant.isBlank() && variant.getFluid().defaultFluidState().is(STEAM);
    }
}
