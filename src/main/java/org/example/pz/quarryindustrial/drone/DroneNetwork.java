package org.example.pz.quarryindustrial.drone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.drone.DronePadStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registro por dimensión de los pads de drones y de los géiseres que encontraron los exploradores. Lo usa la
 * centralita: así ve todos los pads, aunque estén en chunks descargados (con lo último que se supo de ellos).
 */
public class DroneNetwork extends SavedData {
    /** Tope de géiseres guardados: al pasarse se olvidan los más viejos. */
    public static final int MAX_SITES = 256;
    /** Dos géiseres a menos de esto son el mismo (el explorador ya los separa por 32 bloques). */
    private static final int SAME_SITE = 16;

    /** Lo último que se supo de un pad: su dron (o null), qué estaba haciendo y a dónde está vinculado. */
    public record PadInfo(BlockPos pos, @Nullable DroneKind kind, DronePadStatus status, Optional<BlockPos> link,
                          int upgrades) {
        static final Codec<PadInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(PadInfo::pos),
                Codec.STRING.optionalFieldOf("kind").forGetter(info -> Optional.ofNullable(info.kind())
                        .map(DroneKind::name)),
                Codec.STRING.fieldOf("status").forGetter(info -> info.status().name()),
                BlockPos.CODEC.optionalFieldOf("link").forGetter(PadInfo::link),
                Codec.INT.optionalFieldOf("upgrades", 0).forGetter(PadInfo::upgrades)
        ).apply(instance, (pos, kind, status, link, upgrades) -> new PadInfo(pos, kind.map(DroneNetwork::kindOrNull)
                .orElse(null), statusOr(status), link, upgrades)));
    }

    /** Un géiser visto desde el aire y el pad del explorador que lo encontró. */
    public record Site(BlockPos pos, BlockPos finder) {
        static final Codec<Site> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Site::pos),
                BlockPos.CODEC.fieldOf("finder").forGetter(Site::finder)
        ).apply(instance, Site::new));
    }

    private static final Codec<DroneNetwork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PadInfo.CODEC.listOf().fieldOf("pads").forGetter(network -> List.copyOf(network.pads.values())),
            Site.CODEC.listOf().fieldOf("sites").forGetter(network -> List.copyOf(network.sites)),
            BlockPos.CODEC.listOf().optionalFieldOf("radars", List.of()).forGetter(network -> List.copyOf(network.radars))
    ).apply(instance, DroneNetwork::new));

    // Con DataFixTypes null la carga falla (y se pierde todo); el de command storage no toca nuestros datos
    public static final SavedDataType<DroneNetwork> TYPE = new SavedDataType<>(Quarryindustrial.id("drone_network"),
            DroneNetwork::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<BlockPos, PadInfo> pads = new LinkedHashMap<>();
    private final List<Site> sites = new ArrayList<>();
    private final Set<BlockPos> radars = new LinkedHashSet<>();

    public DroneNetwork() {
    }

    private DroneNetwork(List<PadInfo> pads, List<Site> sites, List<BlockPos> radars) {
        this.radars.addAll(radars);
        pads.forEach(info -> this.pads.put(info.pos(), info));
        this.sites.addAll(sites);
    }

    public static DroneNetwork get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void updatePad(BlockPos pos, @Nullable DroneKind kind, DronePadStatus status, @Nullable BlockPos link,
                          int upgrades) {
        PadInfo info = new PadInfo(pos.immutable(), kind, status, Optional.ofNullable(link), upgrades);
        if (!info.equals(pads.get(info.pos()))) {
            pads.put(info.pos(), info);
            setDirty();
        }
    }

    public void removePad(BlockPos pos) {
        if (pads.remove(pos) != null) {
            setDirty();
        }
    }

    public void addRadar(BlockPos pos) {
        if (radars.add(pos.immutable())) {
            setDirty();
        }
    }

    public void removeRadar(BlockPos pos) {
        if (radars.remove(pos)) {
            setDirty();
        }
    }

    public List<BlockPos> radars() {
        return List.copyOf(radars);
    }

    public Collection<PadInfo> pads() {
        return pads.values();
    }

    /** Suma los géiseres de un informe (los que ya estaban no se repiten). */
    public void addSites(List<BlockPos> found, BlockPos finder) {
        for (BlockPos pos : found) {
            if (sites.stream().noneMatch(site -> site.pos().distSqr(pos) < SAME_SITE * SAME_SITE)) {
                sites.add(new Site(pos.immutable(), finder.immutable()));
                setDirty();
            }
        }
        while (sites.size() > MAX_SITES) {
            sites.removeFirst();
        }
    }

    public boolean removeSite(BlockPos pos) {
        boolean removed = sites.removeIf(site -> site.pos().equals(pos));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<Site> sites() {
        return sites;
    }

    private static @Nullable DroneKind kindOrNull(String name) {
        try {
            return DroneKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static DronePadStatus statusOr(String name) {
        try {
            return DronePadStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            return DronePadStatus.NO_DRONE;
        }
    }
}
