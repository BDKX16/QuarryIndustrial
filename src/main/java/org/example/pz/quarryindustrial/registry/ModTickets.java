package org.example.pz.quarryindustrial.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.example.pz.quarryindustrial.Quarryindustrial;

/**
 * Tickets de carga de chunks propios. No usamos {@code ServerLevel.setChunkForced}: ese ticket es uno solo
 * por chunk y lo comparten {@code /forceload}, otros mods y los tests, así que al soltarlo la quarry le
 * descargaba el chunk a quien lo hubiera forzado antes. Con un tipo propio cada uno saca solo lo suyo.
 */
public final class ModTickets {
    /** Igual que el forzado vanilla (se guarda con el mundo y mantiene el chunk simulando), pero nuestro. */
    public static final TicketType QUARRY = Registry.register(BuiltInRegistries.TICKET_TYPE,
            Quarryindustrial.id("quarry"), new TicketType(TicketType.NO_TIMEOUT,
                    TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION
                            | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    /** Lo que vuela un dron: el chunk donde está, por 3 segundos (el dron lo renueva cada uno). */
    public static final TicketType DRONE = Registry.register(BuiltInRegistries.TICKET_TYPE,
            Quarryindustrial.id("drone"), new TicketType(60L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    private ModTickets() {
    }

    /**
     * Radio 2: nivel de ticket 31, el mismo que {@code /forceload} (el chunk queda con entidades y block
     * entities funcionando).
     */
    private static final int RADIUS = 2;

    public static void force(ServerLevel level, ChunkPos chunk, boolean add) {
        if (add) {
            level.getChunkSource().addTicketWithRadius(QUARRY, chunk, RADIUS);
        } else {
            level.getChunkSource().removeTicketWithRadius(QUARRY, chunk, RADIUS);
        }
    }

    /**
     * Radio 3 para el dron: además de su chunk, los ocho de alrededor quedan con entidades andando, así puede
     * cruzar a otro chunk sin congelarse antes de renovar el ticket.
     */
    public static void keepDroneLoaded(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().addTicketWithRadius(DRONE, chunk, RADIUS + 1);
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
