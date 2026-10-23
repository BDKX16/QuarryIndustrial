package org.example.pz.quarryindustrial.block.generator;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.energy.FuelTank;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;

/**
 * Motor diésel con generador: quema diésel, fuel oil o biocombustible de Ala y entrega EU (MV). Cierra el
 * ciclo del petróleo: cigüeña → columna de destilación → diésel → energía para la cigüeña y las quarries.
 */
public class DieselGeneratorBlockEntity extends GeneratorBlockEntity {
    public final FuelTank fuel;
    private final Storage<FluidVariant> exposedFuel;

    public DieselGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIESEL_GENERATOR, pos, state, QuarryConfig.get().generators.dieselGeneratorCapacity,
                QuarryConfig.get().generators.dieselGeneratorEuPerTick);
        this.fuel = new FuelTank(QuarryConfig.get().generators.dieselGeneratorTankBuckets, this::setChanged);
        this.exposedFuel = FilteringStorage.insertOnlyOf(fuel);
    }

    @Override
    protected long generate(ServerLevel level, BlockPos pos) {
        long perTick = Math.min(QuarryConfig.get().generators.dieselGeneratorEuPerTick, space());
        if (perTick <= 0) {
            return 0;
        }
        long produced = fuel.burn(perTick);
        energy.amount += produced;
        return produced;
    }

    @Override
    public Storage<FluidVariant> fluidInput() {
        return exposedFuel;
    }

    @Override
    public Component statusMessage() {
        return Component.translatable("message.quarryindustrial.diesel_generator.status",
                Component.translatable(running() ? "status.quarryindustrial.generator.running"
                        : "status.quarryindustrial.generator.idle"),
                averageOutput(), energy.amount, energy.getCapacity(), fuel.amountMb(), fuel.capacityMb());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fuel.save(output.child("Fuel"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel.load(input.childOrEmpty("Fuel"));
    }
}
