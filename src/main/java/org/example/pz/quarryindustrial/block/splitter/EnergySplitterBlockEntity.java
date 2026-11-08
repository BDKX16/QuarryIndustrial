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
 * Distribuidor de energía: recibe EU por su cara de entrada (FACING) y los reparte a partes iguales entre las
 * otras cinco caras con algo que acepte energía (cables, pads, máquinas, otro distribuidor por su entrada). Si un
 * destino se llena, lo que sobra se reparte entre los otros. Las caras de salida no aceptan energía (solo se les puede sacar, como a un generador): así un pad
 * que empuja la que traen sus drones, o un cable que la devuelve, no pueden confundirse con una entrada.
 */
public class EnergySplitterBlockEntity extends BlockEntity {
    public final SimpleEnergyStorage energy;
    private final long maxTransfer;
    private final EnergyStorage input;
    private final EnergyStorage[] outputs = new EnergyStorage[6];
    /** EU que cada salida todavía puede entregar a un cable en este tick. */
    private final long[] budget = new long[6];
    private final EnergyStorage readOnly;
    private int rotation;

    public EnergySplitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_SPLITTER, pos, state);
        QuarryConfig config = QuarryConfig.get();
        this.maxTransfer = config.splitterMaxTransfer;
        this.energy = new SimpleEnergyStorage(config.splitterCapacity, config.splitterMaxTransfer, config.splitterMaxTransfer) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
        // Entrada: solo deja entrar
        this.input = new EnergyStorage() {
            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                return energy.insert(maxAmount, transaction);
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
        // Para quien pregunta sin cara (Jade y otros): se puede mirar, no tocar
        this.readOnly = new EnergyStorage() {
            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public long extract(long maxAmount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public boolean supportsInsertion() {
                return false;
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

    /** Cara de entrada: solo acepta energía. Las otras cinco: solo se les puede sacar (con tope por tick). */
    public @Nullable EnergyStorage storageFor(@Nullable Direction side) {
        if (side == null) {
            return readOnly;
        }
        if (side == getBlockState().getValue(EnergySplitterBlock.FACING)) {
            return input;
        }
        int index = side.ordinal();
        if (outputs[index] == null) {
            // Salida: no acepta nada, pero se le puede sacar energía. Los cables de Ala tiran de las fuentes
            // que lo permiten (como de un generador), así que un cable pegado a una salida se lleva la energía.
            // Cada cara tiene un tope por tick (su parte equitativa) para que un cable no se lleve todo antes
            // de que el distribuidor reparta entre las demás salidas.
            outputs[index] = new EnergyStorage() {
                @Override
                public long insert(long maxAmount, TransactionContext transaction) {
                    return 0;
                }

                @Override
                public long extract(long maxAmount, TransactionContext transaction) {
                    long allowed = Math.min(maxAmount, budget[index]);
                    if (allowed <= 0) {
                        return 0;
                    }
                    long taken = energy.extract(allowed, transaction);
                    if (taken > 0) {
                        transaction.addCloseCallback((closing, result) -> {
                            if (result.wasCommitted()) {
                                budget[index] = Math.max(0, budget[index] - taken);
                            }
                        });
                    }
                    return taken;
                }

                @Override
                public boolean supportsInsertion() {
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
        return outputs[index];
    }

    public void serverTick(ServerLevel level, BlockPos pos, Direction inputFace) {
        // Parte equitativa de cada salida con algo pegado: lo máximo que un cable puede sacar en este tick
        int connected = 0;
        for (Direction direction : Direction.values()) {
            if (direction != inputFace && !level.getBlockState(pos.relative(direction)).isAir()) {
                connected++;
            }
        }
        long share = connected == 0 ? 0 : Math.max(1, energy.amount / connected);
        for (Direction direction : Direction.values()) {
            budget[direction.ordinal()] = direction == inputFace ? 0 : share;
        }
        if (energy.amount <= 0) {
            return;
        }
        long moved = EnergyOutput.pushEvenly(level, pos, energy, maxTransfer, 1 << inputFace.ordinal(), rotation++);
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
