package org.example.pz.quarryindustrial.block.pumpjack;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador de la cigüeña (la cabeza de pozo). Sobre el pozo de un géiser de Ala:
 * <ol>
 *   <li>mientras quede oil real en el caño o el domo, lo vacía bloque a bloque (rápido, cobra por cubo);</li>
 *   <li>con el domo vacío, extrae indefinidamente a un caudal fijo que crece con los upgrades de velocidad.</li>
 * </ol>
 * El oil va a un tanque interno que se puede vaciar desde cualquier parte de la base; además la cigüeña
 * lo empuja sola a los caños y tanques que tenga al lado.
 */
public class PumpjackBlockEntity extends BlockEntity {
    public static final int MAX_SPEED_UPGRADES = 4;
    private static final long MB = FluidConstants.BUCKET / 1000;
    private static final int REVALIDATE_INTERVAL = 200;
    private static final int RETRY_INTERVAL = 40;
    private static final int PUSH_INTERVAL = 5;
    private static final int DRAIN_QUEUE_LIMIT = 1024;

    public final SimpleEnergyStorage energy;
    public final SingleVariantStorage<FluidVariant> tank = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return QuarryConfig.get().pumpjack.tankBuckets * FluidConstants.BUCKET;
        }

        /** El tanque se llena solo desde adentro; de afuera solo se saca. */
        @Override
        protected boolean canInsert(FluidVariant variant) {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    /**
     * Lo que ven los caños de afuera: solo extracción. Así los caños de Ala Industrial, con la cara en modo
     * neutral, reconocen la cigüeña como un origen de un solo sentido y tiran del oil sin usar la llave.
     */
    private final Storage<FluidVariant> exposedTank = FilteringStorage.extractOnlyOf(tank);

    private PumpjackStatus status = PumpjackStatus.IDLE;
    private @Nullable OilWellValidator.Well well;
    private boolean domeDrained;
    private int speedUpgrades;
    private int tickCounter;
    private int drainCooldown;
    private final Deque<BlockPos> drainQueue = new ArrayDeque<>();
    /** Mientras la estructura se desarma, las partes no tienen que volver a romper el controlador. */
    private boolean dismantling;

    // Solo cliente: ángulo de la manivela
    private float crankAngle;
    private float previousCrankAngle;

    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMPJACK, pos, state);
        QuarryConfig.Pumpjack config = QuarryConfig.get().pumpjack;
        this.energy = new SimpleEnergyStorage(config.energyCapacity, config.maxInsert, 0) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpjackBlockEntity pumpjack) {
        if (level instanceof ServerLevel serverLevel) {
            pumpjack.tick(serverLevel, pos);
        }
    }

    private void tick(ServerLevel level, BlockPos pos) {
        tickCounter++;
        if (tickCounter % PUSH_INTERVAL == 0) {
            pushToNeighbours(level, pos);
        }
        Fluid oil = OilFluids.sourceFluid();
        if (oil == null) {
            setStatus(PumpjackStatus.NO_OIL_FLUID);
            return;
        }
        if (tickCounter % REVALIDATE_INTERVAL == 1 || (well == null && tickCounter % RETRY_INTERVAL == 1)) {
            well = OilWellValidator.validate(level, pos);
            if (well == null) {
                drainQueue.clear();
            }
        }
        if (well == null) {
            setStatus(PumpjackStatus.NO_WELL);
            return;
        }
        if (!tank.isResourceBlank() && !tank.variant.isOf(oil)) {
            // Cambió el mod que provee el oil: lo que haya quedado en el tanque se tira
            tank.variant = FluidVariant.blank();
            tank.amount = 0;
        }

        if (!domeDrained) {
            drain(level, oil);
        } else {
            pump(oil);
        }
    }

    /** Vacía un cubo de oil real cada {@code drainTicksPerBucket} ticks, empezando por lo más alto. */
    private void drain(ServerLevel level, Fluid oil) {
        QuarryConfig.Pumpjack config = QuarryConfig.get().pumpjack;
        if (drainCooldown > 0) {
            drainCooldown--;
            setStatus(PumpjackStatus.DRAINING);
            return;
        }
        if (space() < FluidConstants.BUCKET) {
            setStatus(PumpjackStatus.OUTPUT_FULL);
            return;
        }
        if (energy.amount < config.drainEuPerBucket) {
            setStatus(PumpjackStatus.NO_POWER);
            return;
        }
        BlockPos source = nextSource(level);
        if (source == null) {
            // No queda oil real: de acá en adelante, extracción infinita
            domeDrained = true;
            setChanged();
            return;
        }
        level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        energy.amount -= config.drainEuPerBucket;
        fill(oil, FluidConstants.BUCKET);
        drainCooldown = Math.max(0, config.drainTicksPerBucket - 1);
        setStatus(PumpjackStatus.DRAINING);
    }

    private void pump(Fluid oil) {
        QuarryConfig.Pumpjack config = QuarryConfig.get().pumpjack;
        double multiplier = 1 + speedUpgrades * config.speedUpgradePercent / 100.0;
        long amount = Math.round(config.baseRateMbPerTick * multiplier) * MB;
        long cost = Math.round(config.baseEuPerTick * multiplier);
        if (space() < amount) {
            setStatus(PumpjackStatus.OUTPUT_FULL);
            return;
        }
        if (energy.amount < cost) {
            setStatus(PumpjackStatus.NO_POWER);
            return;
        }
        energy.amount -= cost;
        fill(oil, amount);
        setStatus(PumpjackStatus.PUMPING);
    }

    /** La próxima fuente de oil del pozo. Si la cola está vacía, la rellena recorriendo caño y domo. */
    private @Nullable BlockPos nextSource(ServerLevel level) {
        while (!drainQueue.isEmpty()) {
            BlockPos candidate = drainQueue.peekFirst();
            if (level.isLoaded(candidate) && OilFluids.isOilSource(level.getBlockState(candidate))) {
                return candidate;
            }
            drainQueue.pollFirst();
        }
        if (well == null) {
            return null;
        }
        // BFS por las celdas abiertas, dentro de la caja del caño + domo
        QuarryConfig.Pumpjack config = QuarryConfig.get().pumpjack;
        BlockPos center = well.cavityCenter();
        int reach = well.radius() + 3;
        int minY = center.getY() - well.radius() - 3;
        int maxY = well.shaftTop().getY();
        Deque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> sources = new ArrayList<>();
        open.add(well.shaftTop());
        visited.add(well.shaftTop());
        while (!open.isEmpty() && visited.size() < config.drainScanMaxVisited) {
            BlockPos pos = open.poll();
            BlockState state = level.getBlockState(pos);
            if (OilFluids.isOilSource(state)) {
                sources.add(pos);
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (next.getY() < minY || next.getY() > maxY
                        || Math.abs(next.getX() - center.getX()) > reach
                        || Math.abs(next.getZ() - center.getZ()) > reach
                        || !level.isLoaded(next) || !visited.add(next)) {
                    continue;
                }
                if (OilFluids.isOpen(level.getBlockState(next))) {
                    open.add(next);
                }
            }
        }
        sources.sort(Comparator.comparingInt((BlockPos source) -> source.getY()).reversed());
        sources.stream().limit(DRAIN_QUEUE_LIMIT).forEach(drainQueue::addLast);
        return drainQueue.peekFirst();
    }

    private long space() {
        return tank.getCapacity() - tank.amount;
    }

    private void fill(Fluid oil, long amount) {
        if (tank.isResourceBlank()) {
            tank.variant = FluidVariant.of(oil);
        }
        tank.amount = Math.min(tank.getCapacity(), tank.amount + amount);
        setChanged();
    }

    /** Empuja oil a cualquier caño o tanque pegado a la base de la estructura. */
    private void pushToNeighbours(ServerLevel level, BlockPos pos) {
        if (tank.amount <= 0) {
            return;
        }
        Direction facing = getBlockState().getValue(PumpjackBlock.FACING);
        for (BlockPos port : PumpjackLayout.portNeighbours(pos, facing)) {
            if (!level.isLoaded(port)) {
                continue;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos from = port.relative(direction);
                BlockState fromState = level.getBlockState(from);
                if (!fromState.is(ModBlocks.PUMPJACK) && !fromState.is(ModBlocks.PUMPJACK_PART)) {
                    continue;
                }
                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, port, direction);
                if (target != null && target.supportsInsertion()) {
                    StorageUtil.move(tank, target, variant -> true, Long.MAX_VALUE, null);
                    if (tank.amount <= 0) {
                        return;
                    }
                }
                break;
            }
        }
    }

    // ── Upgrades ──

    /** Instala un upgrade de velocidad (lo único que acepta la cigüeña). */
    public boolean installUpgrade(UpgradeType type) {
        if (type != UpgradeType.SPEED || speedUpgrades >= MAX_SPEED_UPGRADES) {
            return false;
        }
        speedUpgrades++;
        markForSync();
        return true;
    }

    public boolean removeUpgrade() {
        if (speedUpgrades == 0) {
            return false;
        }
        speedUpgrades--;
        markForSync();
        return true;
    }

    public int speedUpgrades() {
        return speedUpgrades;
    }

    // ── Estado ──

    private void setStatus(PumpjackStatus status) {
        if (this.status != status) {
            this.status = status;
            markForSync();
        }
    }

    private void markForSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public PumpjackStatus status() {
        return status;
    }

    public @Nullable OilWellValidator.Well well() {
        return well;
    }

    public boolean domeDrained() {
        return domeDrained;
    }

    /** Oil en el tanque, en mB. */
    public long oilMb() {
        return tank.amount / MB;
    }

    public long tankCapacityMb() {
        return tank.getCapacity() / MB;
    }

    public Storage<FluidVariant> exposedTank() {
        return exposedTank;
    }

    public boolean isDismantling() {
        return dismantling;
    }

    // ── Cliente: animación ──

    public static void clientTick(Level level, BlockPos pos, BlockState state, PumpjackBlockEntity pumpjack) {
        pumpjack.previousCrankAngle = pumpjack.crankAngle;
        if (pumpjack.status.running()) {
            // Unos 8 golpes por minuto sin upgrades, como una cigüeña real; más rápido con upgrades
            float speed = 0.042f * (1 + pumpjack.speedUpgrades * 0.5f);
            if (pumpjack.status == PumpjackStatus.DRAINING) {
                speed *= 1.5f;
            }
            pumpjack.crankAngle += speed;
            if (pumpjack.crankAngle > Math.PI * 2) {
                pumpjack.crankAngle -= (float) (Math.PI * 2);
                pumpjack.previousCrankAngle -= (float) (Math.PI * 2);
            }
        }
    }

    public float crankAngle(float partialTick) {
        return previousCrankAngle + (crankAngle - previousCrankAngle) * partialTick;
    }

    // ── Desarme ──

    /** Al romper el controlador se van todas las partes. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dismantling = true;
        if (level != null && !level.isClientSide()) {
            Direction facing = state.getValue(PumpjackBlock.FACING);
            for (BlockPos part : PumpjackLayout.partPositions(pos, facing)) {
                if (level.getBlockEntity(part) instanceof PumpjackPartBlockEntity partBe
                        && pos.equals(partBe.controller())) {
                    level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ── Sincronización y persistencia ──

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        output.putString("Status", status.name());
        output.putInt("SpeedUpgrades", speedUpgrades);
        return output.buildResult();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Status", status.name());
        output.putInt("SpeedUpgrades", speedUpgrades);
        output.putLong("Energy", energy.amount);
        output.putBoolean("DomeDrained", domeDrained);
        SingleVariantStorage.writeValue(tank, FluidVariant.CODEC, output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        try {
            status = PumpjackStatus.valueOf(input.getStringOr("Status", PumpjackStatus.IDLE.name()));
        } catch (IllegalArgumentException e) {
            status = PumpjackStatus.IDLE;
        }
        speedUpgrades = Math.min(input.getIntOr("SpeedUpgrades", 0), MAX_SPEED_UPGRADES);
        energy.amount = Math.min(input.getLongOr("Energy", 0L), energy.getCapacity());
        domeDrained = input.getBooleanOr("DomeDrained", false);
        SingleVariantStorage.readValue(tank, FluidVariant.CODEC, FluidVariant::blank, input);
    }
}
