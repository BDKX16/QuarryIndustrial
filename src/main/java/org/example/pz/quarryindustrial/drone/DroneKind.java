package org.example.pz.quarryindustrial.drone;

import net.minecraft.network.chat.Component;

/**
 * Los drones del mod. Los tres primeros van y vienen entre dos pads vinculados llevando carga; los otros dos
 * trabajan solos alrededor de su pad.
 */
public enum DroneKind {
    /** Items: vacía el buffer de una quarry y lo lleva a un cofre lejano, sin tuberías. */
    CARGO(0xFFE6AE12, true),
    /** Fluidos: el crudo de una plataforma offshore hasta la costa, el diésel de vuelta. */
    TANKER(0xFF2A5BD8, true),
    /** Energía: una batería voladora para quarries lejos de la red. */
    POWER(0xFFFF3D5A, true),
    /** Cambia solo las brocas gastadas de las quarries y plataformas cercanas. */
    MAINTENANCE(0xFF3CE87A, false),
    /** Sobrevuela la zona buscando géiseres de petróleo y vuelve con un informe. */
    SURVEY(0xFFB050FF, false);

    /** Color de la franja y de las luces del dron (ARGB). */
    private final int color;
    private final boolean needsLink;

    DroneKind(int color, boolean needsLink) {
        this.color = color;
        this.needsLink = needsLink;
    }

    public int color() {
        return color;
    }

    /** Si trabaja entre dos pads (los de transporte) o solo desde el suyo. */
    public boolean needsLink() {
        return needsLink;
    }

    public String id() {
        return name().toLowerCase();
    }

    public Component label() {
        return Component.translatable("item.quarryindustrial." + id() + "_drone");
    }

    public static DroneKind byOrdinal(int ordinal) {
        DroneKind[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CARGO;
    }
}
