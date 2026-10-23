package org.example.pz.quarryindustrial.block.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Buffer chico de un puerto de energía. Acepta hasta 512 EU/t (HV) de su cable; el core de la quarry
 * nuclear lo vacía cada tick. Nadie puede sacarle energía desde afuera.
 */
public class EnergyPortBlockEntity extends BlockEntity {
    public final SimpleEnergyStorage energy;

    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_PORT, pos, state);
        QuarryConfig config = QuarryConfig.get();
        this.energy = new SimpleEnergyStorage(config.energyPortCapacity, config.nuclear.maxInsert, 0) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
    }

    /** Saca hasta {@code max} EU para el core. Devuelve cuánto sacó. */
    public long drain(long max) {
        long taken = Math.min(max, energy.amount);
        if (taken > 0) {
            energy.amount -= taken;
            setChanged();
        }
        return taken;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.amount = Math.min(input.getLongOr("Energy", 0L), energy.getCapacity());
    }
}
