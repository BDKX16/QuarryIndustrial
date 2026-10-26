package org.example.pz.quarryindustrial.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.block.drone.DronePadStatus;
import org.example.pz.quarryindustrial.client.ExploredTerrain;
import org.example.pz.quarryindustrial.drone.DroneKind;
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
import org.example.pz.quarryindustrial.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Pantalla de la centralita de drones, con estilo de consola de radar (el mismo azul de plano del manual).
 * Tres pestañas:
 * <ul>
 *   <li>Mapa: el terreno que tiene cargado el cliente, visto de arriba, con los pads, sus vínculos, los drones
 *   en vuelo (y el barrido que le falta al explorador) y los géiseres encontrados. Arrastrar mueve, la rueda
 *   acerca. Clic en un pad lo elige; con "Vincular", el siguiente clic en otro pad es el destino.</li>
 *   <li>Drones: todos los pads del alcance, con su estado, a dónde van y botones para vincular o explorar.</li>
 *   <li>Pozos: los géiseres que vieron los exploradores.</li>
 * </ul>
 * Los datos los manda el servidor ({@link Snapshot}); la pantalla los pide una vez por segundo.
 * Los botones se dibujan a mano: cada cuadro se arma la lista de zonas clicables ({@link Hit}).
 */
public class DroneConsoleScreen extends Screen {
    private enum Tab {
        MAP,
        DRONES,
        WELLS
    }

    private static final int MAX_WIDTH = 440;
    private static final int MAX_HEIGHT = 270;
    private static final int HEADER = 22;
    private static final int FOOTER = 14;
    private static final int ROW = 24;
    private static final int BUTTON_H = 12;
    /** Bloques por pixel de cada nivel de zoom. */
    private static final float[] ZOOMS = {0.25f, 0.5f, 1, 2, 4, 8, 16};
    /** El terreno se calcula en celdas de 2×2 px. */
    private static final int CELL = 2;
    private static final int REFRESH_TICKS = 20;

    private static final int FRAME = 0xFF06111F;
    private static final int PANEL = 0xFF0B1E33;
    private static final int PANEL_ALT = 0xFF0F2540;
    private static final int PANEL_HOVER = 0xFF17375C;
    private static final int MAP_BG = 0xFF071321;
    private static final int GRID = 0x2A6FB8FF;
    private static final int GRID_MAJOR = 0x506FB8FF;
    private static final int INK = 0xFFE6F0FF;
    private static final int DIM = 0xFF8FB4DA;
    private static final int ACCENT = 0xFFFFD24A;
    private static final int GOOD = 0xFF4ACF5A;
    private static final int WARN = 0xFFFFB02A;
    private static final int BAD = 0xFFFF5A4A;
    private static final int FLYING = 0xFF6FD0FF;
    private static final int EMPTY_PAD = 0xFF6A7380;
    private static final int OIL = 0xFF0E0E10;
    private static final int OIL_RIM = 0xFFB050FF;

    /** Gota de petróleo de 5×7 para los géiseres ('#' petróleo, 'w' brillo). */
    private static final String[] DROP = {
            "..#..",
            ".###.",
            ".###.",
            "##w##",
            "#w###",
            "#####",
            ".###.",
    };

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final BlockPos console;
    private @Nullable Snapshot snapshot;
    private Tab tab = Tab.MAP;
    private int ticks;
    private final List<Hit> hits = new ArrayList<>();

    // Mapa: centro en coordenadas del mundo, zoom y arrastre
    private double centerX;
    private double centerZ;
    private int zoom = 2;
    private boolean fitted;
    private boolean dragging;
    private boolean dragMoved;
    private int mapX;
    private int mapY;
    private int mapW;
    private int mapH;
    /** Colores del terreno por celda (0 = sin datos: chunk no cargado en el cliente). */
    private int[] terrain = new int[0];
    private int terrainCols;
    private int terrainRows;
    private double terrainX = Double.NaN;
    private double terrainZ = Double.NaN;
    private int terrainZoom = -1;
    private boolean terrainStale = true;

    private @Nullable BlockPos selected;
    /** Pad de origen mientras se elige el destino de un vínculo. */
    private @Nullable BlockPos linkSource;
    private int scroll;
    private @Nullable Component notice;
    private boolean noticeOk;
    private int noticeTicks;

    public DroneConsoleScreen(BlockPos console) {
        super(Component.translatable("block.quarryindustrial.drone_console"));
        this.console = console;
        this.centerX = console.getX() + 0.5;
        this.centerZ = console.getZ() + 0.5;
    }

    @Override
    protected void init() {
        layout();
        request();
        // Lo que el cliente tiene cargado ahora también se recuerda, para cuando se aleje
        if (minecraft.level != null && minecraft.player != null) {
            ExploredTerrain.rememberAround(minecraft.level, minecraft.player.chunkPosition(),
                    minecraft.options.getEffectiveRenderDistance());
        }
        terrainStale = true;
    }

    @Override
    public void removed() {
        super.removed();
        ExploredTerrain.saveAll();
    }

