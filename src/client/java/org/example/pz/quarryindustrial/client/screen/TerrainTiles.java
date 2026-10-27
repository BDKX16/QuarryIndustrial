package org.example.pz.quarryindustrial.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.client.ExploredTerrain;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * El terreno del mapa de la centralita como imágenes ya pintadas, a la manera de Xaero: el mundo se divide en
 * regiones fijas de {@link #TILE}×{@link #TILE} píxeles, una textura por región y nivel de detalle (cada nivel
 * junta el doble de bloques por píxel que el anterior). Una región se pinta una sola vez, desde lo anotado en
 * {@link ExploredTerrain}, y después mover o acercar el mapa es solo dibujar esas texturas en otra posición.
 * Solo se vuelve a pintar una región cuando cambia algún chunk que cubre.
 */
public final class TerrainTiles {
    public static final int TILE = 256;
    /** Detalle más grueso: 16 bloques por píxel, el máximo alejamiento del mapa. */
    public static final int MAX_LOD = 4;

    private static final int MAX_TILES = 160;
    /** Cuánto de cada cuadro se puede gastar pintando regiones: lo que falta sale en los siguientes. */
    private static final long BUILD_BUDGET_NANOS = 5_000_000L;
    /** Una región ya pintada que cambió no se repinta más seguido que esto (al explorar cambian todo el tiempo). */
    private static final long REFRESH_NANOS = 500_000_000L;

    private static final class Tile {
        final Identifier id;
        final DynamicTexture texture;
        boolean built;
        boolean dirty;
        long builtAt;
        long usedFrame;
        int colored;

        Tile(Identifier id, DynamicTexture texture) {
            this.id = id;
            this.texture = texture;
        }
    }

    private static int serial;

    private final Map<Long, Tile> tiles = new HashMap<>();
    private final int[] scratch = new int[TILE * TILE];
    private long frame;
    private int drawnPixels;

    private static long key(int lod, int tx, int tz) {
        return (long) lod << 56 | (tx & 0xFFFFFFFL) << 28 | (tz & 0xFFFFFFFL);
    }

    /** Marca para repintar las regiones, de cualquier nivel, que cubren ese chunk. */
    void invalidate(int chunkX, int chunkZ) {
        if (tiles.isEmpty()) {
            return;
        }
        for (int lod = 0; lod <= MAX_LOD; lod++) {
            int shift = 8 + lod;
            int tx = (chunkX * 16) >> shift;
            // El relieve de la fila de arriba de una región sale de la región de arriba: la de abajo también
            for (int tz : new int[]{(chunkZ * 16) >> shift, ((chunkZ + 1) * 16) >> shift}) {
                Tile tile = tiles.get(key(lod, tx, tz));
                if (tile != null) {
                    tile.dirty = true;
                }
            }
        }
    }

    /** Dibuja el terreno que cubre el mapa, pintando antes las regiones que falten (dentro del presupuesto). */
    void draw(GuiGraphicsExtractor graphics, ClientLevel level, double centerX, double centerZ, double scale,
              int mapX, int mapY, int mapW, int mapH) {
        frame++;
        int lod = (int) Math.clamp(Math.round(Math.log(scale) / Math.log(2)), 0, MAX_LOD);
        // Un píxel de la textura son 2^lod bloques, o sea 2^lod/scale píxeles de pantalla (un entero)
        int size = (int) Math.round(TILE * (1 << lod) / scale);
        long span = (long) TILE << lod;
        int cx = mapX + mapW / 2;
        int cz = mapY + mapH / 2;
        int minTx = (int) Math.floorDiv((long) Math.floor(centerX - (mapW / 2) * scale), span);
        int maxTx = (int) Math.floorDiv((long) Math.ceil(centerX + (mapW - mapW / 2) * scale), span);
        int minTz = (int) Math.floorDiv((long) Math.floor(centerZ - (mapH / 2) * scale), span);
        int maxTz = (int) Math.floorDiv((long) Math.ceil(centerZ + (mapH - mapH / 2) * scale), span);

        record Visible(int tx, int tz, int x, int y, double distance) {
        }
        List<Visible> visible = new ArrayList<>();
        for (int tz = minTz; tz <= maxTz; tz++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                int x = cx + (int) Math.floor((tx * span - centerX) / scale);
                int y = cz + (int) Math.floor((tz * span - centerZ) / scale);
                double dx = x + size / 2.0 - cx;
                double dy = y + size / 2.0 - cz;
                visible.add(new Visible(tx, tz, x, y, dx * dx + dy * dy));
            }
        }
        // Lo del centro primero: si no alcanza el presupuesto, los bordes son los que esperan
        visible.sort((a, b) -> Double.compare(a.distance, b.distance));

