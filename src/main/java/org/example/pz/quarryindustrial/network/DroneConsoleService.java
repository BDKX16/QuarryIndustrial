package org.example.pz.quarryindustrial.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.block.drone.DronePadStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.drone.DroneEntity;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.drone.DroneNetwork;
import org.example.pz.quarryindustrial.network.ConsolePayloads.Flight;
import org.example.pz.quarryindustrial.network.ConsolePayloads.ForgetSite;
import org.example.pz.quarryindustrial.network.ConsolePayloads.LaunchSurvey;
import org.example.pz.quarryindustrial.network.ConsolePayloads.Notice;
import org.example.pz.quarryindustrial.network.ConsolePayloads.Pad;
import org.example.pz.quarryindustrial.network.ConsolePayloads.RequestSnapshot;
import org.example.pz.quarryindustrial.network.ConsolePayloads.SetLink;
import org.example.pz.quarryindustrial.network.ConsolePayloads.Site;
import org.example.pz.quarryindustrial.network.ConsolePayloads.Snapshot;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModEntities;
import org.example.pz.quarryindustrial.registry.ModTickets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Lado servidor de la centralita: arma lo que ve y ejecuta lo que se pide desde la pantalla. */
public final class DroneConsoleService {
    /** Hasta dónde puede estar el jugador de la centralita (como cualquier GUI de bloque). */
    private static final double REACH_SQR = 8 * 8;

