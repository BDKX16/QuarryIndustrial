package org.example.pz.quarryindustrial.drone;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/** Lo que lleva un dron: items, un fluido, energía o (el explorador) los géiseres que encontró. */
public final class DronePayload {
    public final List<ItemStack> items = new ArrayList<>();
    public FluidVariant fluid = FluidVariant.blank();
    /** En gotas de Fabric (81000 por cubo). */
    public long fluidAmount;
    public long energy;
    public final List<BlockPos> sites = new ArrayList<>();

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty) && fluidAmount <= 0 && energy <= 0;
    }

    public void save(ValueOutput output) {
        output.store("Items", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(items));
        output.store("Fluid", FluidVariant.CODEC, fluid);
        output.putLong("FluidAmount", fluidAmount);
        output.putLong("Energy", energy);
        output.store("Sites", BlockPos.CODEC.listOf(), List.copyOf(sites));
    }

    public void load(ValueInput input) {
        items.clear();
        items.addAll(input.read("Items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        items.removeIf(ItemStack::isEmpty);
        fluid = input.read("Fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        fluidAmount = input.getLongOr("FluidAmount", 0L);
        energy = input.getLongOr("Energy", 0L);
        sites.clear();
        sites.addAll(input.read("Sites", BlockPos.CODEC.listOf()).orElse(List.of()));
    }
}
