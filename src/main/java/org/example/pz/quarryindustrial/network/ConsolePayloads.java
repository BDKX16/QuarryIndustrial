package org.example.pz.quarryindustrial.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.example.pz.quarryindustrial.Quarryindustrial;

import java.util.List;
import java.util.Optional;

/**
 * Los paquetes de la centralita de drones. Mientras la pantalla está abierta, el cliente pide un
 * {@link Snapshot} por segundo; las acciones (vincular, mandar a explorar, olvidar un géiser) van al servidor,
 * que contesta con un {@link Notice} y un snapshot nuevo. Todas llevan la posición de la centralita: el
 * servidor solo las acepta si el jugador está al lado de una.
 */
public final class ConsolePayloads {
    private ConsolePayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(Quarryindustrial.id("console_" + name));
    }

    // ── Cliente → servidor ──

    public record RequestSnapshot(BlockPos console) implements CustomPacketPayload {
        public static final Type<RequestSnapshot> TYPE = payloadType("request");
        public static final StreamCodec<ByteBuf, RequestSnapshot> CODEC = BlockPos.STREAM_CODEC
                .map(RequestSnapshot::new, RequestSnapshot::console);

        @Override
        public Type<RequestSnapshot> type() {
            return TYPE;
        }
    }

    /** Vincula {@code source} con {@code target}; sin target, le borra el destino. */
    public record SetLink(BlockPos console, BlockPos source, Optional<BlockPos> target) implements CustomPacketPayload {
        public static final Type<SetLink> TYPE = payloadType("link");
        public static final StreamCodec<ByteBuf, SetLink> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetLink::console,
                BlockPos.STREAM_CODEC, SetLink::source,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), SetLink::target,
                SetLink::new);

        @Override
        public Type<SetLink> type() {
            return TYPE;
        }
    }

    public record LaunchSurvey(BlockPos console, BlockPos pad) implements CustomPacketPayload {
        public static final Type<LaunchSurvey> TYPE = payloadType("survey");
        public static final StreamCodec<ByteBuf, LaunchSurvey> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, LaunchSurvey::console,
                BlockPos.STREAM_CODEC, LaunchSurvey::pad,
                LaunchSurvey::new);

        @Override
        public Type<LaunchSurvey> type() {
            return TYPE;
        }
    }

    public record ForgetSite(BlockPos console, BlockPos site) implements CustomPacketPayload {
        public static final Type<ForgetSite> TYPE = payloadType("forget_site");
        public static final StreamCodec<ByteBuf, ForgetSite> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ForgetSite::console,
                BlockPos.STREAM_CODEC, ForgetSite::site,
                ForgetSite::new);

        @Override
        public Type<ForgetSite> type() {
            return TYPE;
        }
    }

    // ── Servidor → cliente ──

    /**
     * Un pad: {@code kind} es el ordinal del dron (-1 sin dron), {@code energy} en tanto por mil (-1 si el chunk
     * no está cargado y no se sabe), {@code range} el alcance del dron en bloques según sus mejoras (0 sin dron).
     */
    public record Pad(BlockPos pos, int kind, int status, Optional<BlockPos> link, boolean loaded, int energy,
                      int range) {
        public static final StreamCodec<ByteBuf, Pad> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Pad::pos,
                ByteBufCodecs.VAR_INT, Pad::kind,
                ByteBufCodecs.VAR_INT, Pad::status,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Pad::link,
                ByteBufCodecs.BOOL, Pad::loaded,
                ByteBufCodecs.VAR_INT, Pad::energy,
                ByteBufCodecs.VAR_INT, Pad::range,
                Pad::new);
    }

    /** Un dron en vuelo: dónde está, a dónde va y, si es un explorador barriendo, lo que le falta del barrido. */
    public record Flight(int kind, float x, float z, BlockPos home, BlockPos heading, boolean returning,
                         List<BlockPos> route) {
        public static final StreamCodec<ByteBuf, Flight> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Flight::kind,
                ByteBufCodecs.FLOAT, Flight::x,
                ByteBufCodecs.FLOAT, Flight::z,
                BlockPos.STREAM_CODEC, Flight::home,
                BlockPos.STREAM_CODEC, Flight::heading,
                ByteBufCodecs.BOOL, Flight::returning,
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), Flight::route,
                Flight::new);
    }

    public record Site(BlockPos pos, BlockPos finder) {
        public static final StreamCodec<ByteBuf, Site> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Site::pos,
                BlockPos.STREAM_CODEC, Site::finder,
                Site::new);
    }

    /** Todo lo que ve la centralita, dentro de {@code range} bloques. */
    public record Snapshot(BlockPos console, int range, int maxLinkDistance, List<Pad> pads, List<Flight> flights,
                           List<Site> sites) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = payloadType("snapshot");
        public static final StreamCodec<ByteBuf, Snapshot> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Snapshot::console,
                ByteBufCodecs.VAR_INT, Snapshot::range,
                ByteBufCodecs.VAR_INT, Snapshot::maxLinkDistance,
                Pad.CODEC.apply(ByteBufCodecs.list()), Snapshot::pads,
                Flight.CODEC.apply(ByteBufCodecs.list()), Snapshot::flights,
                Site.CODEC.apply(ByteBufCodecs.list()), Snapshot::sites,
                Snapshot::new);

        @Override
        public Type<Snapshot> type() {
            return TYPE;
        }
    }

    /** El resultado de una acción, para la barra de abajo de la pantalla. */
    public record Notice(Component message, boolean ok) implements CustomPacketPayload {
        public static final Type<Notice> TYPE = payloadType("notice");
        public static final StreamCodec<RegistryFriendlyByteBuf, Notice> CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, Notice::message,
                ByteBufCodecs.BOOL, Notice::ok,
                Notice::new);

        @Override
        public Type<Notice> type() {
            return TYPE;
        }
    }
}
