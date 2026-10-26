package org.example.pz.quarryindustrial.block.drone;

import org.example.pz.quarryindustrial.menu.InfoKind;
import org.example.pz.quarryindustrial.menu.InfoMachine;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.registry.ModAdvancements;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreStatus;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.drone.DroneEntity;
import org.example.pz.quarryindustrial.drone.DroneFlights;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.drone.DroneNetwork;
import org.example.pz.quarryindustrial.drone.DronePayload;
import org.example.pz.quarryindustrial.item.DrillBitItem;
import org.example.pz.quarryindustrial.item.DroneItem;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModComponents;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.example.pz.quarryindustrial.registry.ModTickets;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pad de drones. Tiene dos "buzones": el de salida (lo que el dron se lleva) y el de llegada (lo que trajo
 * otro dron, que el pad reparte a sus vecinos). Los caños y tolvas meten en el de salida y sacan del de llegada.
 *
 * <ul>
 *   <li>Carga / cisterna / energía: junta en el buzón de salida (también tirando de cofres y tanques pegados) y,
 *   cuando hay suficiente, el dron lo lleva al pad vinculado.</li>
 *   <li>Mantenimiento: busca quarries y plataformas cercanas con la broca gastada y les lleva una del buzón.</li>
 *   <li>Explorador: con un clic, sale a sobrevolar la zona y vuelve con un informe de géiseres.</li>
 * </ul>
 * Cada viaje cuesta energía del pad.
 */
public class DronePadBlockEntity extends BlockEntity implements InfoMachine {
    public static final int BOX_SLOTS = 9;
    private static final long MB = FluidConstants.BUCKET / 1000;
    private static final int TRANSFER_INTERVAL = 10;
    private static final int MAINTENANCE_INTERVAL = 100;

