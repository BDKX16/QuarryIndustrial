package org.example.pz.quarryindustrial.menu;

import net.minecraft.network.chat.Component;
import org.example.pz.quarryindustrial.block.drone.DronePadStatus;
import org.example.pz.quarryindustrial.block.offshore.OffshoreStatus;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackStatus;
import org.example.pz.quarryindustrial.drone.DroneKind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Las máquinas con GUI de solo lectura y qué muestra cada una. Cada campo ocupa algunos valores de un arreglo
 * de {@code long} que llena la máquina en {@link InfoMachine#fillInfo}, en el mismo orden que esta lista.
 */
public enum InfoKind {
    PAD("drone_pad", List.of(
            Field.choice("status", options(DronePadStatus.values(), DronePadStatus::label),
                    statusColors(DronePadStatus.values())),
            Field.choice("drone", droneOptions(), null),
            Field.coords("link"),
            Field.bar("energy", "EU", 0xFFFFD24A, 0xFFFF3D5A),
            Field.bar("energy_in", "EU", 0xFFFFD24A, 0xFFFF3D5A),
            Field.bar("fluid_out", "mB", 0xFF6FB8FF, 0xFF2A5BD8),
            Field.bar("fluid_in", "mB", 0xFF6FB8FF, 0xFF2A5BD8),
            Field.number("items_out", ""),
            Field.number("items_in", ""),
            Field.number("trip_cost", "EU"),
            Field.choice("chunk_loader", yesNo(), null),
            Field.number("survey_upgrades", ""))),
    PUMPJACK("pumpjack", List.of(
            Field.choice("status", options(PumpjackStatus.values(), PumpjackStatus::label),
                    statusColors(PumpjackStatus.values())),
            Field.bar("energy", "EU", 0xFFFFD24A, 0xFFFF3D5A),
            Field.bar("oil", "mB", 0xFF5A4A3A, 0xFF1A1410),
            Field.bar("fuel", "mB", 0xFFE0B040, 0xFF8A5A10),
            Field.number("speed_upgrades", ""))),
    OFFSHORE("offshore_rig", List.of(
            Field.choice("status", options(OffshoreStatus.values(), OffshoreStatus::label),
                    statusColors(OffshoreStatus.values())),
            Field.bar("depth", "m", 0xFF8AA0B0, 0xFF3A4A58),
            Field.bar("energy", "EU", 0xFFFFD24A, 0xFFFF3D5A),
            Field.bar("oil", "mB", 0xFF5A4A3A, 0xFF1A1410),
            Field.bar("fuel", "mB", 0xFFE0B040, 0xFF8A5A10),
            Field.number("speed_upgrades", ""))),
    DIESEL("diesel_generator", List.of(
            Field.choice("status", generatorStates(), new int[]{0xFF404040, 0xFF1E8A3C}),
            Field.number("output", "EU/t"),
            Field.bar("energy", "EU", 0xFFFFD24A, 0xFFFF3D5A),
            Field.bar("fuel", "mB", 0xFFE0B040, 0xFF8A5A10))),
    TURBINE("steam_turbine", List.of(
            Field.choice("status", generatorStates(), new int[]{0xFF404040, 0xFF1E8A3C}),
            Field.number("output", "EU/t"),
            Field.bar("energy", "EU", 0xFFFFD24A, 0xFFFF3D5A),
            Field.bar("steam", "mB", 0xFFE8E8F0, 0xFF9AA0B0)));

    public enum Type {
        /** Una opción de una lista (estado, sí/no): guarda el índice. */
        CHOICE(1),
        NUMBER(1),
        /** Barra con "actual / máximo": guarda los dos. */
        BAR(2),
        /** Una posición: si hay una, y x, y, z. */
        COORDS(4);

        private final int values;

        Type(int values) {
            this.values = values;
        }

        public int width() {
            return values;
        }
    }

    public record Field(Type type, String name, String unit, List<Component> options, int[] colors, int top,
                        int bottom) {
        static Field choice(String name, List<Component> options, int[] colors) {
            return new Field(Type.CHOICE, name, "", options, colors, 0, 0);
        }

        static Field number(String name, String unit) {
            return new Field(Type.NUMBER, name, unit, List.of(), null, 0, 0);
        }

        static Field bar(String name, String unit, int top, int bottom) {
            return new Field(Type.BAR, name, unit, List.of(), null, top, bottom);
        }

        static Field coords(String name) {
            return new Field(Type.COORDS, name, "", List.of(), null, 0, 0);
        }

        public Component label() {
            return Component.translatable("gui.quarryindustrial.info." + name);
        }
    }

    private final String blockId;
    private final List<Field> fields;
    private final int valueCount;

    InfoKind(String blockId, List<Field> fields) {
        this.blockId = blockId;
        this.fields = fields;
        this.valueCount = fields.stream().mapToInt(field -> field.type().width()).sum();
    }

    public List<Field> fields() {
        return fields;
    }

    /** Cantidad de {@code long} que usa en total (la GUI manda cada uno en dos mitades de 16 bits). */
    public int valueCount() {
        return valueCount;
    }

    public Component title() {
        return Component.translatable("block.quarryindustrial." + blockId);
    }

    /** El pad tiene un botón para mandar a explorar, que solo sirve con el explorador adentro. */
    public boolean hasAction() {
        return this == PAD;
    }

    /** Índice (dentro del arreglo de valores) donde empieza el campo {@code field}. */
    public int offset(int field) {
        int offset = 0;
        for (int i = 0; i < field; i++) {
            offset += fields.get(i).type().width();
        }
        return offset;
    }

    private static <E extends Enum<E>> List<Component> options(E[] values, Function<E, Component> label) {
        List<Component> list = new ArrayList<>();
        for (E value : values) {
            list.add(label.apply(value));
        }
        return list;
    }

    private static List<Component> droneOptions() {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("status.quarryindustrial.drone_pad.none"));
        for (DroneKind kind : DroneKind.values()) {
            list.add(kind.label());
        }
        return list;
    }

    private static List<Component> yesNo() {
        return List.of(Component.translatable("gui.quarryindustrial.info.no"),
                Component.translatable("gui.quarryindustrial.info.yes"));
    }

    private static List<Component> generatorStates() {
        return List.of(Component.translatable("status.quarryindustrial.generator.idle"),
                Component.translatable("status.quarryindustrial.generator.running"));
    }

    private static int[] statusColors(Enum<?>[] values) {
        int[] colors = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            colors[i] = statusColor(values[i].name());
        }
        return colors;
    }

    /** Color del texto de un estado: rojo si falta algo, naranja si está lleno o esperando, verde si anda. */
    private static int statusColor(String name) {
        if (name.startsWith("NO_") || name.equals("BAD_LINK") || name.equals("DAMAGED") || name.equals("LOST")) {
            return 0xFFB02020;
        }
        if (name.contains("FULL") || name.equals("LOADING")) {
            return 0xFFC05A00;
        }
        if (name.equals("IDLE") || name.equals("STANDBY")) {
            return 0xFF404040;
        }
        return 0xFF1E8A3C;
    }
}
