package org.example.pz.quarryindustrial.block.splitter;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.energy.EnergyOutput;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Distribuidor de energía: recibe EU de un cable por cualquier cara y los reparte a partes iguales entre
 * todas las demás caras con algo que acepte energía (cables, pads, máquinas). Si un destino se llena,
 * lo que sobra se reparte entre los otros. Una cara que le acaba de dar energía no recibe nada de vuelta.
 */
public class EnergySplitterBlockEntity extends BlockEntity {
    /** Ticks que una cara se considera "entrada" después de darle energía. */
    private static final long INPUT_MEMORY = 5;

    public final SimpleEnergyStorage energy;
    private final long maxTransfer;
    private final long[] lastInput = new long[6];
    private final EnergyStorage[] faces = new EnergyStorage[6];
    private int rotation;

    public EnergySplitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_SPLITTER, pos, state);
        QuarryConfig config = QuarryConfig.get();
        this.maxTransfer = config.splitterMaxTransfer;
        this.energy = new SimpleEnergyStorage(config.splitterCapacity, config.splitterMaxTransfer, 0) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
        java.util.Arrays.fill(lastInput, Long.MIN_VALUE / 2);
    }

    /** Vista de una cara: solo deja entrar (nadie le saca energía) y recuerda por dónde llegó. */
    public EnergyStorage storageFor(@Nullable Direction side) {
        if (side == null) {
            return energy;
        }
        int index = side.ordinal();
        if (faces[index] == null) {
            faces[index] = new EnergyStorage() {
                @Override
                public long insert(long maxAmount, TransactionContext transaction) {
                    long accepted = energy.insert(maxAmount, transaction);
                    if (accepted > 0 && level != null) {
                        lastInput[index] = level.getGameTime();
                    }
                    return accepted;
                }

                @Override
                public long extract(long maxAmount, TransactionContext transaction) {
                    return 0;
                }

                @Override
                public boolean supportsExtraction() {
                    return false;
                }

                @Override
                public long getAmount() {
                    return energy.getAmount();
                }

                @Override
                public long getCapacity() {
                    return energy.getCapacity();
                }
            };
        }
        return faces[index];
    }

    public void serverTick(ServerLevel level, BlockPos pos) {
        if (energy.amount <= 0) {
            return;
        }
        long now = level.getGameTime();
        int excluded = 0;
        for (Direction direction : Direction.values()) {
            if (now - lastInput[direction.ordinal()] <= INPUT_MEMORY) {
                excluded |= 1 << direction.ordinal();
            }
        }
        long moved = EnergyOutput.pushEvenly(level, pos, energy, maxTransfer, excluded, rotation++);
        if (moved > 0) {
            setChanged();
        }
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