    private ItemStack drone = ItemStack.EMPTY;
    private final SimpleContainer outbox = new SimpleContainer(BOX_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            DronePadBlockEntity.this.setChanged();
        }
    };
    private final SimpleContainer inbox = new SimpleContainer(BOX_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            DronePadBlockEntity.this.setChanged();
        }
    };
    private final Storage<ItemVariant> outboxStorage = ContainerStorage.of(outbox, null);
    private final Storage<ItemVariant> inboxStorage = ContainerStorage.of(inbox, null);
    private final SingleVariantStorage<FluidVariant> outTank = tank();
    private final SingleVariantStorage<FluidVariant> inTank = tank();
    /** Energía del pad: paga los viajes y es la carga del dron de energía. Entra por cable. */
    public final SimpleEnergyStorage energy;
    /** Energía que trajo un dron de energía: sale por cable o se empuja a los vecinos. */
    private final SimpleEnergyStorage inEnergy;

    @SuppressWarnings("unchecked")
    private final Storage<ItemVariant> exposedItems = new CombinedStorage<>(List.of(
            FilteringStorage.insertOnlyOf(outboxStorage), FilteringStorage.extractOnlyOf(inboxStorage)));
    @SuppressWarnings("unchecked")
    private final Storage<FluidVariant> exposedFluids = new CombinedStorage<>(List.of(
            FilteringStorage.insertOnlyOf(outTank), FilteringStorage.extractOnlyOf(inTank)));
    private final EnergyStorage exposedEnergy;

    private @Nullable BlockPos link;
    private @Nullable UUID flight;
    /** Ticks del pad desde que salió el dron sin noticias de él (para darlo por perdido). */
    private int silentTicks;
    /** Ticks que lleva esperando la carga en el buzón de salida. */
    private int waitingTicks;
    private boolean surveyRequested;
    /** Tiene el cargador de chunks instalado. */
    private boolean chunkLoader;
    /** Mejoras de alcance del explorador instaladas. */
    private int surveyUpgrades;
    /** El ticket de este pad ya está puesto en esta sesión. */
    private boolean chunkForced;
    private DronePadStatus status = DronePadStatus.NO_DRONE;
    private int tickCounter;

    public DronePadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_PAD, pos, state);
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        this.energy = new SimpleEnergyStorage(config.padEnergyCapacity, config.padMaxTransfer, 0) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
        this.inEnergy = new SimpleEnergyStorage(config.padEnergyCapacity, 0, config.padMaxTransfer) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
        this.exposedEnergy = new EnergyStorage() {
            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                return energy.insert(maxAmount, transaction);
            }

            @Override
            public long extract(long maxAmount, TransactionContext transaction) {
                return inEnergy.extract(maxAmount, transaction);
            }

            @Override
            public long getAmount() {
                return energy.amount + inEnergy.amount;
            }

            @Override
            public long getCapacity() {
                return energy.getCapacity() + inEnergy.getCapacity();
            }
        };
    }

    private SingleVariantStorage<FluidVariant> tank() {
        return new SingleVariantStorage<>() {
            @Override
            protected FluidVariant getBlankVariant() {
                return FluidVariant.blank();
            }

            @Override
            protected long getCapacity(FluidVariant variant) {
                return QuarryConfig.get().drones.padTankBuckets * FluidConstants.BUCKET;
            }

            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
    }

    void serverTick(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        tickCounter++;
        updateChunkTicket(serverLevel);
        if (tickCounter == 1) {
            // Se anota en el registro de la dimensión (para la centralita) al cargarse
            report(serverLevel);
        }
        // La energía sale cada tick (los consumidores aceptan poco por vez); items y fluidos, cada tanto
        pushInbox(serverLevel, pos, tickCounter % TRANSFER_INTERVAL == 0);
        if (flight != null) {
            watchFlight(serverLevel);
            return;
        }
        if (!(drone.getItem() instanceof DroneItem droneItem)) {
            setStatus(DronePadStatus.NO_DRONE);
            return;
        }
        DroneKind kind = droneItem.kind();
        switch (kind) {
            case CARGO, TANKER, POWER -> tickTransport(serverLevel, pos, kind);
            case MAINTENANCE -> tickMaintenance(serverLevel, pos);
            case SURVEY -> tickSurvey(serverLevel, pos);
        }
    }

    // ── Transporte ──

    private void tickTransport(ServerLevel level, BlockPos pos, DroneKind kind) {
        if (link == null) {
            setStatus(DronePadStatus.NO_LINK);
            return;
        }
        int max = QuarryConfig.get().drones.maxLinkDistance;
        boolean loaded = level.isLoaded(link);
        if (link.distSqr(pos) > (double) max * max
                || (loaded && !(level.getBlockEntity(link) instanceof DronePadBlockEntity))) {
            setStatus(DronePadStatus.BAD_LINK);
            return;
        }
        if (tickCounter % TRANSFER_INTERVAL == 0) {
            pullFromNeighbours(level, pos, kind);
        }
        if (!hasCargo(kind)) {
            waitingTicks = 0;
            setStatus(DronePadStatus.LOADING);
            return;
        }
        waitingTicks++;
        if (!readyToLaunch(kind)) {
            setStatus(DronePadStatus.LOADING);
            return;
        }
        if (loaded && level.getBlockEntity(link) instanceof DronePadBlockEntity destination
                && !destination.hasRoomFor(kind)) {
            setStatus(DronePadStatus.DESTINATION_FULL);
            return;
        }
        long cost = tripCost(pos, link);
        if (energy.amount < cost) {
            setStatus(DronePadStatus.NO_POWER);
            return;
        }
        energy.amount -= cost;
        DronePayload payload = loadPayload(kind);
        launch(level, kind, link, payload, List.of());
    }

    private boolean hasCargo(DroneKind kind) {
        return switch (kind) {
            case CARGO -> !outbox.isEmpty();
            case TANKER -> outTank.amount > 0;
            case POWER -> energy.amount > tripCost(worldPosition, link == null ? worldPosition : link);
            default -> false;
        };
    }

    private boolean readyToLaunch(DroneKind kind) {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        boolean waitedEnough = waitingTicks >= config.launchIdleTicks;
        return switch (kind) {
            case CARGO -> waitedEnough || usedSlots(outbox) >= Math.min(BOX_SLOTS, config.cargoLaunchStacks);
            case TANKER -> waitedEnough || outTank.amount >= config.tankerLaunchBuckets * FluidConstants.BUCKET;
            case POWER -> energy.amount - tripCost(worldPosition, link) >= config.powerLaunchEu;
            default -> false;
        };
    }

    private DronePayload loadPayload(DroneKind kind) {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        DronePayload payload = new DronePayload();
        switch (kind) {
            case CARGO -> payload.items.addAll(outbox.removeAllItems());
            case TANKER -> {
                long amount = Math.min(outTank.amount, config.tankerCapacityBuckets * FluidConstants.BUCKET);
                payload.fluid = outTank.variant;
                payload.fluidAmount = amount;
                outTank.amount -= amount;
                if (outTank.amount == 0) {
                    outTank.variant = FluidVariant.blank();
                }
            }
            case POWER -> {
                long amount = Math.min(energy.amount, config.powerCapacity);
                payload.energy = amount;
                energy.amount -= amount;
            }
            default -> {
            }
        }
        payload.items.removeIf(ItemStack::isEmpty);
        return payload;
    }

    /** Lo que llega de otro dron: entra en el buzón de llegada; lo que no entre se lo lleva de vuelta. */
    public void receive(DronePayload payload) {
        if (level instanceof ServerLevel serverLevel && !payload.isEmpty()) {
            ModAdvancements.grant(serverLevel, worldPosition, ModAdvancements.AIR_MAIL);
        }
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack stack : payload.items) {
            ItemStack left = inbox.addItem(stack);
            if (!left.isEmpty()) {
                rest.add(left);
            }
        }
        payload.items.clear();
        payload.items.addAll(rest);
        if (payload.fluidAmount > 0) {
            try (Transaction transaction = Transaction.openOuter()) {
                long accepted = inTank.insert(payload.fluid, payload.fluidAmount, transaction);
                payload.fluidAmount -= accepted;
                transaction.commit();
            }
            if (payload.fluidAmount <= 0) {
                payload.fluid = FluidVariant.blank();
            }
        }
        if (payload.energy > 0) {
            long accepted = Math.min(payload.energy, inEnergy.getCapacity() - inEnergy.amount);
            inEnergy.amount += accepted;
            payload.energy -= accepted;
        }
        setChanged();
    }

    public int surveyUpgrades() {
        return surveyUpgrades;
    }

    /** Instala una mejora de alcance del explorador; false si ya no entran más. */
    public boolean installSurveyUpgrade() {
        if (surveyUpgrades >= QuarryConfig.get().drones.surveyRangeMax) {
            return false;
        }
        surveyUpgrades++;
        markForSync();
        return true;
    }

    // ── Cargador de chunks ──

    public boolean hasChunkLoader() {
        return chunkLoader;
    }

    /** Instala el cargador; false si ya lo tenía. */
    public boolean installChunkLoader() {
        if (chunkLoader) {
            return false;
        }
        chunkLoader = true;
        markForSync();
        return true;
    }

    /** Mantiene el chunk del pad cargado (mismo ticket que las quarries) mientras tenga el cargador. */
    private void updateChunkTicket(ServerLevel level) {
        boolean wanted = chunkLoader && QuarryConfig.get().chunkLoading;
        if (wanted && !chunkForced) {
            ModTickets.force(level, ChunkPos.containing(worldPosition), true);
            chunkForced = true;
        } else if (!wanted && chunkForced) {
            releaseChunk(level);
        }
    }

    private void releaseChunk(ServerLevel level) {
        if (chunkForced) {
            ModTickets.force(level, ChunkPos.containing(worldPosition), false);
            chunkForced = false;
        }
    }

    private boolean hasRoomFor(DroneKind kind) {
        return switch (kind) {
            case CARGO -> usedSlots(inbox) < BOX_SLOTS;
            case TANKER -> inTank.amount < inTank.getCapacity();
            case POWER -> inEnergy.amount < inEnergy.getCapacity();
            default -> true;
        };
    }

    // ── Mantenimiento ──

    /** Busca, en los chunks cargados alrededor, una quarry o plataforma que necesite broca nueva. */
    private void tickMaintenance(ServerLevel level, BlockPos pos) {
        setStatus(DronePadStatus.STANDBY);
        if (tickCounter % MAINTENANCE_INTERVAL != 0) {
            return;
        }
        int bitSlot = firstBitSlot();
        if (bitSlot < 0) {
            return;
        }
        BlockPos machine = findWornMachine(level, pos);
        if (machine == null) {
            return;
        }
        long cost = tripCost(pos, machine);
        if (energy.amount < cost) {
            setStatus(DronePadStatus.NO_POWER);
            return;
        }
        UUID id = UUID.randomUUID();
        if (!DroneFlights.claim(machine, id)) {
            return;
        }
        energy.amount -= cost;
        DronePayload payload = new DronePayload();
        payload.items.add(outbox.removeItem(bitSlot, 1));
        launch(level, DroneKind.MAINTENANCE, machine, payload, List.of(), id);
    }

    private int firstBitSlot() {
        for (int i = 0; i < BOX_SLOTS; i++) {
            if (outbox.getItem(i).getItem() instanceof DrillBitItem) {
                return i;
            }
        }
        return -1;
    }

    private @Nullable BlockPos findWornMachine(ServerLevel level, BlockPos pos) {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        int chunks = (config.maintenanceRadius >> 4) + 1;
        ChunkPos center = ChunkPos.containing(pos);
        double radiusSqr = (double) config.maintenanceRadius * config.maintenanceRadius;
        for (int cx = center.x() - chunks; cx <= center.x() + chunks; cx++) {
            for (int cz = center.z() - chunks; cz <= center.z() + chunks; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos machine = be.getBlockPos();
                    if (machine.distSqr(pos) > radiusSqr || DroneFlights.claimed(machine)) {
                        continue;
                    }
                    if (needsBit(be, config.maintenanceWear)) {
                        return machine;
                    }
                }
            }
        }
        return null;
    }

    private static boolean needsBit(BlockEntity be, double wear) {
        if (be instanceof QuarryBlockEntity quarry) {
            ItemStack bit = quarry.bitSlot().getItem(0);
            return bit.isEmpty() ? quarry.status() == QuarryStatus.NO_BIT : DrillBitItem.wear(bit) >= wear;
        }
        if (be instanceof OffshoreRigBlockEntity rig) {
            ItemStack bit = rig.bit();
            boolean drilling = rig.status() == OffshoreStatus.DRILLING || rig.status() == OffshoreStatus.NO_BIT;
            return drilling && (bit.isEmpty() || DrillBitItem.wear(bit) >= wear);
        }
        return false;
    }

    // ── Exploración ──

    /** Clic derecho en el pad con el explorador: lo manda a volar. */
    public void requestSurvey() {
        if (drone.getItem() instanceof DroneItem item && item.kind() == DroneKind.SURVEY && flight == null) {
            surveyRequested = true;
        }
    }

    private void tickSurvey(ServerLevel level, BlockPos pos) {
        if (!surveyRequested) {
            setStatus(DronePadStatus.READY);
            return;
        }
        List<BlockPos> route = surveyRoute(pos, surveyUpgrades);
        long cost = tripCost(pos, route.getFirst()) * 4;
        if (energy.amount < cost) {
            setStatus(DronePadStatus.NO_POWER);
            return;
        }
        surveyRequested = false;
        energy.amount -= cost;
        launch(level, DroneKind.SURVEY, pos, new DronePayload(), route);
    }

    /** Un barrido en espiral cuadrada alrededor del pad, con pasadas separadas por el ancho que mira el dron. */
    private static List<BlockPos> surveyRoute(BlockPos pos, int upgrades) {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        int step = Math.max(4, config.surveyScanRadius * 2);
        List<BlockPos> route = new ArrayList<>();
        int x = 0;
        int z = 0;
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        int leg = 1;
        int direction = 0;
        while (Math.max(Math.abs(x), Math.abs(z)) <= config.surveyRadius + upgrades * config.surveyRangeBonus) {
            for (int repeat = 0; repeat < 2; repeat++) {
                x += directions[direction][0] * leg * step;
                z += directions[direction][1] * leg * step;
                route.add(pos.offset(x, 0, z));
                direction = (direction + 1) % 4;
            }
            leg++;
        }
        return route;
    }

    // ── Vuelos ──

    private void launch(ServerLevel level, DroneKind kind, BlockPos target, DronePayload payload,
                        List<BlockPos> waypoints) {
        launch(level, kind, target, payload, waypoints, UUID.randomUUID());
    }

    private void launch(ServerLevel level, DroneKind kind, BlockPos target, DronePayload payload,
                        List<BlockPos> waypoints, UUID id) {
        flight = id;
        silentTicks = 0;
        waitingTicks = 0;
        DroneFlights.seen(id, level.getGameTime());
        DroneEntity.launch(level, kind, id, worldPosition, target, payload, waypoints);
        setStatus(DronePadStatus.IN_FLIGHT);
        markForSync();
    }

    /** Si no se sabe nada del dron por mucho tiempo, se da por perdido y vuelve al pad (sin lo que llevaba). */
    private void watchFlight(ServerLevel level) {
        setStatus(DronePadStatus.IN_FLIGHT);
        long seen = DroneFlights.lastSeen(flight);
        if (seen >= 0 && level.getGameTime() - seen < 40) {
            silentTicks = 0;
            return;
        }
        silentTicks++;
        if (silentTicks > QuarryConfig.get().drones.lostAfterTicks) {
            DroneFlights.finished(flight);
            flight = null;
            setStatus(DronePadStatus.LOST);
            markForSync();
        }
    }

    /** El dron del vuelo {@code id} cayó (lo bajó un jugador): vuelve a estar en el pad, sin la carga. */
    public void recall(@Nullable UUID id) {
        if (flight != null && flight.equals(id)) {
            DroneFlights.finished(flight);
            flight = null;
            markForSync();
        }
    }

    /** Vuelve el dron: descarga lo que trae al buzón de llegada. Devuelve false si no es el que esperaba. */
    public boolean dock(DroneEntity drone) {
        if (flight == null || !flight.equals(drone.flightId())) {
            return false;
        }
        DronePayload payload = drone.payload();
        if (drone.kind() == DroneKind.SURVEY) {
            if (!payload.sites.isEmpty() && level instanceof ServerLevel serverLevel) {
                ModAdvancements.grant(serverLevel, worldPosition, ModAdvancements.PROSPECTOR);
                DroneNetwork.get(serverLevel).addSites(payload.sites, worldPosition);
            }
            ItemStack report = new ItemStack(ModItems.SURVEY_REPORT);
            report.set(ModComponents.SURVEY_SITES, List.copyOf(payload.sites));
            payload.items.add(report);
        }
        // Lo que no se pudo entregar vuelve al buzón de salida; lo que trae (brocas gastadas, informes) al de llegada
        for (ItemStack stack : payload.items) {
            ItemStack left = drone.kind() == DroneKind.CARGO ? outbox.addItem(stack) : inbox.addItem(stack);
            if (!left.isEmpty() && level != null) {
                Block.popResource(level, worldPosition.above(), left);
            }
        }
        if (payload.fluidAmount > 0) {
            try (Transaction transaction = Transaction.openOuter()) {
                outTank.insert(payload.fluid, payload.fluidAmount, transaction);
                transaction.commit();
            }
        }
        energy.amount = Math.min(energy.getCapacity(), energy.amount + payload.energy);
        DroneFlights.finished(flight);
        flight = null;
        markForSync();
        return true;
    }

    private long tripCost(BlockPos from, BlockPos to) {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        return config.tripBaseEu + Math.round(Math.sqrt(from.distSqr(to)) * config.tripEuPerBlock);
    }

    // ── Vecinos ──

    /** Origen: tira de los cofres o tanques pegados (los pads no cuentan). */
    private void pullFromNeighbours(ServerLevel level, BlockPos pos, DroneKind kind) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (level.getBlockEntity(neighbour) instanceof DronePadBlockEntity) {
                continue;
            }
            if (kind == DroneKind.CARGO) {
                Storage<ItemVariant> source = ItemStorage.SIDED.find(level, neighbour, direction.getOpposite());
                if (source != null && source.supportsExtraction()) {
                    StorageUtil.move(source, outboxStorage, variant -> true, 64, null);
                }
            } else if (kind == DroneKind.TANKER) {
                Storage<FluidVariant> source = FluidStorage.SIDED.find(level, neighbour, direction.getOpposite());
                if (source != null && source.supportsExtraction()) {
                    StorageUtil.move(source, outTank, variant -> true, 4 * FluidConstants.BUCKET, null);
                }
            }
        }
    }

    /** Destino: reparte lo que llegó a los vecinos que lo acepten. */
    private void pushInbox(ServerLevel level, BlockPos pos, boolean itemsAndFluids) {
        boolean items = itemsAndFluids && !inbox.isEmpty();
        boolean fluid = itemsAndFluids && inTank.amount > 0;
        boolean power = inEnergy.amount > 0;
        if (!items && !fluid && !power) {
            return;
        }
        long powerLeft = Math.min(inEnergy.amount, QuarryConfig.get().drones.padMaxTransfer);
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (level.getBlockEntity(neighbour) instanceof DronePadBlockEntity) {
                continue;
            }
            Direction side = direction.getOpposite();
            if (items) {
                Storage<ItemVariant> target = ItemStorage.SIDED.find(level, neighbour, side);
                if (target != null && target.supportsInsertion()) {
                    StorageUtil.move(inboxStorage, target, variant -> true, Long.MAX_VALUE, null);
                }
            }
            if (fluid) {
                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighbour, side);
                if (target != null && target.supportsInsertion()) {
                    StorageUtil.move(inTank, target, variant -> true, Long.MAX_VALUE, null);
                }
            }
            if (power && powerLeft > 0) {
                EnergyStorage target = EnergyStorage.SIDED.find(level, neighbour, side);
                if (target != null && target.supportsInsertion()) {
                    try (Transaction transaction = Transaction.openOuter()) {
                        long moved = target.insert(powerLeft, transaction);
                        inEnergy.amount -= moved;
                        powerLeft -= moved;
                        transaction.commit();
                    }
                }
            }
        }
    }

    private static int usedSlots(SimpleContainer container) {
        int used = 0;
        for (ItemStack stack : container.getItems()) {
            if (!stack.isEmpty()) {
                used++;
            }
        }
        return used;
    }

    // ── Dron en el pad ──

    public boolean insertDrone(ItemStack stack) {
        if (!drone.isEmpty() || flight != null) {
            return false;
        }
        drone = stack;
        markForSync();
        return true;
    }

    public ItemStack takeDrone() {
        if (flight != null) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = drone;
        drone = ItemStack.EMPTY;
        markForSync();
        return taken;
    }

    public void setLink(@Nullable BlockPos link) {
        this.link = link;
        markForSync();
    }

    public @Nullable BlockPos link() {
        return link;
    }

    /** El tipo de dron del pad, o null si no tiene. */
    public @Nullable DroneKind droneKind() {
        return drone.getItem() instanceof DroneItem item ? item.kind() : null;
    }

    /** Si el dron está en el pad (para el renderer, que lo dibuja posado). */
    public boolean droneDocked() {
        return droneKind() != null && flight == null && status != DronePadStatus.IN_FLIGHT;
    }

    public DronePadStatus status() {
        return status;
    }

    @Override
    public InfoKind infoKind() {
        return InfoKind.PAD;
    }

    @Override
    public void fillInfo(long[] values) {
        DroneKind kind = droneKind();
        values[0] = status.ordinal();
        values[1] = kind == null ? 0 : kind.ordinal() + 1;
        values[2] = link == null ? 0 : 1;
        values[3] = link == null ? 0 : link.getX();
        values[4] = link == null ? 0 : link.getY();
        values[5] = link == null ? 0 : link.getZ();
        values[6] = energy.amount;
        values[7] = energy.getCapacity();
        values[8] = inEnergy.amount;
        values[9] = inEnergy.getCapacity();
        values[10] = outTank.amount / MB;
        values[11] = outTank.getCapacity() / MB;
        values[12] = inTank.amount / MB;
        values[13] = inTank.getCapacity() / MB;
        values[14] = usedSlots(outbox);
        values[15] = usedSlots(inbox);
        values[16] = link == null ? 0 : tripCost(worldPosition, link);
        values[17] = chunkLoader ? 1 : 0;
        values[18] = surveyUpgrades;
    }

    /** El botón de la GUI: manda a explorar al dron explorador. */
    @Override
    public void onInfoButton(ServerPlayer player) {
        requestSurvey();
        player.sendOverlayMessage(statusMessage());
    }

    public Component statusMessage() {
        DroneKind kind = droneKind();
        Component droneName = kind != null ? kind.label() : Component.translatable("status.quarryindustrial.drone_pad.none");
        Component target = link != null
                ? Component.literal(link.getX() + " " + link.getY() + " " + link.getZ())
                : Component.literal("-");
        return Component.translatable("message.quarryindustrial.drone_pad.status", status.label(), droneName,
                target, energy.amount, energy.getCapacity(), usedSlots(outbox), outTank.amount / MB,
                usedSlots(inbox), inTank.amount / MB);
    }

    /** Lo primero que hay en el buzón de llegada (para Jade y las pruebas). */
    public ItemStack peekInbox() {
        for (ItemStack stack : inbox.getItems()) {
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public Storage<ItemVariant> itemStorage() {
        return exposedItems;
    }

    public Storage<FluidVariant> fluidStorage() {
        return exposedFluids;
    }

    public EnergyStorage energyStorage() {
        return exposedEnergy;
    }

    private void setStatus(DronePadStatus status) {
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
        if (level instanceof ServerLevel serverLevel) {
            report(serverLevel);
        }
    }

    private void report(ServerLevel level) {
        DroneNetwork.get(level).updatePad(worldPosition, droneKind(), status, link);
    }

    /** Lo que se suelta al romper el pad: el dron (si está) y los dos buzones. */
    public List<ItemStack> contentsToDrop() {
        List<ItemStack> all = new ArrayList<>();
        if (flight == null && !drone.isEmpty()) {
            all.add(drone);
            drone = ItemStack.EMPTY;
        }
        if (surveyUpgrades > 0) {
            all.add(new ItemStack(ModItems.SURVEY_RANGE_UPGRADE, surveyUpgrades));
            surveyUpgrades = 0;
        }
        if (chunkLoader) {
            all.add(new ItemStack(ModItems.PAD_CHUNK_LOADER));
            chunkLoader = false;
        }
        all.addAll(outbox.removeAllItems());
        all.addAll(inbox.removeAllItems());
        return all;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            for (ItemStack stack : contentsToDrop()) {
                Block.popResource(level, pos, stack);
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            releaseChunk(serverLevel);
            DroneNetwork.get(serverLevel).removePad(pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ── Sincronización y persistencia ──

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        output.store("Drone", ItemStack.OPTIONAL_CODEC, drone);
        output.putString("Status", status.name());
        output.putBoolean("InFlight", flight != null);
        return output.buildResult();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Drone", ItemStack.OPTIONAL_CODEC, drone);
        output.putString("Status", status.name());
        output.putBoolean("InFlight", flight != null);
        if (flight != null) {
            output.store("Flight", UUIDUtil.CODEC, flight);
        }
        if (link != null) {
            output.store("Link", BlockPos.CODEC, link);
        }
        output.store("Outbox", ItemStack.OPTIONAL_CODEC.listOf(), outbox.getItems());
        output.store("Inbox", ItemStack.OPTIONAL_CODEC.listOf(), inbox.getItems());
        SingleVariantStorage.writeValue(outTank, FluidVariant.CODEC, output.child("OutTank"));
        SingleVariantStorage.writeValue(inTank, FluidVariant.CODEC, output.child("InTank"));
        output.putBoolean("ChunkLoader", chunkLoader);
        output.putInt("SurveyUpgrades", surveyUpgrades);
        output.putLong("Energy", energy.amount);
        output.putLong("InEnergy", inEnergy.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        drone = input.read("Drone", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        try {
            status = DronePadStatus.valueOf(input.getStringOr("Status", DronePadStatus.NO_DRONE.name()));
        } catch (IllegalArgumentException e) {
            status = DronePadStatus.NO_DRONE;
        }
        flight = input.read("Flight", UUIDUtil.CODEC).orElse(null);
        if (flight == null && input.getBooleanOr("InFlight", false)) {
            // En el cliente solo llega el booleano
            status = DronePadStatus.IN_FLIGHT;
        }
        link = input.read("Link", BlockPos.CODEC).orElse(null);
        load(outbox, input.read("Outbox", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        load(inbox, input.read("Inbox", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        SingleVariantStorage.readValue(outTank, FluidVariant.CODEC, FluidVariant::blank, input.childOrEmpty("OutTank"));
        SingleVariantStorage.readValue(inTank, FluidVariant.CODEC, FluidVariant::blank, input.childOrEmpty("InTank"));
        chunkLoader = input.getBooleanOr("ChunkLoader", false);
        surveyUpgrades = input.getIntOr("SurveyUpgrades", 0);
        energy.amount = Math.min(input.getLongOr("Energy", 0L), energy.getCapacity());
        inEnergy.amount = Math.min(input.getLongOr("InEnergy", 0L), inEnergy.getCapacity());
    }

    private static void load(SimpleContainer container, List<ItemStack> items) {
        container.clearContent();
        for (int i = 0; i < Math.min(items.size(), container.getContainerSize()); i++) {
            container.getItems().set(i, items.get(i));
        }
    }
}
