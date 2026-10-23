package org.example.pz.quarryindustrial.block.offshore;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.registry.ModAdvancements;
import org.example.pz.quarryindustrial.ClientEffects;
import org.example.pz.quarryindustrial.block.pumpjack.OilFluids;
import org.example.pz.quarryindustrial.compat.AlaOilSlime;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.energy.FuelTank;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.item.DrillBitItem;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Plataforma offshore. Primero perfora: baja la sarta por el agua y {@code drillDepth} metros dentro del
 * lecho marino (gasta EU y broca). Al llegar al reservorio produce petróleo para siempre, más rápido que una
 * cigüeña. Puede andar a diésel (combustible por el controlador) y el oil sale por cualquier bloque de la
 * cubierta: con caños hasta la costa o, mejor, con un dron cisterna.
 */
public class OffshoreRigBlockEntity extends BlockEntity {
    public static final int MAX_SPEED_UPGRADES = 4;
    private static final long MB = FluidConstants.BUCKET / 1000;
    private static final int STRUCTURE_CHECK_INTERVAL = 100;

    public final SimpleEnergyStorage energy;
    public final SingleVariantStorage<FluidVariant> tank = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return QuarryConfig.get().offshore.tankBuckets * FluidConstants.BUCKET;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };
    private final Storage<FluidVariant> exposedTank = FilteringStorage.extractOnlyOf(tank);
    public final FuelTank fuel = new FuelTank(QuarryConfig.get().offshore.fuelTankBuckets, this::setChanged);
    private final Storage<FluidVariant> exposedFuel = FilteringStorage.insertOnlyOf(fuel);
    private ItemStack bit = ItemStack.EMPTY;

    private OffshoreStatus status = OffshoreStatus.IDLE;
    private boolean intact = true;
    private int speedUpgrades;
    /** Metros perforados debajo del lecho marino. */
    private double drilled;
    /** Y del bloque del fondo justo debajo del moonpool (para el renderer). */
    private int seabedY;
    private boolean onDiesel;
    private boolean dismantling;
    private int tickCounter;

    // Solo cliente
    private float drillAngle;
    private float previousDrillAngle;

    public OffshoreRigBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OFFSHORE_RIG, pos, state);
        QuarryConfig.Offshore config = QuarryConfig.get().offshore;
        this.energy = new SimpleEnergyStorage(config.energyCapacity, config.maxInsert, 0) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
        this.seabedY = pos.getY() - 1;
    }

    /** Recién armada: mide el fondo. */
    void onBuilt() {
        if (level != null) {
            seabedY = worldPosition.getY() - OffshoreLayout.waterDepth(level, worldPosition) - 1;
            markForSync();
        }
    }

    void serverTick(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        tickCounter++;
        if (tickCounter % STRUCTURE_CHECK_INTERVAL == 1) {
            intact = checkStructure(level, pos);
        }
        if (!intact) {
            setStatus(OffshoreStatus.DAMAGED);
            return;
        }
        Fluid oil = OilFluids.sourceFluid();
        if (oil == null) {
            setStatus(OffshoreStatus.NO_OIL_FLUID);
            return;
        }
        if (!tank.isResourceBlank() && !tank.variant.isOf(oil)) {
            tank.variant = FluidVariant.blank();
            tank.amount = 0;
        }
        if (drilled < QuarryConfig.get().offshore.drillDepth) {
            drill(serverLevel);
        } else {
            produce(serverLevel, oil);
        }
    }

    private void drill(ServerLevel level) {
        QuarryConfig.Offshore config = QuarryConfig.get().offshore;
        if (!(bit.getItem() instanceof DrillBitItem)) {
            setStatus(OffshoreStatus.NO_BIT);
            return;
        }
        if (!pay(config.drillEuPerTick)) {
            setStatus(OffshoreStatus.NO_POWER);
            return;
        }
        int before = (int) drilled;
        drilled = Math.min(config.drillDepth, drilled + 1.0 / Math.max(1, config.drillTicksPerMeter));
        if ((int) drilled > before) {
            // Un metro más: la broca se gasta y la sarta se ve más abajo
            int damage = bit.getDamageValue() + config.bitWearPerMeter;
            if (damage >= bit.getMaxDamage()) {
                bit = ItemStack.EMPTY;
                level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 1.0f, 0.7f);
            } else {
                bit.setDamageValue(damage);
            }
            markForSync();
        }
        setStatus(OffshoreStatus.DRILLING);
        setChanged();
    }

    private void produce(ServerLevel level, Fluid oil) {
        QuarryConfig.Offshore config = QuarryConfig.get().offshore;
        double multiplier = 1 + speedUpgrades * config.speedUpgradePercent / 100.0;
        long amount = Math.round(config.baseRateMbPerTick * multiplier) * MB;
        long cost = Math.round(config.baseEuPerTick * multiplier);
        if (tank.getCapacity() - tank.amount < amount) {
            setStatus(OffshoreStatus.OUTPUT_FULL);
            return;
        }
        if (!pay(cost)) {
            setStatus(OffshoreStatus.NO_POWER);
            return;
        }
        if (tank.isResourceBlank()) {
            tank.variant = FluidVariant.of(oil);
        }
        tank.amount += amount;
        setStatus(OffshoreStatus.PRODUCING);
        AlaOilSlime.maybeRelease(level, worldPosition.offset(OffshoreLayout.HALF - 1, 1, 0), config.oilSlimeChance);
        setChanged();
    }

    /** Cobra {@code cost} EU; lo que falte lo pone el motor diésel. */
    private boolean pay(long cost) {
        boolean diesel = false;
        if (energy.amount < cost && fuel.hasFuel()) {
            long burned = fuel.burn(Math.min(cost - energy.amount, energy.getCapacity() - energy.amount));
            energy.amount += burned;
            diesel = burned > 0;
        }
        if (diesel != onDiesel) {
            onDiesel = diesel;
            markForSync();
        }
        if (energy.amount < cost) {
            return false;
        }
        energy.amount -= cost;
        return true;
    }

    /** La cubierta completa y las cuatro patas en su lugar. */
    private boolean checkStructure(Level level, BlockPos pos) {
        for (BlockPos deck : OffshoreLayout.deckPositions(pos)) {
            if (!level.getBlockState(deck).is(ModBlocks.OFFSHORE_DECK)) {
                return false;
            }
        }
        for (BlockPos corner : OffshoreLayout.corners(pos)) {
            if (!level.getBlockState(corner.below()).is(ModBlocks.OFFSHORE_LEG)) {
                return false;
            }
        }
        return true;
    }

    // ── Broca y upgrades ──

    public ItemStack bit() {
        return bit;
    }

    public ItemStack swapBit(ItemStack stack) {
        ItemStack previous = bit;
        bit = stack;
        markForSync();
        return previous;
    }

    public boolean installSpeedUpgrade() {
        if (speedUpgrades >= MAX_SPEED_UPGRADES) {
            return false;
        }
        speedUpgrades++;
        markForSync();
        return true;
    }

    public boolean removeSpeedUpgrade() {
        if (speedUpgrades == 0) {
            return false;
        }
        speedUpgrades--;
        markForSync();
        return true;
    }

    // ── Estado ──

    private void setStatus(OffshoreStatus status) {
        if (this.status != status && status == OffshoreStatus.PRODUCING && level instanceof ServerLevel serverLevel) {
            ModAdvancements.grant(serverLevel, worldPosition, ModAdvancements.INTO_THE_DEEP);
        }
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

    public OffshoreStatus status() {
        return status;
    }

    public double drilled() {
        return drilled;
    }

    public int seabedY() {
        return seabedY;
    }

    public int speedUpgrades() {
        return speedUpgrades;
    }

    public boolean onDiesel() {
        return onDiesel;
    }

    public long oilMb() {
        return tank.amount / MB;
    }

    public long tankCapacityMb() {
        return tank.getCapacity() / MB;
    }

    public Storage<FluidVariant> oilOutput() {
        return exposedTank;
    }

    public Storage<FluidVariant> fuelInput() {
        return exposedFuel;
    }

    public boolean isDismantling() {
        return dismantling;
    }

    public Component statusMessage() {
        QuarryConfig.Offshore config = QuarryConfig.get().offshore;
        return Component.translatable("message.quarryindustrial.offshore.status", status.label(),
                (int) drilled, config.drillDepth, energy.amount, energy.getCapacity(), oilMb(), tankCapacityMb(),
                fuel.amountMb(), speedUpgrades);
    }

    /** Broca que se ve en la punta de la sarta. */
    public @Nullable BitTier visualBit() {
        return bit.getItem() instanceof DrillBitItem item ? item.tier() : null;
    }

    // ── Cliente ──

    void clientTick(Level level, BlockPos pos) {
        ClientEffects.machineTick.accept(this);
        previousDrillAngle = drillAngle;
        if (status.running()) {
            drillAngle += status == OffshoreStatus.DRILLING ? 0.35f : 0.08f;
            if (drillAngle > Math.PI * 2) {
                drillAngle -= (float) (Math.PI * 2);
                previousDrillAngle -= (float) (Math.PI * 2);
            }
        }
        RandomSource random = level.getRandom();
        if (status == OffshoreStatus.PRODUCING) {
            // Antorcha: el gas asociado se quema en la punta del brazo
            double fx = pos.getX() + 0.5 + OffshoreLayout.FLARE_TIP_X;
            double fy = pos.getY() + OffshoreLayout.FLARE_TIP_Y;
            double fz = pos.getZ() + 0.5 + OffshoreLayout.FLARE_TIP_Z;
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.FLAME, fx + (random.nextDouble() - 0.5) * 0.3, fy,
                        fz + (random.nextDouble() - 0.5) * 0.3, 0, 0.06 + random.nextDouble() * 0.05, 0);
            }
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, fx, fy + 0.8, fz, 0, 0.05, 0);
            }
        }
        if (status == OffshoreStatus.DRILLING && random.nextInt(2) == 0) {
            // Burbujas donde la sarta entra al lecho marino
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + 0.5 + (random.nextDouble() - 0.5),
                    seabedY + 1.1, pos.getZ() + 0.5 + (random.nextDouble() - 0.5), 0, 0.2, 0);
        }
        if (onDiesel && status.running() && random.nextInt(3) == 0) {
            // Chimenea de la sala de máquinas
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() - 2.1, pos.getY() + 3.2, pos.getZ() + 2.1, 0, 0.06, 0);
        }
    }

    public float drillAngle(float partialTick) {
        return previousDrillAngle + (drillAngle - previousDrillAngle) * partialTick;
    }

    // ── Desarme ──

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dismantling = true;
        if (level != null && !level.isClientSide()) {
            for (BlockPos deck : OffshoreLayout.deckPositions(pos)) {
                if (level.getBlockEntity(deck) instanceof OffshoreDeckBlockEntity part && pos.equals(part.controller())) {
                    level.setBlock(deck, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            // Las patas vuelven a ser agua
            for (BlockPos corner : OffshoreLayout.corners(pos)) {
                BlockPos.MutableBlockPos cursor = corner.below().mutable();
                while (level.getBlockState(cursor).is(ModBlocks.OFFSHORE_LEG)) {
                    level.setBlock(cursor, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                    cursor.move(0, -1, 0);
                }
            }
            if (!bit.isEmpty()) {
                Block.popResource(level, pos, bit);
                bit = ItemStack.EMPTY;
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ── Sincronización y persistencia ──

    private void writeSync(ValueOutput output) {
        output.putString("Status", status.name());
        output.putDouble("Drilled", drilled);
        output.putInt("SeabedY", seabedY);
        output.putInt("SpeedUpgrades", speedUpgrades);
        output.putBoolean("Diesel", onDiesel);
        output.store("Bit", ItemStack.OPTIONAL_CODEC, bit);
    }

    private void readSync(ValueInput input) {
        try {
            status = OffshoreStatus.valueOf(input.getStringOr("Status", OffshoreStatus.IDLE.name()));
        } catch (IllegalArgumentException e) {
            status = OffshoreStatus.IDLE;
        }
        drilled = input.getDoubleOr("Drilled", 0);
        seabedY = input.getIntOr("SeabedY", worldPosition.getY() - 1);
        speedUpgrades = Math.min(input.getIntOr("SpeedUpgrades", 0), MAX_SPEED_UPGRADES);
        onDiesel = input.getBooleanOr("Diesel", false);
        bit = input.read("Bit", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writeSync(output);
        return output.buildResult();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        writeSync(output);
        output.putLong("Energy", energy.amount);
        SingleVariantStorage.writeValue(tank, FluidVariant.CODEC, output.child("Oil"));
        fuel.save(output.child("Fuel"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        readSync(input);
        energy.amount = Math.min(input.getLongOr("Energy", 0L), energy.getCapacity());
        SingleVariantStorage.readValue(tank, FluidVariant.CODEC, FluidVariant::blank, input.childOrEmpty("Oil"));
        fuel.load(input.childOrEmpty("Fuel"));
    }
}
