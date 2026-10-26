package org.example.pz.quarryindustrial.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Memoria del terreno que el jugador ya vio, para el mapa de la centralita. El cliente solo tiene cargados los
 * chunks de su distancia de visión, así que sin esto el mapa mostraba nada más que ese círculo. Se guarda, por
 * mundo y dimensión, el color de mapa y la altura de cada columna cuando un chunk se descarga (o al abrir la
 * centralita), y se persiste en disco para la próxima sesión.
 */
public final class ExploredTerrain {
    private static final int VERSION = 1;

    /** Un chunk recordado: color de mapa (id) y altura de cada una de sus 16×16 columnas, fila por fila (z, x). */
    public record Chunk(byte[] colors, short[] heights) {
        Chunk() {
            this(new byte[256], new short[256]);
        }

        public int color(int x, int z) {
            return colors[(z & 15) * 16 + (x & 15)] & 0xFF;
        }

        public int height(int x, int z) {
            return heights[(z & 15) * 16 + (x & 15)];
        }
    }

    private static final Map<String, Map<Long, Chunk>> MAPS = new HashMap<>();
    private static final Map<String, Boolean> DIRTY = new HashMap<>();

    private ExploredTerrain() {
    }

    /** Se engancha a la descarga de chunks y a la salida del mundo. */
    public static void init() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_UNLOAD.register(
                (level, chunk) -> remember(level, chunk));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.level != null) {
                rememberAround(client.level, client.player == null ? ChunkPos.ZERO : client.player.chunkPosition(),
                        client.options.getEffectiveRenderDistance());
            }
            saveAll();
            MAPS.clear();
            DIRTY.clear();
        });
    }

    /** Anota las columnas de un chunk cargado. */
    public static void remember(ClientLevel level, LevelChunk chunk) {
        Chunk sample = new Chunk();
        ChunkPos pos = chunk.getPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int worldX = pos.getMinBlockX() + x;
                int worldZ = pos.getMinBlockZ() + z;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ) - 1;
                if (top < level.getMinY()) {
                    continue;
                }
                cursor.set(worldX, top, worldZ);
                MapColor mapColor = chunk.getBlockState(cursor).getMapColor(level, cursor);
                if (mapColor == MapColor.NONE) {
                    continue;
                }
                sample.colors[z * 16 + x] = (byte) mapColor.id;
                sample.heights[z * 16 + x] = (short) top;
                any = true;
            }
        }
        if (any) {
            String key = key(level);
            maps(key).put(pos.pack(), sample);
            DIRTY.put(key, true);
        }
    }

    /** Anota todos los chunks cargados alrededor del jugador. */
    public static void rememberAround(ClientLevel level, ChunkPos center, int radius) {
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (level.getChunkSource().hasChunk(center.x() + dx, center.z() + dz)) {
                    remember(level, level.getChunk(center.x() + dx, center.z() + dz));
                }
            }
        }
    }

    public static @Nullable Chunk get(ClientLevel level, int chunkX, int chunkZ) {
        return maps(key(level)).get(ChunkPos.pack(chunkX, chunkZ));
    }

    /** Escribe a disco lo que cambió. */
    public static void saveAll() {
        for (Map.Entry<String, Map<Long, Chunk>> entry : MAPS.entrySet()) {
            if (Boolean.TRUE.equals(DIRTY.get(entry.getKey()))) {
                save(entry.getKey(), entry.getValue());
                DIRTY.put(entry.getKey(), false);
            }
        }
    }

    // ── Disco ──

    private static Map<Long, Chunk> maps(String key) {
        return MAPS.computeIfAbsent(key, ExploredTerrain::load);
    }

    /** Mundo (o servidor) + dimensión: cada uno tiene su archivo. */
    private static String key(ClientLevel level) {
        Minecraft minecraft = Minecraft.getInstance();
        String world;
        if (minecraft.getSingleplayerServer() != null) {
            world = "sp_" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        } else if (minecraft.getCurrentServer() != null) {
            world = "mp_" + minecraft.getCurrentServer().ip;
        } else {
            world = "unknown";
        }
        return (world + "_" + level.dimension().identifier().toString()).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Path file(String key) {
        return FabricLoader.getInstance().getGameDir().resolve("quarryindustrial_map").resolve(key + ".bin");
    }

    private static Map<Long, Chunk> load(String key) {
        Map<Long, Chunk> chunks = new HashMap<>();
        Path path = file(key);
        if (!Files.exists(path)) {
            return chunks;
        }
        try (InputStream raw = Files.newInputStream(path);
             DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
            if (in.readInt() != VERSION) {
                return chunks;
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                long pos = in.readLong();
                Chunk chunk = new Chunk();
                in.readFully(chunk.colors);
                for (int j = 0; j < 256; j++) {
                    chunk.heights[j] = in.readShort();
                }
                chunks.put(pos, chunk);
            }
        } catch (IOException e) {
            Quarryindustrial.LOGGER.warn("No se pudo leer el mapa guardado {}", path, e);
            chunks.clear();
        }
        return chunks;
    }

    private static void save(String key, Map<Long, Chunk> chunks) {
        Path path = file(key);
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream raw = Files.newOutputStream(path);
                 DataOutputStream out = new DataOutputStream(new GZIPOutputStream(raw))) {
                out.writeInt(VERSION);
                out.writeInt(chunks.size());
                for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
                    out.writeLong(entry.getKey());
                    out.write(entry.getValue().colors);
                    for (short height : entry.getValue().heights) {
                        out.writeShort(height);
                    }
                }
            }
        } catch (IOException e) {
            Quarryindustrial.LOGGER.warn("No se pudo guardar el mapa {}", path, e);
        }
    }
}
