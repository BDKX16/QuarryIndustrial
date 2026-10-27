package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.client.ExploredTerrain;
import org.example.pz.quarryindustrial.client.screen.DroneConsoleScreen;
import org.example.pz.quarryindustrial.network.ConsolePayloads;

import java.lang.reflect.Field;
import java.util.List;

/**
 * El mapa de la centralita tiene que mostrar lo que el jugador ya recorrió. Avanza en saltos hasta salir de la
 * distancia de visión de donde empezó, comprueba que esos chunks quedaron anotados y que el mapa, mirando esa
 * zona (que ya no está cargada), la dibuja desde lo recordado. Solo corre con {@code QUARRY_EXPLORED=1}.
 */
public class ExploredTerrainClientGameTest implements FabricClientGameTest {
    private static final int STEP = 64;
    private static final int STEPS = 20;
    /** Cuánto se mira hacia atrás del jugador, bien fuera de lo cargado. */
    private static final int LOOK_BACK = 600;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("QUARRY_EXPLORED") == null) {
            return;
        }
        context.runOnClient(minecraft -> minecraft.options.renderDistance().set(8));
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode spectator @a");
            for (int step = 0; step <= STEPS; step++) {
                int blockX = step * STEP;
                world.getServer().runCommand("tp @a " + blockX + " 100 0");
                int centre = blockX >> 4;
                // Que lleguen las esquinas de la zona que se va a comprobar, no solo el chunk del medio
                context.waitFor(minecraft -> minecraft.level != null
                        && minecraft.level.getChunkSource().hasChunk(centre, 0)
                        && minecraft.level.getChunkSource().hasChunk(centre - 4, -4)
                        && minecraft.level.getChunkSource().hasChunk(centre - 4, 4)
                        && minecraft.level.getChunkSource().hasChunk(centre + 4, -4)
                        && minecraft.level.getChunkSource().hasChunk(centre + 4, 4), 20 * 60);
                context.waitTicks(10);
            }

            // Todo lo que quedó atrás (más allá de la distancia de visión del punto final) tiene que estar anotado
            int total = 0;
            int missing = 0;
            int lastChunk = STEPS * STEP / 16;
            for (int chunkX = -4; chunkX <= lastChunk - 12; chunkX++) {
                for (int chunkZ = -4; chunkZ <= 4; chunkZ++) {
                    total++;
                    int cx = chunkX;
                    int cz = chunkZ;
                    if (!context.computeOnClient(minecraft -> ExploredTerrain.get(minecraft.level, cx, cz) != null)) {
                        missing++;
                    }
                }
            }
            Quarryindustrial.LOGGER.info("Terreno recordado: faltan {} de {} chunks recorridos", missing, total);
            if (missing > 0) {
                throw new AssertionError("Faltan " + missing + " de " + total + " chunks recorridos en el mapa recordado");
            }

            // La centralita junto al jugador (si no, la pantalla se cierra sola)
            BlockPos console = new BlockPos(STEPS * STEP + 2, 100, 0);
            context.setScreen(() -> new DroneConsoleScreen(console));
            context.runOnClient(minecraft -> ((DroneConsoleScreen) minecraft.gui.screen()).accept(
                    new ConsolePayloads.Snapshot(console, 1024, 512, List.of(), List.of(), List.of())));
            context.waitTicks(40);
            int loaded = coloredCells(context);
            Quarryindustrial.LOGGER.info("Mapa de la centralita, zona cargada: {} celdas con terreno", loaded);

            // Mirar hacia atrás: ahí no hay nada cargado, solo lo recordado
            context.runOnClient(minecraft -> {
                setField(minecraft.gui.screen(), "centerX", STEPS * STEP - LOOK_BACK);
                setField(minecraft.gui.screen(), "centerZ", 0);
            });
            context.waitTicks(20);
            int remembered = coloredCells(context);
            Quarryindustrial.LOGGER.info("Mapa de la centralita, zona recordada: {} celdas con terreno", remembered);

            // Alejado (8 bloques por píxel) cada celda promedia varios bloques en vez de quedarse con uno
            context.runOnClient(minecraft -> setZoom(minecraft.gui.screen(), 5));
            context.waitTicks(20);
            int zoomedOut = coloredCells(context);
            Quarryindustrial.LOGGER.info("Mapa de la centralita, alejado: {} celdas con terreno", zoomedOut);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot(
                    TestScreenshotOptions.of("explored_map_zoomed_out").withSize(1920, 1080)));
            context.setScreen(() -> null);

            if (zoomedOut == 0) {
                throw new AssertionError("El mapa alejado no dibujó terreno");
            }
            if (loaded == 0) {
                throw new AssertionError("El mapa no dibujó el terreno cargado");
            }
            if (remembered == 0) {
                throw new AssertionError("El mapa no dibujó el terreno recordado de una zona ya descargada");
            }
        }
    }

    private static void setZoom(Object target, int zoom) {
        try {
            Field field = DroneConsoleScreen.class.getDeclaredField("zoom");
            field.setAccessible(true);
            field.setInt(target, zoom);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setField(Object target, String name, double value) {
        try {
            Field field = DroneConsoleScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setDouble(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Celdas del terreno del mapa de la centralita abierta que tienen color. */
    private static int coloredCells(ClientGameTestContext context) {
        return context.computeOnClient(minecraft -> {
            try {
                Field field = DroneConsoleScreen.class.getDeclaredField("terrain");
                field.setAccessible(true);
                int colored = 0;
                for (int cell : (int[]) field.get(minecraft.gui.screen())) {
                    if (cell != 0) {
                        colored++;
                    }
                }
                return colored;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
    }
}
