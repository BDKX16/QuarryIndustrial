package org.example.pz.quarryindustrial.block.generator;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.registry.ModAdvancements;
import org.example.pz.quarryindustrial.energy.EnergyOutput;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Base de los generadores: un buffer que solo se vacía (por cables o empujando a los vecinos) y un
 * tanque de fluido que solo se llena. Cada tick: {@link #generate} produce y después se empuja la energía.
 */
public abstract class GeneratorBlockEntity extends BlockEntity {
    public final SimpleEnergyStorage energy;
    private final long maxOutput;
    private boolean running;
    /** EU/t producidos, promedio de ~1 segundo (para Jade y el mensaje de estado). */
    private double averageOutput;

    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity,
                                   long maxOutput) {
        super(type, pos, state);
        this.maxOutput = maxOutput;
        // Sin entrada (no es una batería); los cables pueden sacar hasta maxOutput por tick
        this.energy = new SimpleEnergyStorage(capacity, 0, maxOutput) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
    }

    /** Produce energía en el buffer. Devuelve los EU generados este tick. */
    protected abstract long generate(ServerLevel level, BlockPos pos);

    /** Lo que entra por caño: combustible o vapor. Nunca se puede sacar. */
    public abstract Storage<FluidVariant> fluidInput();

    public abstract Component statusMessage();

    protected void serverTick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long produced = generate(serverLevel, pos);
        averageOutput += (produced - averageOutput) * 0.05;
        EnergyOutput.push(serverLevel, pos, energy, maxOutput);
        boolean nowRunning = produced > 0;
        if (nowRunning != running) {
            running = nowRunning;
            if (running && this instanceof SteamTurbineBlockEntity) {
                ModAdvancements.grant(serverLevel, pos, ModAdvancements.WASTE_NOT);
            }
            level.setBlock(pos, state.setValue(GeneratorBlock.LIT, running), Block.UPDATE_CLIENTS);
        }
        if (produced > 0) {
            setChanged();
        }
    }

    protected long space() {
        return energy.getCapacity() - energy.amount;
    }

    public boolean running() {
        return running;
    }

    public long averageOutput() {
        return Math.round(averageOutput);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy.amount);
        output.putBoolean("Running", running);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.amount = Math.min(input.getLongOr("Energy", 0L), energy.getCapacity());
        running = input.getBooleanOr("Running", false);
    }
}