        long start = System.nanoTime();
        long now = start;
        boolean builtOne = false;
        drawnPixels = 0;
        for (Visible region : visible) {
            long key = key(lod, region.tx, region.tz);
            Tile tile = tiles.get(key);
            boolean missing = tile == null || !tile.built;
            boolean stale = tile != null && tile.dirty && now - tile.builtAt > REFRESH_NANOS;
            if ((missing || stale) && (!builtOne || System.nanoTime() - start < BUILD_BUDGET_NANOS)) {
                if (tile == null) {
                    tile = create();
                    tiles.put(key, tile);
                }
                build(level, tile, lod, region.tx, region.tz);
                tile.builtAt = System.nanoTime();
                builtOne = true;
            }
            if (tile != null && tile.built) {
                tile.usedFrame = frame;
                drawnPixels += tile.colored;
                if (tile.colored > 0) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, tile.id, region.x, region.y, 0f, 0f, size, size,
                            TILE, TILE, TILE, TILE);
                }
            }
        }
        evict();
    }

    /** Píxeles con terreno de las regiones que se dibujaron en el último cuadro (para los tests). */
    int drawnPixels() {
        return drawnPixels;
    }

    private static Tile create() {
        Identifier id = Quarryindustrial.id("terrain_tile/" + serial++);
        DynamicTexture texture = new DynamicTexture(() -> "quarryindustrial terrain tile", TILE, TILE, true);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return new Tile(id, texture);
    }

    private void build(ClientLevel level, Tile tile, int lod, int tx, int tz) {
        tile.colored = fill(level, lod, tx, tz, scratch);
        NativeImage image = tile.texture.getPixels();
        if (image != null) {
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    image.setPixel(x, y, scratch[y * TILE + x]);
                }
            }
            tile.texture.upload();
        }
        tile.built = true;
        tile.dirty = false;
    }

    /** Si hay demasiadas regiones guardadas, se sueltan las que hace más que no se dibujan. */
    private void evict() {
        if (tiles.size() <= MAX_TILES) {
            return;
        }
        List<Map.Entry<Long, Tile>> byAge = new ArrayList<>(tiles.entrySet());
        byAge.sort((a, b) -> Long.compare(a.getValue().usedFrame, b.getValue().usedFrame));
        for (Map.Entry<Long, Tile> entry : byAge) {
            if (tiles.size() <= MAX_TILES * 3 / 4 || entry.getValue().usedFrame == frame) {
                break;
            }
            Minecraft.getInstance().getTextureManager().release(entry.getValue().id);
            tiles.remove(entry.getKey());
        }
    }

    void close() {
        Iterator<Tile> it = tiles.values().iterator();
        while (it.hasNext()) {
            Minecraft.getInstance().getTextureManager().release(it.next().id);
            it.remove();
        }
    }

    // ── Pintado de una región ──

    private static final int[] COLORS = new int[(TILE + 1) * TILE];
    private static final int[] HEIGHTS = new int[(TILE + 1) * TILE];

    /**
     * Pinta una región en {@code out} (ARGB, 0 = sin datos) y devuelve cuántos píxeles tienen terreno. Cada píxel
     * promedia hasta 3×3 columnas del terreno anotado y se sombrea por el desnivel respecto del de arriba.
     */
    public static int fill(ClientLevel level, int lod, int tx, int tz, int[] out) {
        int samples = Math.min(lod + 1, 3);
        double blocks = 1 << lod;
        double step = blocks / samples;
        Sampler sampler = new Sampler(level);
        long originX = (long) tx * TILE << lod;
        long originZ = (long) tz * TILE << lod;
        // La fila -1 es la de arriba de la región: solo da la altura con la que comparar la primera
        for (int row = -1; row < TILE; row++) {
            double baseZ = originZ + row * blocks;
            for (int col = 0; col < TILE; col++) {
                double baseX = originX + col * blocks;
                int red = 0;
                int green = 0;
                int blue = 0;
                long height = 0;
                int found = 0;
                for (int sz = 0; sz < samples; sz++) {
                    int z = (int) Math.floor(baseZ + (sz + 0.5) * step);
                    for (int sx = 0; sx < samples; sx++) {
                        int x = (int) Math.floor(baseX + (sx + 0.5) * step);
                        int argb = sampler.color(x, z);
                        if (argb != 0) {
                            red += (argb >> 16) & 0xFF;
                            green += (argb >> 8) & 0xFF;
                            blue += argb & 0xFF;
                            height += sampler.height;
                            found++;
                        }
                    }
                }
                int i = (row + 1) * TILE + col;
                if (found > 0) {
                    HEIGHTS[i] = (int) (height / found);
                    COLORS[i] = 0xFF000000 | (red / found) << 16 | (green / found) << 8 | blue / found;
                } else {
                    COLORS[i] = 0;
                }
            }
        }
        int colored = 0;
        for (int row = 0; row < TILE; row++) {
            for (int col = 0; col < TILE; col++) {
                int i = (row + 1) * TILE + col;
                if (COLORS[i] == 0) {
                    out[row * TILE + col] = 0;
                    continue;
                }
                // Relieve: más claro si sube respecto del píxel de arriba (como los mapas vanilla), más oscuro si baja
                int shade = COLORS[i - TILE] == 0 ? 0 : Integer.signum(HEIGHTS[i] - HEIGHTS[i - TILE]);
                out[row * TILE + col] = tint(COLORS[i], shade);
                colored++;
            }
        }
        return colored;
    }

    /** Colores de mapa ya resueltos a ARGB (0 = todavía sin calcular), normales y apagados. */
    private static final int[] PALETTE = new int[256];
    private static final int[] DIM_PALETTE = new int[256];

    private static int paletteColor(int id, boolean loaded) {
        int[] palette = loaded ? PALETTE : DIM_PALETTE;
        int argb = palette[id];
        if (argb == 0) {
            argb = MapColor.byId(id).calculateARGBColor(MapColor.Brightness.NORMAL) | 0xFF000000;
            palette[id] = loaded ? argb : dim(argb);
            return palette[id];
        }
        return argb;
    }

    /** Lo recordado (no cargado ahora) se ve más oscuro que lo que el cliente tiene delante. */
    private static int dim(int argb) {
        int r = (int) (((argb >> 16) & 0xFF) * 0.7f);
        int g = (int) (((argb >> 8) & 0xFF) * 0.7f);
        int b = (int) ((argb & 0xFF) * 0.7f);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Mezcla el color del mapa con el azul de fondo, para que el terreno quede como "pantalla". */
    private static int tint(int argb, int shade) {
        float light = shade > 0 ? 1.12f : shade < 0 ? 0.78f : 0.95f;
        int r = (int) (((argb >> 16) & 0xFF) * 0.62f * light + 0x07 * 0.38f);
        int g = (int) (((argb >> 8) & 0xFF) * 0.62f * light + 0x13 * 0.38f);
        int b = (int) ((argb & 0xFF) * 0.62f * light + 0x21 * 0.38f);
        return 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
    }

    /**
     * Color y altura del bloque más alto de una columna, desde lo anotado en {@link ExploredTerrain} (arreglos, sin
     * tocar el mundo). Las muestras vecinas caen casi siempre en el mismo chunk: se busca una vez y se reutiliza.
     */
    private static final class Sampler {
        private final ClientLevel level;
        private boolean cached;
        private int chunkX;
        private int chunkZ;
        private ExploredTerrain.@Nullable Chunk chunk;
        private boolean loaded;
        int height;

        Sampler(ClientLevel level) {
            this.level = level;
        }

        int color(int x, int z) {
            int cx = x >> 4;
            int cz = z >> 4;
            if (!cached || cx != chunkX || cz != chunkZ) {
                cached = true;
                chunkX = cx;
                chunkZ = cz;
                chunk = ExploredTerrain.get(level, cx, cz);
                loaded = ExploredTerrain.isLoaded(level, cx, cz);
            }
            if (chunk == null) {
                return 0;
            }
            int id = chunk.color(x, z);
            if (id == 0) {
                return 0;
            }
            height = chunk.height(x, z);
            return paletteColor(id, loaded);
        }
    }
}
