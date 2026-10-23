package org.example.pz.quarryindustrial.drone;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria de los vuelos en curso: cuándo se vio por última vez cada dron (para que su pad lo dé
 * por perdido si desaparece) y qué máquinas ya tienen un dron de mantenimiento en camino.
 */
public final class DroneFlights {
    private static final Map<UUID, Long> LAST_SEEN = new ConcurrentHashMap<>();
    private static final Map<BlockPos, UUID> SERVICING = new ConcurrentHashMap<>();

    private DroneFlights() {
    }

    public static void seen(UUID flight, long gameTime) {
        LAST_SEEN.put(flight, gameTime);
    }

    /** Última vez que se vio el vuelo, o -1 si nunca (desde que arrancó el servidor). */
    public static long lastSeen(UUID flight) {
        return LAST_SEEN.getOrDefault(flight, -1L);
    }

    public static void finished(UUID flight) {
        LAST_SEEN.remove(flight);
        SERVICING.values().removeIf(flight::equals);
    }

    /** Reserva una máquina para un dron de mantenimiento. Devuelve false si ya va otro. */
    public static boolean claim(BlockPos machine, UUID flight) {
        return SERVICING.putIfAbsent(machine.immutable(), flight) == null;
    }

    public static boolean claimed(BlockPos machine) {
        return SERVICING.containsKey(machine);
    }
}