    private void request() {
        ClientPlayNetworking.send(new RequestSnapshot(console));
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks % REFRESH_TICKS == 0) {
            request();
        }
        if (ticks % 40 == 0) {
            terrainStale = true;
        }
        if (noticeTicks > 0) {
            noticeTicks--;
        }
        if (minecraft.player != null
                && minecraft.player.distanceToSqr(Vec3.atCenterOf(console)) > 8 * 8) {
            onClose();
        }
    }

    /** Llega del servidor (una vez por segundo y después de cada acción). */
    public void accept(Snapshot snapshot) {
        this.snapshot = snapshot;
        if (!fitted) {
            fitted = true;
            fitAll();
        }
        if (selected != null && pad(selected) == null) {
            selected = null;
        }
        if (linkSource != null && pad(linkSource) == null) {
            linkSource = null;
        }
    }

    public void accept(Notice notice) {
        this.notice = notice.message();
        this.noticeOk = notice.ok();
        this.noticeTicks = 100;
    }

    // ── Geometría ──

    private int panelW() {
        return Math.min(MAX_WIDTH, width - 16);
    }

    private int panelH() {
        return Math.min(MAX_HEIGHT, height - 16);
    }

    private int left() {
        return (width - panelW()) / 2;
    }

    private int top() {
        return (height - panelH()) / 2;
    }

    private int bodyX() {
        return left() + 6;
    }

    private int bodyY() {
        return top() + HEADER + 2;
    }

    private int bodyW() {
        return panelW() - 12;
    }

    private int bodyH() {
        return panelH() - HEADER - FOOTER - 4;
    }

    private void layout() {
        mapX = bodyX();
        mapY = bodyY();
        mapW = bodyW();
        mapH = bodyH();
    }

    private double scale() {
        return ZOOMS[zoom];
    }

    private int screenX(double worldX) {
        return mapX + mapW / 2 + (int) Math.floor((worldX - centerX) / scale());
    }

    private int screenZ(double worldZ) {
        return mapY + mapH / 2 + (int) Math.floor((worldZ - centerZ) / scale());
    }

    private double worldX(double screenX) {
        return centerX + (screenX - mapX - mapW / 2.0) * scale();
    }

    private double worldZ(double screenY) {
        return centerZ + (screenY - mapY - mapH / 2.0) * scale();
    }

    private boolean inMap(double mx, double my) {
        return mx >= mapX && mx < mapX + mapW && my >= mapY && my < mapY + mapH;
    }

    /** Encuadra la centralita, los pads y los géiseres con el zoom más cercano que los muestre a todos. */
    private void fitAll() {
        if (snapshot == null) {
            return;
        }
        int minX = console.getX();
        int maxX = minX;
        int minZ = console.getZ();
        int maxZ = minZ;
        List<BlockPos> points = new ArrayList<>();
        snapshot.pads().forEach(pad -> points.add(pad.pos()));
        snapshot.sites().forEach(site -> points.add(site.pos()));
        for (BlockPos pos : points) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        centerX = (minX + maxX) / 2.0 + 0.5;
        centerZ = (minZ + maxZ) / 2.0 + 0.5;
        zoom = ZOOMS.length - 1;
        for (int i = 1; i < ZOOMS.length; i++) {
            if ((maxX - minX) / ZOOMS[i] <= mapW - 48 && (maxZ - minZ) / ZOOMS[i] <= mapH - 48) {
                zoom = i;
                break;
            }
        }
    }

    private void centerOn(BlockPos pos) {
        centerX = pos.getX() + 0.5;
        centerZ = pos.getZ() + 0.5;
    }

    // ── Datos ──

    private List<Pad> pads() {
        return snapshot == null ? List.of() : snapshot.pads();
    }

    private List<Site> sites() {
        return snapshot == null ? List.of() : snapshot.sites();
    }

    private List<Flight> flights() {
        return snapshot == null ? List.of() : snapshot.flights();
    }

    private @Nullable Pad pad(BlockPos pos) {
        for (Pad pad : pads()) {
            if (pad.pos().equals(pos)) {
                return pad;
            }
        }
        return null;
    }

    private static @Nullable DroneKind kind(int ordinal) {
        return ordinal < 0 || ordinal >= DroneKind.values().length ? null : DroneKind.values()[ordinal];
    }

    private static DronePadStatus status(Pad pad) {
        DronePadStatus[] values = DronePadStatus.values();
        return pad.status() >= 0 && pad.status() < values.length ? values[pad.status()] : DronePadStatus.NO_DRONE;
    }

    private static int kindColor(int ordinal) {
        DroneKind kind = kind(ordinal);
        return kind == null ? EMPTY_PAD : kind.color();
    }

    private static int statusColor(DronePadStatus status) {
        return switch (status) {
            case LOADING, STANDBY, READY -> GOOD;
            case IN_FLIGHT -> FLYING;
            case NO_LINK, DESTINATION_FULL -> WARN;
            case BAD_LINK, NO_POWER, LOST -> BAD;
            case NO_DRONE -> DIM;
        };
    }

    private static Component padName(Pad pad) {
        DroneKind kind = kind(pad.kind());
        return kind == null ? Component.translatable("gui.quarryindustrial.console.empty_pad") : kind.label();
    }

    private static ItemStack padIcon(Pad pad) {
        DroneKind kind = kind(pad.kind());
        return kind == null ? new ItemStack(ModBlocks.DRONE_PAD) : new ItemStack(ModItems.drone(kind));
    }

    /** Si el pad se puede vincular: sin dron (para dejarlo listo) o con uno de transporte. */
    private static boolean linkable(Pad pad) {
        DroneKind kind = kind(pad.kind());
        return kind == null || kind.needsLink();
    }

    private int distance(BlockPos pos) {
        double dx = pos.getX() - console.getX();
        double dz = pos.getZ() - console.getZ();
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    private static String coords(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    // ── Acciones ──

    private void select(Pad pad) {
        if (linkSource != null) {
            if (!linkSource.equals(pad.pos())) {
                ClientPlayNetworking.send(new SetLink(console, linkSource, Optional.of(pad.pos())));
                selected = linkSource;
                linkSource = null;
            }
            return;
        }
        selected = pad.pos();
    }

    private void startLink(Pad pad) {
        linkSource = pad.pos();
        selected = pad.pos();
    }

    private void unlink(Pad pad) {
        ClientPlayNetworking.send(new SetLink(console, pad.pos(), Optional.empty()));
    }

    private void survey(Pad pad) {
        ClientPlayNetworking.send(new LaunchSurvey(console, pad.pos()));
    }

    private void forget(Site site) {
        ClientPlayNetworking.send(new ForgetSite(console, site.pos()));
    }

    private void showOnMap(BlockPos pos) {
        tab = Tab.MAP;
        centerOn(pos);
        zoom = Math.min(zoom, 2);
    }

    // ── Dibujo ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        layout();
        hits.clear();
        int x = left();
        int y = top();
        graphics.fill(x - 3, y - 3, x + panelW() + 3, y + panelH() + 3, FRAME);
        graphics.fill(x, y, x + panelW(), y + panelH(), PANEL);
        graphics.outline(x + 2, y + 2, panelW() - 4, panelH() - 4, 0xFF1F4A75);
        drawHeader(graphics, x, y, mouseX, mouseY);
        if (snapshot == null) {
            graphics.centeredText(font, Component.translatable("gui.quarryindustrial.console.connecting"),
                    x + panelW() / 2, y + panelH() / 2, DIM);
        } else {
            switch (tab) {
                case MAP -> drawMap(graphics, mouseX, mouseY, partialTick);
                case DRONES -> drawDrones(graphics, mouseX, mouseY);
                case WELLS -> drawWells(graphics, mouseX, mouseY);
            }
        }
        drawFooter(graphics, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        graphics.item(new ItemStack(ModBlocks.DRONE_CONSOLE), x + 6, y + 3);
        graphics.text(font, title, x + 26, y + 8, ACCENT, false);
        // Pestañas a la derecha
        Component[] labels = {
                Component.translatable("gui.quarryindustrial.console.tab.map"),
                Component.translatable("gui.quarryindustrial.console.tab.drones", pads().size()),
                Component.translatable("gui.quarryindustrial.console.tab.wells", sites().size()),
        };
        int right = x + panelW() - 6;
        for (int i = labels.length - 1; i >= 0; i--) {
            Tab target = Tab.values()[i];
            int w = font.width(labels[i]) + 14;
            int tx = right - w;
            boolean active = tab == target;
            boolean hovered = mouseX >= tx && mouseX < tx + w && mouseY >= y + 4 && mouseY < y + HEADER;
            graphics.fill(tx, y + 4, tx + w, y + HEADER, active ? PANEL_ALT : hovered ? PANEL_HOVER : FRAME);
            if (active) {
                graphics.fill(tx, y + 4, tx + w, y + 5, ACCENT);
            }
            graphics.text(font, labels[i], tx + 7, y + 9, active ? ACCENT : INK, false);
            hits.add(new Hit(tx, y + 4, w, HEADER - 4, () -> {
                tab = target;
                scroll = 0;
            }));
            right = tx - 2;
        }
        graphics.fill(x + 6, y + HEADER, x + panelW() - 6, y + HEADER + 1, 0xFF1F4A75);
    }

    private void drawFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int y = top() + panelH() - FOOTER + 2;
        int x = bodyX();
        Component text;
        int color;
        if (noticeTicks > 0 && notice != null) {
            text = notice;
            color = noticeOk ? GOOD : BAD;
        } else if (linkSource != null) {
            text = Component.translatable("gui.quarryindustrial.console.linking");
            color = ACCENT;
        } else {
            text = Component.translatable(tab == Tab.MAP ? "gui.quarryindustrial.console.map_hint"
                    : "gui.quarryindustrial.console.list_hint");
            color = DIM;
        }
        String right = "";
        if (tab == Tab.MAP && inMap(mouseX, mouseY)) {
            right = Component.translatable("gui.quarryindustrial.console.cursor", (int) Math.floor(worldX(mouseX)),
                    (int) Math.floor(worldZ(mouseY)), scaleLabel()).getString();
        }
        int rightW = font.width(right);
        graphics.text(font, font.plainSubstrByWidth(text.getString(), bodyW() - rightW - 8), x, y, color, false);
        graphics.text(font, right, x + bodyW() - rightW, y, DIM, false);
    }

    private String scaleLabel() {
        float s = ZOOMS[zoom];
        return s < 1 ? (int) (1 / s) + ":1" : "1:" + (int) s;
    }

    // ── Pestaña mapa ──

    private void drawMap(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(mapX - 1, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, 0xFF1F4A75);
        graphics.fill(mapX, mapY, mapX + mapW, mapY + mapH, MAP_BG);
        graphics.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
        drawTerrain(graphics);
        drawGrid(graphics);
        drawSweep(graphics, partialTick);
        if (linkSource != null) {
            // Hasta dónde llega un vínculo desde el origen
            circle(graphics, screenX(linkSource.getX() + 0.5), screenZ(linkSource.getZ() + 0.5),
                    snapshot.maxLinkDistance() / scale(), 0x80FFD24A);
        }
        for (Pad pad : pads()) {
            pad.link().ifPresent(target -> drawLink(graphics, pad, target));
        }
        for (Flight flight : flights()) {
            drawFlightPath(graphics, flight);
        }
        for (Site site : sites()) {
            drawDrop(graphics, screenX(site.pos().getX() + 0.5), screenZ(site.pos().getZ() + 0.5));
        }
        drawConsoleMarker(graphics);
        for (Pad pad : pads()) {
            drawPad(graphics, pad);
        }
        for (Flight flight : flights()) {
            drawDrone(graphics, flight);
        }
        graphics.disableScissor();

        drawMapControls(graphics, mouseX, mouseY);
        drawLegend(graphics);
        if (linkSource != null) {
            Component banner = Component.translatable("gui.quarryindustrial.console.linking");
            int w = font.width(banner) + 12;
            int bx = mapX + (mapW - w) / 2;
            graphics.fill(bx, mapY + 4, bx + w, mapY + 17, 0xE0201A08);
            graphics.outline(bx, mapY + 4, w, 13, ACCENT);
            graphics.text(font, banner, bx + 6, mapY + 7, ACCENT, false);
        }
        Pad current = selected == null ? null : pad(selected);
        if (current != null) {
            drawPadCard(graphics, current, mouseX, mouseY);
        }
        if (inMap(mouseX, mouseY) && !dragging) {
            mapTooltip(graphics, mouseX, mouseY);
        }
    }

    /** El terreno que tiene el cliente, con sombreado de relieve y teñido de azul de radar. */
    private void drawTerrain(GuiGraphicsExtractor graphics) {
        int cols = (mapW + CELL - 1) / CELL;
        int rows = (mapH + CELL - 1) / CELL;
        if (terrainStale || cols != terrainCols || rows != terrainRows || zoom != terrainZoom
                || centerX != terrainX || centerZ != terrainZ) {
            buildTerrain(cols, rows);
        }
        for (int row = 0; row < rows; row++) {
            int py = mapY + row * CELL;
            int col = 0;
            while (col < cols) {
                int color = terrain[row * cols + col];
                int end = col + 1;
                while (end < cols && terrain[row * cols + end] == color) {
                    end++;
                }
                if (color != 0) {
                    graphics.fill(mapX + col * CELL, py, mapX + end * CELL, py + CELL, color);
                }
                col = end;
            }
        }
    }

    private void buildTerrain(int cols, int rows) {
        terrainCols = cols;
        terrainRows = rows;
        terrainZoom = zoom;
        terrainX = centerX;
        terrainZ = centerZ;
        terrainStale = false;
        terrain = new int[cols * rows];
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        int[] heights = new int[cols * rows];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int row = 0; row < rows; row++) {
            int z = (int) Math.floor(worldZ(mapY + row * CELL + CELL / 2.0));
            for (int col = 0; col < cols; col++) {
                int x = (int) Math.floor(worldX(mapX + col * CELL + CELL / 2.0));
                int i = row * cols + col;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    // Fuera de lo cargado: lo que se vio antes, un poco más apagado
                    ExploredTerrain.Chunk seen = ExploredTerrain.get(level, x >> 4, z >> 4);
                    if (seen != null && seen.color(x, z) != 0) {
                        heights[i] = seen.height(x, z);
                        int argb = MapColor.byId(seen.color(x, z)).calculateARGBColor(MapColor.Brightness.NORMAL);
                        terrain[i] = dim(argb | 0xFF000000);
                    }
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                if (top < level.getMinY()) {
                    continue;
                }
                pos.set(x, top, z);
                BlockState state = level.getBlockState(pos);
                MapColor mapColor = state.getMapColor(level, pos);
                if (mapColor == MapColor.NONE) {
                    continue;
                }
                heights[i] = top;
                terrain[i] = mapColor.calculateARGBColor(MapColor.Brightness.NORMAL) | 0xFF000000;
            }
        }
        // Relieve: más claro si sube respecto de la celda de arriba (como los mapas vanilla), más oscuro si baja
        for (int row = rows - 1; row >= 0; row--) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if (terrain[i] == 0) {
                    continue;
                }
                int shade = 0;
                if (row > 0 && terrain[i - cols] != 0) {
                    shade = Integer.signum(heights[i] - heights[i - cols]);
                }
                terrain[i] = tint(terrain[i], shade);
            }
        }
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

    /** Cuadrícula alineada a chunks: la separación crece con el zoom para no quedar menos de 32 px. */
    private void drawGrid(GuiGraphicsExtractor graphics) {
        int step = 16;
        while (step / scale() < 32) {
            step *= 2;
        }
        int major = step * 4;
        double startX = Math.floor(worldX(mapX) / step) * step;
        for (double wx = startX; screenX(wx) < mapX + mapW; wx += step) {
            int sx = screenX(wx);
            graphics.fill(sx, mapY, sx + 1, mapY + mapH, Math.floorMod((long) wx, major) == 0 ? GRID_MAJOR : GRID);
        }
        double startZ = Math.floor(worldZ(mapY) / step) * step;
        for (double wz = startZ; screenZ(wz) < mapY + mapH; wz += step) {
            int sz = screenZ(wz);
            graphics.fill(mapX, sz, mapX + mapW, sz + 1, Math.floorMod((long) wz, major) == 0 ? GRID_MAJOR : GRID);
        }
    }

    /** El barrido del radar, girando desde la centralita, con una estela que se apaga. */
    private void drawSweep(GuiGraphicsExtractor graphics, float partialTick) {
        int cx = screenX(console.getX() + 0.5);
        int cz = screenZ(console.getZ() + 0.5);
        double angle = (ticks + partialTick) * 0.04;
        int length = mapW + mapH;
        for (int trail = 5; trail >= 0; trail--) {
            double a = angle - trail * 0.035;
            int alpha = 0x50 - trail * 0x0C;
            line(graphics, cx, cz, cx + (int) (Math.cos(a) * length), cz + (int) (Math.sin(a) * length),
                    alpha << 24 | 0x4ACF8A, trail == 0 ? 0 : 2);
        }
    }

    /** Vínculo de un pad: línea a trazos del color del dron, con una flecha a mitad de camino. */
    private void drawLink(GuiGraphicsExtractor graphics, Pad pad, BlockPos target) {
        int x0 = screenX(pad.pos().getX() + 0.5);
        int z0 = screenZ(pad.pos().getZ() + 0.5);
        int x1 = screenX(target.getX() + 0.5);
        int z1 = screenZ(target.getZ() + 0.5);
        boolean highlighted = pad.pos().equals(selected);
        int color = (highlighted ? 0xF0000000 : 0xA0000000) | (kindColor(pad.kind()) & 0xFFFFFF);
        line(graphics, x0, z0, x1, z1, color, 4);
        if (highlighted) {
            line(graphics, x0 + 1, z0, x1 + 1, z1, color, 4);
        }
        double dx = x1 - x0;
        double dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 16) {
            return;
        }
        double ux = dx / length;
        double uz = dz / length;
        int mx = (int) (x0 + dx * 0.55);
        int mz = (int) (z0 + dz * 0.55);
        int tipX = (int) (mx + ux * 3);
        int tipZ = (int) (mz + uz * 3);
        line(graphics, tipX, tipZ, (int) (mx - ux * 3 + uz * 3), (int) (mz - uz * 3 - ux * 3), color | 0xFF000000, 0);
        line(graphics, tipX, tipZ, (int) (mx - ux * 3 - uz * 3), (int) (mz - uz * 3 + ux * 3), color | 0xFF000000, 0);
    }

    /** Hacia dónde va un dron y, si es un explorador barriendo, el resto de su espiral. */
    private void drawFlightPath(GuiGraphicsExtractor graphics, Flight flight) {
        int color = kindColor(flight.kind()) & 0xFFFFFF;
        int x = screenX(flight.x());
        int z = screenZ(flight.z());
        int hx = screenX(flight.heading().getX() + 0.5);
        int hz = screenZ(flight.heading().getZ() + 0.5);
        line(graphics, x, z, hx, hz, 0xD0000000 | color, 2);
        int px = hx;
        int pz = hz;
        for (BlockPos point : flight.route()) {
            int nx = screenX(point.getX() + 0.5);
            int nz = screenZ(point.getZ() + 0.5);
            if (nx == px && nz == pz) {
                continue;
            }
            line(graphics, px, pz, nx, nz, 0x60000000 | color, 2);
            px = nx;
            pz = nz;
        }
    }

    private void drawDrone(GuiGraphicsExtractor graphics, Flight flight) {
        DroneKind kind = kind(flight.kind());
        if (kind == null) {
            return;
        }
        int x = screenX(flight.x());
        int z = screenZ(flight.z());
        // Halo que late, para encontrarlos rápido
        int pulse = (int) (Math.sin(ticks * 0.3) * 1.5 + 6.5);
        graphics.outline(x - pulse, z - pulse, pulse * 2 + 1, pulse * 2 + 1, 0x70000000 | (kind.color() & 0xFFFFFF));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x - 6, z - 6);
        graphics.pose().scale(0.75f, 0.75f);
        graphics.item(new ItemStack(ModItems.drone(kind)), 0, 0);
        graphics.pose().popMatrix();
    }

    /** Pad: placa gris con el color del dron (hueco si está volando), luz de estado y anillo si está elegido. */
    private void drawPad(GuiGraphicsExtractor graphics, Pad pad) {
        int x = screenX(pad.pos().getX() + 0.5);
        int z = screenZ(pad.pos().getZ() + 0.5);
        DronePadStatus status = status(pad);
        int color = kindColor(pad.kind());
        if (pad.pos().equals(selected) || pad.pos().equals(linkSource)) {
            int ring = (ticks / 6) % 2 == 0 ? ACCENT : 0xFFFFF0A0;
            graphics.outline(x - 7, z - 7, 15, 15, ring);
        }
        graphics.fill(x - 5, z - 5, x + 6, z + 6, 0xFF000000);
        graphics.fill(x - 4, z - 4, x + 5, z + 5, 0xFF2B313A);
        graphics.fill(x - 4, z - 4, x + 5, z - 3, 0xFF4A525C);
        if (kind(pad.kind()) != null) {
            if (status == DronePadStatus.IN_FLIGHT) {
                graphics.outline(x - 2, z - 2, 5, 5, color);
            } else {
                graphics.fill(x - 2, z - 2, x + 3, z + 3, color);
            }
        }
        graphics.fill(x + 3, z + 3, x + 6, z + 6, statusColor(status));
        if (!pad.loaded()) {
            graphics.fill(x - 5, z - 5, x + 6, z + 6, 0x60000000);
        }
    }

    private void drawConsoleMarker(GuiGraphicsExtractor graphics) {
        int x = screenX(console.getX() + 0.5);
        int z = screenZ(console.getZ() + 0.5);
        graphics.fill(x - 3, z - 3, x + 4, z + 4, 0xFF000000);
        graphics.fill(x - 2, z - 2, x + 3, z + 3, FLYING);
        graphics.fill(x - 1, z - 1, x + 2, z + 2, 0xFF0B1E33);
        graphics.fill(x, z - 6, x + 1, z - 3, FLYING);
    }

    private void drawDrop(GuiGraphicsExtractor graphics, int cx, int cz) {
        int x0 = cx - 2;
        int z0 = cz - 4;
        // Contorno violeta (el color del explorador) para que se vea sobre el terreno oscuro
        for (int row = 0; row < DROP.length; row++) {
            for (int col = 0; col < DROP[row].length(); col++) {
                if (DROP[row].charAt(col) != '.') {
                    graphics.fill(x0 + col - 1, z0 + row - 1, x0 + col + 2, z0 + row + 2, OIL_RIM);
                }
            }
        }
        for (int row = 0; row < DROP.length; row++) {
            for (int col = 0; col < DROP[row].length(); col++) {
                char c = DROP[row].charAt(col);
                if (c != '.') {
                    graphics.fill(x0 + col, z0 + row, x0 + col + 1, z0 + row + 1, c == 'w' ? 0xFF8A8A9A : OIL);
                }
            }
        }
    }

    /** Arriba a la derecha: acercar, alejar, volver a la centralita y encuadrar todo. */
    private void drawMapControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = mapX + mapW - 16;
        int y = mapY + 4;
        String[] labels = {"+", "-", "⌂", "⤢"};
        Runnable[] actions = {
                () -> zoom = Math.max(0, zoom - 1),
                () -> zoom = Math.min(ZOOMS.length - 1, zoom + 1),
                () -> centerOn(console),
                this::fitAll,
        };
        for (int i = 0; i < labels.length; i++) {
            int by = y + i * 14;
            boolean hovered = mouseX >= x && mouseX < x + 12 && mouseY >= by && mouseY < by + 12;
            graphics.fill(x, by, x + 12, by + 12, hovered ? PANEL_HOVER : 0xE00B1E33);
            graphics.outline(x, by, 12, 12, DIM);
            graphics.centeredText(font, labels[i], x + 6, by + 2, INK);
            hits.add(new Hit(x, by, 12, 12, actions[i]));
        }
    }

    private void drawLegend(GuiGraphicsExtractor graphics) {
        int x = mapX + 4;
        int y = mapY + mapH - 14;
        Component pad = Component.translatable("gui.quarryindustrial.console.legend.pad");
        Component well = Component.translatable("gui.quarryindustrial.console.legend.well");
        Component route = Component.translatable("gui.quarryindustrial.console.legend.route");
        int w = 14 + font.width(pad) + 18 + font.width(well) + 22 + font.width(route) + 4;
        graphics.fill(x, y, x + w, y + 11, 0xC0071321);
        graphics.fill(x + 3, y + 2, x + 10, y + 9, 0xFF2B313A);
        graphics.fill(x + 5, y + 4, x + 8, y + 7, ACCENT);
        int tx = x + 13;
        graphics.text(font, pad, tx, y + 2, DIM, false);
        tx += font.width(pad) + 8;
        drawDrop(graphics, tx + 3, y + 5);
        tx += 10;
        graphics.text(font, well, tx, y + 2, DIM, false);
        tx += font.width(well) + 6;
        line(graphics, tx, y + 5, tx + 12, y + 5, 0xFFE6AE12, 2);
        tx += 16;
        graphics.text(font, route, tx, y + 2, DIM, false);
    }

    /** Ficha del pad elegido, arriba a la izquierda del mapa, con sus botones. */
    private void drawPadCard(GuiGraphicsExtractor graphics, Pad pad, int mouseX, int mouseY) {
        int x = mapX + 4;
        int y = mapY + 4;
        int w = 150;
        int h = 66;
        DronePadStatus status = status(pad);
        // La ficha tapa el mapa: los clics y tooltips no pasan a lo que hay debajo (los botones van después y ganan)
        hits.add(new Hit(x, y, w, h, () -> {
        }));
        graphics.fill(x, y, x + w, y + h, 0xE8081626);
        graphics.outline(x, y, w, h, kindColor(pad.kind()));
        graphics.item(padIcon(pad), x + 4, y + 4);
        graphics.text(font, font.plainSubstrByWidth(padName(pad).getString(), w - 26), x + 23, y + 4, INK, false);
        graphics.text(font, coords(pad.pos()), x + 23, y + 13, DIM, false);
        graphics.text(font, font.plainSubstrByWidth(status.label().getString(), w - 8), x + 4, y + 24,
                statusColor(status), false);
        Component link = pad.link().map(target -> (Component) Component.translatable(
                        "gui.quarryindustrial.console.link", coords(target), (int) Math.sqrt(pad.pos().distSqr(target))))
                .orElse(Component.translatable("gui.quarryindustrial.console.no_link"));
        graphics.text(font, font.plainSubstrByWidth(link.getString(), w - 8), x + 4, y + 34, DIM, false);
        if (pad.energy() >= 0) {
            graphics.fill(x + 4, y + 45, x + w - 4, y + 48, 0xFF1A1A1A);
            graphics.fillGradient(x + 4, y + 45, x + 4 + (w - 8) * pad.energy() / 1000, y + 48, 0xFFFFD24A, 0xFFFF3D5A);
        } else {
            graphics.text(font, Component.translatable("gui.quarryindustrial.console.no_signal"), x + 4, y + 43,
                    0xFF5A7090, false);
        }
        int right = x + w - 4;
        int by = y + h - BUTTON_H - 3;
        right = button(graphics, right, by, Component.literal("✕"), DIM, mouseX, mouseY, () -> selected = null);
        if (linkSource != null && linkSource.equals(pad.pos())) {
            button(graphics, right, by, Component.translatable("gui.quarryindustrial.console.button.cancel"), ACCENT,
                    mouseX, mouseY, () -> linkSource = null);
            return;
        }
        if (linkable(pad)) {
            right = button(graphics, right, by, Component.translatable("gui.quarryindustrial.console.button.link"),
                    ACCENT, mouseX, mouseY, () -> startLink(pad));
        }
        if (pad.link().isPresent()) {
            right = button(graphics, right, by, Component.translatable("gui.quarryindustrial.console.button.unlink"),
                    BAD, mouseX, mouseY, () -> unlink(pad));
        }
        if (kind(pad.kind()) == DroneKind.SURVEY && status != DronePadStatus.IN_FLIGHT) {
            button(graphics, right, by, Component.translatable("gui.quarryindustrial.console.button.survey"),
                    OIL_RIM, mouseX, mouseY, () -> survey(pad));
        }
    }

    // ── Lo que hay bajo el mouse en el mapa ──

    private @Nullable Pad padAt(double mx, double my) {
        Pad best = null;
        double bestDistance = 7 * 7;
        for (Pad pad : pads()) {
            double dx = screenX(pad.pos().getX() + 0.5) - mx;
            double dz = screenZ(pad.pos().getZ() + 0.5) - my;
            if (dx * dx + dz * dz <= bestDistance) {
                bestDistance = dx * dx + dz * dz;
                best = pad;
            }
        }
        return best;
    }

    private @Nullable Site siteAt(double mx, double my) {
        for (Site site : sites()) {
            double dx = screenX(site.pos().getX() + 0.5) - mx;
            double dz = screenZ(site.pos().getZ() + 0.5) - my;
            if (dx * dx + dz * dz <= 5 * 5) {
                return site;
            }
        }
        return null;
    }

    private @Nullable Flight flightAt(double mx, double my) {
        for (Flight flight : flights()) {
            double dx = screenX(flight.x()) - mx;
            double dz = screenZ(flight.z()) - my;
            if (dx * dx + dz * dz <= 7 * 7) {
                return flight;
            }
        }
        return null;
    }

    private boolean overHit(double mx, double my) {
        for (Hit hit : hits) {
            if (hit.contains(mx, my)) {
                return true;
            }
        }
        return false;
    }

    private void mapTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (overHit(mouseX, mouseY)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        Flight flight = flightAt(mouseX, mouseY);
        Pad pad = flight == null ? padAt(mouseX, mouseY) : null;
        Site site = flight == null && pad == null ? siteAt(mouseX, mouseY) : null;
        if (flight != null) {
            DroneKind kind = kind(flight.kind());
            lines.add(kind == null ? Component.literal("?") : kind.label().copy().withColor(kind.color()));
            BlockPos heading = flight.heading();
            lines.add(Component.translatable(flight.returning() ? "gui.quarryindustrial.console.flight_home"
                    : "gui.quarryindustrial.console.flight_to", coords(heading)).withColor(DIM));
            if (!flight.route().isEmpty()) {
                lines.add(Component.translatable("gui.quarryindustrial.console.surveying", flight.route().size())
                        .withColor(OIL_RIM));
            }
        } else if (pad != null) {
            DronePadStatus status = status(pad);
            lines.add(padName(pad).copy().withColor(kindColor(pad.kind())));
            lines.add(Component.literal(coords(pad.pos())).withColor(DIM));
            lines.add(status.label().copy().withColor(statusColor(status)));
            pad.link().ifPresent(target -> lines.add(Component.translatable("gui.quarryindustrial.console.link",
                    coords(target), (int) Math.sqrt(pad.pos().distSqr(target))).withColor(DIM)));
            if (pad.energy() >= 0) {
                lines.add(Component.translatable("gui.quarryindustrial.console.energy", pad.energy() / 10).withColor(DIM));
            } else {
                lines.add(Component.translatable("gui.quarryindustrial.console.no_signal").withColor(0xFF5A7090));
            }
            if (linkSource != null && !linkSource.equals(pad.pos())) {
                lines.add(Component.translatable("gui.quarryindustrial.console.link_here").withColor(ACCENT));
            }
        } else if (site != null) {
            lines.add(Component.translatable("gui.quarryindustrial.console.site").withColor(OIL_RIM));
            lines.add(Component.literal(coords(site.pos())).withColor(DIM));
            lines.add(Component.translatable("gui.quarryindustrial.console.site_finder", coords(site.finder()))
                    .withColor(DIM));
            lines.add(Component.translatable("gui.quarryindustrial.console.site_hint").withColor(0xFF5A7090));
        } else {
            int x = screenX(console.getX() + 0.5);
            int z = screenZ(console.getZ() + 0.5);
            if (Math.abs(x - mouseX) <= 4 && Math.abs(z - mouseY) <= 6) {
                lines.add(title.copy().withColor(FLYING));
                lines.add(Component.translatable("gui.quarryindustrial.console.range", snapshot.range()).withColor(DIM));
            }
        }
        if (!lines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    // ── Pestaña drones ──

    private void drawDrones(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Pad> sorted = new ArrayList<>(pads());
        sorted.sort(Comparator.comparingInt(pad -> distance(pad.pos())));
        if (sorted.isEmpty()) {
            emptyMessage(graphics, Component.translatable("gui.quarryindustrial.console.no_pads", snapshot.range()));
            return;
        }
        drawRows(graphics, sorted.size(), mouseX, mouseY, (index, x, y, w, hovered) -> {
            Pad pad = sorted.get(index);
            DronePadStatus status = status(pad);
            if (pad.pos().equals(selected) || pad.pos().equals(linkSource)) {
                graphics.fill(x, y, x + 2, y + ROW, ACCENT);
            }
            graphics.item(padIcon(pad), x + 5, y + 4);
            Component name = padName(pad);
            graphics.text(font, name, x + 25, y + 3, kindColor(pad.kind()), false);
            int afterName = x + 25 + font.width(name) + 6;
            int right = x + w - 4;
            int by = y + (ROW - BUTTON_H) / 2;
            // Botones, de derecha a izquierda
            right = button(graphics, right, by, Component.translatable("gui.quarryindustrial.console.button.view"),
                    DIM, mouseX, mouseY, () -> {
                        selected = pad.pos();
                        showOnMap(pad.pos());
                    });
            if (linkSource != null) {
                if (linkSource.equals(pad.pos())) {
                    right = button(graphics, right, by, Component.translatable(
                            "gui.quarryindustrial.console.button.cancel"), ACCENT, mouseX, mouseY, () -> linkSource = null);
                } else {
                    right = button(graphics, right, by, Component.translatable(
                            "gui.quarryindustrial.console.button.target"), ACCENT, mouseX, mouseY, () -> select(pad));
                }
            } else {
                if (pad.link().isPresent()) {
                    right = button(graphics, right, by, Component.translatable(
                            "gui.quarryindustrial.console.button.unlink"), BAD, mouseX, mouseY, () -> unlink(pad));
                }
                if (linkable(pad)) {
                    right = button(graphics, right, by, Component.translatable(
                            "gui.quarryindustrial.console.button.link"), ACCENT, mouseX, mouseY, () -> startLink(pad));
                }
                if (kind(pad.kind()) == DroneKind.SURVEY && status != DronePadStatus.IN_FLIGHT) {
                    right = button(graphics, right, by, Component.translatable(
                            "gui.quarryindustrial.console.button.survey"), OIL_RIM, mouseX, mouseY, () -> survey(pad));
                }
            }
            // Energía: barra finita antes de los botones
            if (pad.energy() >= 0) {
                int bx = right - 32;
                graphics.fill(bx, y + 10, bx + 28, y + 14, 0xFF1A1A1A);
                graphics.fillGradient(bx, y + 10, bx + 28 * pad.energy() / 1000, y + 14, 0xFFFFD24A, 0xFFFF3D5A);
                right = bx - 4;
            }
            graphics.text(font, font.plainSubstrByWidth(status.label().getString(), Math.max(0, right - afterName)),
                    afterName, y + 3, statusColor(status), false);
            String where = coords(pad.pos()) + " · " + Component.translatable("gui.quarryindustrial.console.distance",
                    distance(pad.pos())).getString();
            if (pad.link().isPresent()) {
                where += "  " + Component.translatable("gui.quarryindustrial.console.link", coords(pad.link().get()),
                        (int) Math.sqrt(pad.pos().distSqr(pad.link().get()))).getString();
            }
            if (!pad.loaded()) {
                where += "  " + Component.translatable("gui.quarryindustrial.console.no_signal").getString();
            }
            graphics.text(font, font.plainSubstrByWidth(where, Math.max(0, right - x - 25)), x + 25, y + 13, DIM, false);
        });
    }

    // ── Pestaña pozos ──

    private void drawWells(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Site> sorted = new ArrayList<>(sites());
        sorted.sort(Comparator.comparingInt(site -> distance(site.pos())));
        if (sorted.isEmpty()) {
            emptyMessage(graphics, Component.translatable("gui.quarryindustrial.console.no_wells"));
            return;
        }
        drawRows(graphics, sorted.size(), mouseX, mouseY, (index, x, y, w, hovered) -> {
            Site site = sorted.get(index);
            drawDrop(graphics, x + 12, y + ROW / 2);
            graphics.text(font, Component.translatable("gui.quarryindustrial.console.site"), x + 25, y + 3, OIL_RIM, false);
            int right = x + w - 4;
            int by = y + (ROW - BUTTON_H) / 2;
            right = button(graphics, right, by, Component.translatable("gui.quarryindustrial.console.button.view"),
                    DIM, mouseX, mouseY, () -> showOnMap(site.pos()));
            right = button(graphics, right, by, Component.translatable("gui.quarryindustrial.console.button.forget"),
                    BAD, mouseX, mouseY, () -> forget(site));
            String where = coords(site.pos()) + " · " + Component.translatable("gui.quarryindustrial.console.distance",
                    distance(site.pos())).getString() + "  " + Component.translatable(
                    "gui.quarryindustrial.console.site_finder", coords(site.finder())).getString();
            graphics.text(font, font.plainSubstrByWidth(where, Math.max(0, right - x - 25)), x + 25, y + 13, DIM, false);
        });
    }

    @FunctionalInterface
    private interface RowDrawer {
        void draw(int index, int x, int y, int w, boolean hovered);
    }

    /** Filas con scroll y barra a la derecha; solo se dibujan (y son clicables) las que se ven. */
    private void drawRows(GuiGraphicsExtractor graphics, int count, int mouseX, int mouseY, RowDrawer drawer) {
        int x = bodyX();
        int y = bodyY();
        int w = bodyW() - 6;
        int h = bodyH();
        int visible = h / ROW;
        scroll = Math.clamp(scroll, 0, Math.max(0, count - visible));
        graphics.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < visible + 1 && scroll + i < count; i++) {
            int ry = y + i * ROW;
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW && mouseY < y + h;
            graphics.fill(x, ry, x + w, ry + ROW - 1, hovered ? PANEL_HOVER : (scroll + i) % 2 == 0 ? PANEL_ALT : PANEL);
            drawer.draw(scroll + i, x, ry, w, hovered);
        }
        graphics.disableScissor();
        // Las filas cortadas abajo no tienen que recibir clics fuera de la lista
        hits.removeIf(hit -> hit.y() + hit.h() > y + h && hit.y() >= y);
        if (count > visible) {
            int barX = x + w + 2;
            graphics.fill(barX, y, barX + 3, y + h, FRAME);
            int thumb = Math.max(10, h * visible / count);
            int thumbY = y + (h - thumb) * scroll / Math.max(1, count - visible);
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumb, DIM);
        }
    }

    private void emptyMessage(GuiGraphicsExtractor graphics, Component message) {
        graphics.textWithWordWrap(font, message, bodyX() + 20, bodyY() + bodyH() / 2 - 10, bodyW() - 40, DIM, false);
    }

    /** Botón chico dibujado a mano, alineado a la derecha en {@code right}. Devuelve el borde para el siguiente. */
    private int button(GuiGraphicsExtractor graphics, int right, int y, Component label, int color, int mouseX,
                       int mouseY, Runnable action) {
        int w = font.width(label) + 8;
        int x = right - w;
        boolean hovered = mouseX >= x && mouseX < right && mouseY >= y && mouseY < y + BUTTON_H;
        graphics.fill(x, y, right, y + BUTTON_H, hovered ? 0xFF24527F : 0xFF102A46);
        graphics.outline(x, y, w, BUTTON_H, color);
        graphics.text(font, label, x + 4, y + 2, hovered ? INK : color, false);
        hits.add(new Hit(x, y, w, BUTTON_H, action));
        return x - 3;
    }

    // ── Primitivas ──

    /** Línea de 1 px (Bresenham), recortada al área del mapa. {@code dash} > 0 la hace a trazos. */
    private void line(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color, int dash) {
        double[] clipped = clip(x0, y0, x1, y1);
        if (clipped == null) {
            return;
        }
        int x = (int) clipped[0];
        int y = (int) clipped[1];
        int ex = (int) clipped[2];
        int ey = (int) clipped[3];
        int dx = Math.abs(ex - x);
        int dy = -Math.abs(ey - y);
        int sx = x < ex ? 1 : -1;
        int sy = y < ey ? 1 : -1;
        int err = dx + dy;
        for (int i = 0; i < 4096; i++) {
            if (dash == 0 || (i / dash) % 2 == 0) {
                graphics.fill(x, y, x + 1, y + 1, color);
            }
            if (x == ex && y == ey) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /** Liang-Barsky contra el rectángulo del mapa: lo que queda adentro, o null si no toca. */
    private double[] clip(double x0, double y0, double x1, double y1) {
        double t0 = 0;
        double t1 = 1;
        double dx = x1 - x0;
        double dy = y1 - y0;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 - mapX, mapX + mapW - 1 - x0, y0 - mapY, mapY + mapH - 1 - y0};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) {
                    return null;
                }
                continue;
            }
            double t = q[i] / p[i];
            if (p[i] < 0) {
                t0 = Math.max(t0, t);
            } else {
                t1 = Math.min(t1, t);
            }
            if (t0 > t1) {
                return null;
            }
        }
        return new double[]{Math.round(x0 + t0 * dx), Math.round(y0 + t0 * dy), Math.round(x0 + t1 * dx),
                Math.round(y0 + t1 * dy)};
    }

    /** Círculo punteado (el alcance de un vínculo). */
    private void circle(GuiGraphicsExtractor graphics, int cx, int cz, double radius, int color) {
        int steps = (int) Math.clamp(radius * 2, 24, 720);
        for (int i = 0; i < steps; i += 2) {
            double a = i * Math.PI * 2 / steps;
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int z = cz + (int) Math.round(Math.sin(a) * radius);
            if (inMap(x, z)) {
                graphics.fill(x, z, x + 1, z + 1, color);
            }
        }
    }

    // ── Entrada ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).contains(mx, my)) {
                    hits.get(i).action().run();
                    return true;
                }
            }
        }
        if (snapshot != null && tab == Tab.MAP && inMap(mx, my)) {
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                if (linkSource != null) {
                    linkSource = null;
                } else {
                    Site site = siteAt(mx, my);
                    if (site != null) {
                        forget(site);
                    }
                }
                return true;
            }
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                Pad pad = padAt(mx, my);
                if (pad != null) {
                    select(pad);
                    return true;
                }
                dragging = true;
                dragMoved = false;
                return true;
            }
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && linkSource != null) {
            linkSource = null;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            centerX -= dragX * scale();
            centerZ -= dragY * scale();
            dragMoved |= dragX != 0 || dragY != 0;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            if (!dragMoved && linkSource == null) {
                // Clic en el vacío: suelta el pad elegido
                selected = null;
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.MAP) {
            if (!inMap(mouseX, mouseY)) {
                return false;
            }
            // Acerca o aleja dejando quieto el punto bajo el mouse
            double beforeX = worldX(mouseX);
            double beforeZ = worldZ(mouseY);
            zoom = Math.clamp(zoom + (scrollY > 0 ? -1 : 1), 0, ZOOMS.length - 1);
            centerX += beforeX - worldX(mouseX);
            centerZ += beforeZ - worldZ(mouseY);
            return true;
        }
        scroll += scrollY > 0 ? -1 : 1;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && linkSource != null) {
            linkSource = null;
            return true;
        }
        // 1, 2, 3: las pestañas
        int index = event.key() - InputConstants.KEY_1;
        if (index >= 0 && index < Tab.values().length) {
            tab = Tab.values()[index];
            scroll = 0;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