    private DroneConsoleService() {
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(RequestSnapshot.TYPE, RequestSnapshot.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetLink.TYPE, SetLink.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LaunchSurvey.TYPE, LaunchSurvey.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ForgetSite.TYPE, ForgetSite.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE, Snapshot.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Notice.TYPE, Notice.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestSnapshot.TYPE, (payload, context) -> {
            if (atConsole(context.player(), payload.console())) {
                ServerPlayNetworking.send(context.player(), snapshot(context.player().level(), payload.console()));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(SetLink.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (atConsole(player, payload.console()) && inRange(coverage(player.level(), payload.console()), payload.source())) {
                respond(player, payload.console(), link(player.level(), payload.source(), payload.target().orElse(null)));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(LaunchSurvey.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (atConsole(player, payload.console()) && inRange(coverage(player.level(), payload.console()), payload.pad())) {
                respond(player, payload.console(), survey(player.level(), payload.pad()));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ForgetSite.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (atConsole(player, payload.console())
                    && DroneNetwork.get(player.level()).removeSite(payload.site())) {
                respond(player, payload.console(), ok(Component.translatable("message.quarryindustrial.console.site_forgotten")));
            }
        });
    }

    private static boolean atConsole(ServerPlayer player, BlockPos console) {
        return player.level().getBlockState(console).is(ModBlocks.DRONE_CONSOLE)
                && player.distanceToSqr(Vec3.atCenterOf(console)) <= REACH_SQR;
    }

    /** Una zona circular (horizontal) que ve la centralita: ella misma o un radar repetidor. */
    private record Coverage(BlockPos center, long radius) {
        boolean contains(BlockPos pos) {
            long dx = pos.getX() - center.getX();
            long dz = pos.getZ() - center.getZ();
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    /**
     * La centralita y los radares que alcanza, directo o a través de otros radares. Los radares que ya no
     * están (rotos sin avisar, un /setblock) se borran del registro.
     */
    private static List<Coverage> coverage(ServerLevel level, BlockPos console) {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        DroneNetwork network = DroneNetwork.get(level);
        List<Coverage> zones = new ArrayList<>();
        zones.add(new Coverage(console, config.consoleRange));
        List<BlockPos> pending = new ArrayList<>();
        for (BlockPos radar : network.radars()) {
            if (level.isLoaded(radar) && !level.getBlockState(radar).is(ModBlocks.RADAR)) {
                network.removeRadar(radar);
            } else {
                pending.add(radar);
            }
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (BlockPos radar : List.copyOf(pending)) {
                if (zones.stream().anyMatch(zone -> zone.contains(radar))) {
                    zones.add(new Coverage(radar, config.radarRange));
                    pending.remove(radar);
                    grew = true;
                }
            }
        }
        return zones;
    }

    private static boolean inRange(List<Coverage> zones, BlockPos pos) {
        return zones.stream().anyMatch(zone -> zone.contains(pos));
    }

    /** Hasta dónde llega lo más lejano que ve, desde la centralita (para el texto de la pantalla). */
    private static int reach(List<Coverage> zones, BlockPos console) {
        long reach = 0;
        for (Coverage zone : zones) {
            long dx = zone.center().getX() - console.getX();
            long dz = zone.center().getZ() - console.getZ();
            reach = Math.max(reach, (long) Math.sqrt(dx * dx + dz * dz) + zone.radius());
        }
        return (int) Math.min(reach, Integer.MAX_VALUE);
    }

    private static void respond(ServerPlayer player, BlockPos console, Notice notice) {
        ServerPlayNetworking.send(player, notice);
        ServerPlayNetworking.send(player, snapshot(player.level(), console));
    }

    private static Notice ok(Component message) {
        return new Notice(message, true);
    }

    private static Notice error(Component message) {
        return new Notice(message, false);
    }

    // ── Lo que se ve ──

    /**
     * Hasta dónde llega un dron, en bloques: el vínculo de los de transporte, el radio de búsqueda del de
     * mantenimiento y el del barrido del explorador, que suma lo que dan sus mejoras.
     */
    static int range(@Nullable DroneKind kind, int surveyUpgrades) {
        if (kind == null) {
            return 0;
        }
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        return switch (kind) {
            case CARGO, TANKER, POWER -> config.maxLinkDistance;
            case MAINTENANCE -> config.maintenanceRadius;
            case SURVEY -> config.surveyRadius + surveyUpgrades * config.surveyRangeBonus;
        };
    }

    /** Los pads (con lo último que se supo de los que están descargados), los drones en vuelo y los géiseres. */
    public static Snapshot snapshot(ServerLevel level, BlockPos console) {
        DroneNetwork network = DroneNetwork.get(level);
        List<Coverage> zones = coverage(level, console);
        List<Pad> pads = new ArrayList<>();
        for (DroneNetwork.PadInfo info : List.copyOf(network.pads())) {
            BlockPos pos = info.pos();
            if (!inRange(zones, pos)) {
                continue;
            }
            if (level.isLoaded(pos)) {
                if (!(level.getBlockEntity(pos) instanceof DronePadBlockEntity pad)) {
                    // Lo sacaron sin romperlo (un /setblock, por ejemplo): se borra del registro
                    network.removePad(pos);
                    continue;
                }
                long capacity = Math.max(1, pad.energy.getCapacity());
                pads.add(new Pad(pos, ordinal(pad.droneKind()), pad.status().ordinal(), Optional.ofNullable(pad.link()),
                        true, (int) (pad.energy.amount * 1000 / capacity), range(pad.droneKind(), pad.surveyUpgrades())));
            } else {
                pads.add(new Pad(pos, ordinal(info.kind()), info.status().ordinal(), info.link(), false, -1,
                        range(info.kind(), info.upgrades())));
            }
        }
        List<Flight> flights = new ArrayList<>();
        for (DroneEntity drone : level.getEntities(ModEntities.DRONE, drone -> inRange(zones, drone.blockPosition()))) {
            flights.add(new Flight(drone.kind().ordinal(), (float) drone.getX(), (float) drone.getZ(), drone.home(),
                    drone.heading(), drone.returning(), drone.remainingRoute()));
        }
        List<Site> sites = new ArrayList<>();
        for (DroneNetwork.Site site : network.sites()) {
            if (inRange(zones, site.pos())) {
                sites.add(new Site(site.pos(), site.finder()));
            }
        }
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        return new Snapshot(console, reach(zones, console), config.maxLinkDistance, pads, flights, sites);
    }

    private static int ordinal(@Nullable DroneKind kind) {
        return kind == null ? -1 : kind.ordinal();
    }

    // ── Acciones ──

    /**
     * Las mismas reglas que el vinculador. Los pads pueden estar lejos: se carga su chunk para tocarlos (el pad
     * actualiza solo su entrada en el registro).
     */
    private static Notice link(ServerLevel level, BlockPos source, @Nullable BlockPos target) {
        level.getChunkAt(source);
        if (!(level.getBlockEntity(source) instanceof DronePadBlockEntity pad)) {
            return error(Component.translatable("message.quarryindustrial.linker.source_gone"));
        }
        if (target == null) {
            pad.setLink(null);
            return ok(Component.translatable("message.quarryindustrial.linker.cleared"));
        }
        int max = QuarryConfig.get().drones.maxLinkDistance;
        if (source.equals(target)) {
            return error(Component.translatable("message.quarryindustrial.linker.same"));
        }
        if (source.distSqr(target) > (double) max * max) {
            return error(Component.translatable("message.quarryindustrial.linker.too_far", max));
        }
        level.getChunkAt(target);
        if (!(level.getBlockEntity(target) instanceof DronePadBlockEntity)) {
            return error(Component.translatable("message.quarryindustrial.console.not_a_pad"));
        }
        pad.setLink(target.immutable());
        return ok(Component.translatable("message.quarryindustrial.linker.linked", source.getX(), source.getY(),
                source.getZ(), target.getX(), target.getY(), target.getZ(), (int) Math.sqrt(source.distSqr(target))));
    }

    /** Manda a explorar al dron del pad. Si el pad está lejos, le mantiene el chunk andando unos segundos para que salga. */
    private static Notice survey(ServerLevel level, BlockPos pos) {
        level.getChunkAt(pos);
        if (!(level.getBlockEntity(pos) instanceof DronePadBlockEntity pad) || pad.droneKind() != DroneKind.SURVEY) {
            return error(Component.translatable("message.quarryindustrial.console.not_survey"));
        }
        if (pad.status() == DronePadStatus.IN_FLIGHT) {
            return error(Component.translatable("message.quarryindustrial.console.survey_busy"));
        }
        pad.requestSurvey();
        ModTickets.keepDroneLoaded(level, ChunkPos.containing(pos));
        return ok(Component.translatable("message.quarryindustrial.console.survey_sent"));
    }
}
