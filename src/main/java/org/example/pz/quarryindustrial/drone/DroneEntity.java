package org.example.pz.quarryindustrial.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.registry.ModAdvancements;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.compat.AlaDistillery;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.OilFluids;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.registry.ModEntities;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.example.pz.quarryindustrial.registry.ModTickets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Un dron en vuelo. Despega vertical hasta la altura de crucero, vuela en línea recta, baja sobre el destino,
 * hace su trabajo (entregar, cambiar una broca, explorar) y vuelve a su pad, donde vuelve a ser un item.
 * Atraviesa los bloques (va por arriba de todo, pero si encuentra una montaña sube). Mantiene cargado el chunk
 * donde está. Un jugador lo puede bajar de un golpe: suelta el dron y lo que llevaba.
 */
public class DroneEntity extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(DroneEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LOADED = SynchedEntityData.defineId(DroneEntity.class,
            EntityDataSerializers.BOOLEAN);

    private enum Phase {
        ASCEND,
        CRUISE,
        DESCEND
    }

    private @Nullable UUID flightId;
    private BlockPos home = BlockPos.ZERO;
    /** Destino de ida: el pad vinculado o la máquina a atender. El explorador usa {@link #waypoints}. */
    private BlockPos target = BlockPos.ZERO;
    private final List<BlockPos> waypoints = new ArrayList<>();
    private int waypoint;
    private boolean returning;
    private Phase phase = Phase.ASCEND;
    private double cruiseY;
    private final DronePayload payload = new DronePayload();
    private boolean docked;
    /** Chunk del último ticket de carga: al cambiar de chunk se renueva enseguida. */
    private long ticketChunk = Long.MIN_VALUE;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** Sale de {@code home} hacia {@code target} con {@code payload}. */
    public static DroneEntity launch(ServerLevel level, DroneKind kind, UUID flight, BlockPos home, BlockPos target,
                                     DronePayload payload, List<BlockPos> waypoints) {
        DroneEntity drone = new DroneEntity(ModEntities.DRONE, level);
        drone.flightId = flight;
        drone.home = home.immutable();
        drone.target = target.immutable();
        drone.waypoints.addAll(waypoints);
        drone.entityData.set(KIND, kind.ordinal());
        drone.copyPayload(payload);
        drone.setPos(Vec3.atBottomCenterOf(home).add(0, 0.3, 0));
        drone.startLeg();
        level.addFreshEntity(drone);
        level.playSound(null, home, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 0.6f, 1.6f);
        return drone;
    }

    private void copyPayload(DronePayload from) {
        payload.items.clear();
        from.items.forEach(stack -> payload.items.add(stack.copy()));
        payload.fluid = from.fluid;
        payload.fluidAmount = from.fluidAmount;
        payload.energy = from.energy;
        payload.sites.clear();
        payload.sites.addAll(from.sites);
        entityData.set(LOADED, !payload.isEmpty());
    }

    public DroneKind kind() {
        return DroneKind.byOrdinal(entityData.get(KIND));
    }

    /** Si lleva algo colgando (para el renderer). */
    public boolean loaded() {
        return entityData.get(LOADED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0);
        builder.define(LOADED, false);
    }

    // ── Vuelo ──

    private Vec3 goal() {
        if (returning) {
            return Vec3.atBottomCenterOf(home).add(0, 0.3, 0);
        }
        if (kind() == DroneKind.SURVEY && waypoint < waypoints.size()) {
            return Vec3.atBottomCenterOf(waypoints.get(waypoint));
        }
        // Mantenimiento: se para encima de la máquina; los demás, sobre el pad de destino
        double above = kind() == DroneKind.MAINTENANCE ? 1.2 : 0.3;
        return Vec3.atBottomCenterOf(target).add(0, above, 0);
    }

    private void startLeg() {
        Vec3 goal = goal();
        int top = level().getMaxY() - 2;
        cruiseY = Math.min(top, Math.max(getY(), goal.y) + QuarryConfig.get().drones.cruiseAltitude);
        phase = Phase.ASCEND;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (flightId != null) {
            DroneFlights.seen(flightId, level.getGameTime());
        }
        ChunkPos chunk = ChunkPos.containing(blockPosition());
        if (tickCount % 20 == 1 || chunk.pack() != ticketChunk) {
            ticketChunk = chunk.pack();
            ModTickets.keepDroneLoaded(level, chunk);
        }
        fly(level);
    }

    private void fly(ServerLevel level) {
        double speed = QuarryConfig.get().drones.speed;
        Vec3 pos = position();
        Vec3 goal = goal();
        boolean surveying = kind() == DroneKind.SURVEY && !returning;
        switch (phase) {
            case ASCEND -> {
                if (pos.y < cruiseY - 1e-3) {
                    setPos(pos.x, Math.min(cruiseY, pos.y + speed * 0.6), pos.z);
                } else {
                    phase = Phase.CRUISE;
                }
            }
            case CRUISE -> {
                double dx = goal.x - pos.x;
                double dz = goal.z - pos.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                setYRot((float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG));
                BlockPos ahead = BlockPos.containing(pos.x + dx / Math.max(distance, 1e-6) * 2, pos.y, pos.z + dz / Math.max(distance, 1e-6) * 2);
                if (level.isLoaded(ahead) && !level.getBlockState(ahead).isAir()) {
                    // Montaña adelante: sube antes de seguir
                    setPos(pos.x, Math.min(level.getMaxY() - 2, pos.y + speed), pos.z);
                    cruiseY = getY();
                    break;
                }
                if (surveying) {
                    scan(level);
                }
                if (distance <= speed) {
                    setPos(goal.x, pos.y, goal.z);
                    if (surveying) {
                        waypoint++;
                        if (waypoint >= waypoints.size()) {
                            returning = true;
                            startLeg();
                        }
                    } else {
                        phase = Phase.DESCEND;
                    }
                } else {
                    setPos(pos.x + dx / distance * speed, pos.y, pos.z + dz / distance * speed);
                }
            }
            case DESCEND -> {
                if (pos.y > goal.y + 1e-3) {
                    setPos(pos.x, Math.max(goal.y, pos.y - speed * 0.5), pos.z);
                } else {
                    arrive(level);
                }
            }
        }
    }

    /** Explorador: mira la superficie alrededor; un chorro de petróleo arriba de todo es un géiser. */
    private void scan(ServerLevel level) {
        if (tickCount % 4 != 0) {
            return;
        }
        int radius = QuarryConfig.get().drones.surveyScanRadius;
        BlockPos center = blockPosition();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.isLoaded(new BlockPos(x, center.getY(), z))) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                BlockPos surface = new BlockPos(x, top, z);
                if (OilFluids.isOil(level.getBlockState(surface)) && payload.sites.stream()
                        .noneMatch(site -> site.distSqr(surface) < 32 * 32)) {
                    payload.sites.add(surface);
                    entityData.set(LOADED, true);
                }
            }
        }
    }

    private void arrive(ServerLevel level) {
        if (returning) {
            if (level.getBlockEntity(home) instanceof DronePadBlockEntity pad) {
                if (!pad.dock(this)) {
                    // El pad lo dio por perdido y ya lo tiene de vuelta: solo queda la carga
                    dropPayload(level);
                }
                finish();
            } else {
                // El pad ya no está: el dron queda como item, con su carga
                spawnAtLocation(level, new ItemStack(ModItems.drone(kind())));
                dropPayload(level);
                finish();
            }
            return;
        }
        switch (kind()) {
            case CARGO, TANKER, POWER -> {
                if (level.getBlockEntity(target) instanceof DronePadBlockEntity pad) {
                    pad.receive(payload);
                }
            }
            case MAINTENANCE -> service(level);
            case SURVEY -> {
            }
        }
        entityData.set(LOADED, !payload.isEmpty());
        returning = true;
        startLeg();
    }

    /** Cambia la broca (deja la nueva, que es la única que lleva, y se lleva la gastada) o limpia una columna. */
    private void service(ServerLevel level) {
        BlockEntity machine = level.getBlockEntity(target);
        if (AlaDistillery.isColumn(machine)) {
            // Una columna de destilación sucia: se limpia y el carbón vuelve al pad
            int coal = AlaDistillery.clean(machine);
            if (coal > 0) {
                payload.items.add(new ItemStack(Items.COAL, coal));
                level.playSound(null, target, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
            }
            return;
        }
        ItemStack fresh = payload.items.isEmpty() ? ItemStack.EMPTY : payload.items.removeFirst();
        if (fresh.isEmpty()) {
            return;
        }
        ItemStack old;
        if (level.getBlockEntity(target) instanceof QuarryBlockEntity quarry) {
            old = quarry.swapBit(fresh);
        } else if (level.getBlockEntity(target) instanceof OffshoreRigBlockEntity rig) {
            old = rig.swapBit(fresh);
        } else {
            old = fresh;
        }
        if (!old.isEmpty()) {
            payload.items.add(old);
        }
        if (old != fresh) {
            ModAdvancements.grant(level, target, ModAdvancements.PIT_CREW);
        }
        level.playSound(null, target, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.6f);
    }

    public DronePayload payload() {
        return payload;
    }

    public @Nullable UUID flightId() {
        return flightId;
    }

    public BlockPos home() {
        return home;
    }

    /** Hacia dónde va ahora mismo (para el mapa de la centralita). */
    public BlockPos heading() {
        return BlockPos.containing(goal());
    }

    public boolean returning() {
        return returning;
    }

    /** Explorador: los puntos del barrido que le faltan recorrer (vacío si ya vuelve o no es explorador). */
    public List<BlockPos> remainingRoute() {
        if (returning || kind() != DroneKind.SURVEY || waypoint >= waypoints.size()) {
            return List.of();
        }
        return List.copyOf(waypoints.subList(waypoint, waypoints.size()));
    }

    private void dropPayload(ServerLevel level) {
        for (ItemStack stack : payload.items) {
            spawnAtLocation(level, stack);
        }
        payload.items.clear();
    }

    private void finish() {
        if (flightId != null) {
            DroneFlights.finished(flightId);
        }
        docked = true;
        discard();
    }

    /**
     * Un golpe de un jugador lo baja: suelta lo que llevaba (los fluidos y la energía se pierden) y el dron
     * vuelve a aparecer en su pad. El item del dron vive siempre en el pad, así no se duplica.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof Player) || docked) {
            return false;
        }
        dropPayload(level);
        if (level.getBlockEntity(home) instanceof DronePadBlockEntity pad) {
            pad.recall(flightId);
        } else {
            spawnAtLocation(level, new ItemStack(ModItems.drone(kind())));
        }
        finish();
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    // ── Persistencia ──

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (flightId != null) {
            output.store("Flight", UUIDUtil.CODEC, flightId);
        }
        output.putInt("Kind", entityData.get(KIND));
        output.store("Home", BlockPos.CODEC, home);
        output.store("Target", BlockPos.CODEC, target);
        output.store("Waypoints", BlockPos.CODEC.listOf(), List.copyOf(waypoints));
        output.putInt("Waypoint", waypoint);
        output.putBoolean("Returning", returning);
        output.putString("Phase", phase.name());
        output.putDouble("CruiseY", cruiseY);
        payload.save(output.child("Payload"));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        flightId = input.read("Flight", UUIDUtil.CODEC).orElse(null);
        entityData.set(KIND, input.getIntOr("Kind", 0));
        home = input.read("Home", BlockPos.CODEC).orElse(BlockPos.ZERO);
        target = input.read("Target", BlockPos.CODEC).orElse(BlockPos.ZERO);
        waypoints.clear();
        waypoints.addAll(input.read("Waypoints", BlockPos.CODEC.listOf()).orElse(List.of()));
        waypoint = input.getIntOr("Waypoint", 0);
        returning = input.getBooleanOr("Returning", false);
        try {
            phase = Phase.valueOf(input.getStringOr("Phase", Phase.ASCEND.name()));
        } catch (IllegalArgumentException e) {
            phase = Phase.ASCEND;
        }
        cruiseY = input.getDoubleOr("CruiseY", getY());
        payload.load(input.childOrEmpty("Payload"));
        entityData.set(LOADED, !payload.isEmpty());
    }
}
