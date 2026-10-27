package org.example.pz.quarryindustrial.gametest;

import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.registry.ModItems;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.NuclearStructure;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.client.screen.QuarryScreen;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlocks;

import java.nio.file.Path;
import java.util.List;

/**
 * Capturas para la página del mod (CurseForge / README): un mundo normal con semilla fija, una quarry HV, la
 * quarry nuclear y una cigüeña sobre un géiser real de Ala Industrial, en 1920×1080 y sin HUD.
 *
 * <p>Solo corre con la variable de entorno {@code QUARRY_SHOWCASE} (tarda varios minutos):
 * {@code QUARRY_SHOWCASE=1 ./gradlew runClientGameTest}; después, {@code python tools/finish_showcase.py}.
 */
public class ShowcaseClientGameTest implements FabricClientGameTest {
    static final String SEED = "quarryindustrial";
    static final int CHUNK_TIMEOUT = 20 * 600;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("QUARRY_SHOWCASE") == null) {
            return;
        }
        Path output = Path.of(System.getenv().getOrDefault("QUARRY_SHOWCASE_DIR", "screenshots-showcase"))
                .toAbsolutePath();
        QuarryConfig config = QuarryConfig.get();
        config.hv.maxBlocksPerSecond = 400;
        config.nuclear.baseSide = 32;
        config.nuclear.maxBlocksPerSecond = 500;
        config.frameBlocksPerTick = 32;
        config.maxSkipsPerTick = 2048;

        context.runOnClient(minecraft -> {
            minecraft.options.renderDistance().set(6);
            minecraft.options.fov().set(70);
        });

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(settings -> {
                    settings.setWorldType(settings.getNormalPresetList().getFirst());
                    settings.setSeed(SEED);
                    settings.setGenerateStructures(false);
                })
                .create()) {
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule advance_weather false");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("time set 1500");
            world.getServer().runCommand("gamemode spectator @a");

            BlockPos plains = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
                        biome -> biome.is(Biomes.PLAINS), level.getRespawnData().pos(), 6400, 32, 64);
                return found != null ? found.getFirst() : level.getRespawnData().pos();
            });
            Quarryindustrial.LOGGER.info("Showcase: pradera en {}", plains);

            // ── Escena 1: quarry HV conectada a Ala con cable de electrum ──
            // El área (32×32) queda al norte de la quarry: buscamos un cuadrado seco y parejo para ella
            BlockPos hvArea = findSite(world, context, plains, 40);
            BlockPos hv = surface(world, context, hvArea.offset(0, 0, 18));
            clearAbove(world, hvArea, 20, hv.getY());
            prepareSite(world, hv, 6);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(hv, ModBlocks.QUARRY_HV.defaultBlockState()
                        .setValue(QuarryBlock.FACING, Direction.SOUTH));
                QuarryBlockEntity quarry = (QuarryBlockEntity) level.getBlockEntity(hv);
                quarry.installUpgrade(UpgradeType.VOID);
                quarry.installUpgrade(UpgradeType.SPEED);
                quarry.installUpgrade(UpgradeType.FORTUNE);
            });
            command(world, "setblock %d %d %d alaindustrial:insulated_electrum_cable", hv.getX(), hv.getY(), hv.getZ() + 1);
            command(world, "setblock %d %d %d alaindustrial:insulated_electrum_cable", hv.getX(), hv.getY(), hv.getZ() + 2);
            command(world, "setblock %d %d %d alaindustrial:cesu", hv.getX(), hv.getY(), hv.getZ() + 3);
            command(world, "setblock %d %d %d minecraft:chest", hv.getX(), hv.getY() + 1, hv.getZ());

            // ── Escena 2: quarry nuclear, en otro sitio seco al este ──
            BlockPos coreArea = findSite(world, context, hvArea.offset(80, 0, 0), 40);
            BlockPos core = surface(world, context, coreArea.offset(0, 0, 20));
            clearAbove(world, coreArea, 20, core.getY());
            prepareSite(world, core, 5);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(core, ModBlocks.QUARRY_CORE.defaultBlockState()
                        .setValue(QuarryBlock.FACING, Direction.SOUTH));
                for (BlockPos pos : NuclearStructure.positions(core, Direction.SOUTH)) {
                    // Puertos en la fila de arriba (uno por reactor), carcasa en el resto
                    boolean port = pos.getY() == core.getY() + 1 && !pos.equals(core.above());
                    level.setBlockAndUpdate(pos, (port ? ModBlocks.ENERGY_PORT : ModBlocks.QUARRY_CASING).defaultBlockState());
                }
                ((QuarryBlockEntity) level.getBlockEntity(core)).installUpgrade(UpgradeType.VOID);
            });

            // ── Escena 3: géiser de Ala con una cigüeña y un tanque ──
            BlockPos well = surface(world, context, findSite(world, context, hvArea.offset(0, 0, 70), 14));
            prepareSite(world, well, 6);
            command(world, "place feature alaindustrial:oil_geyser %d %d %d", well.getX(), well.getY(), well.getZ());
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(well, ModBlocks.PUMPJACK.defaultBlockState()
                        .setValue(PumpjackBlock.FACING, Direction.NORTH));
                PumpjackBlock.build(level, well, Direction.NORTH);
            });
            // Caño desde el costado de la base hasta un tanque de Ala
            for (int i = 2; i <= 4; i++) {
                command(world, "setblock %d %d %d alaindustrial:fluid_pipe", well.getX() + i, well.getY(), well.getZ() + 2);
            }
            command(world, "setblock %d %d %d alaindustrial:fluid_tank", well.getX() + 5, well.getY(), well.getZ() + 2);
            // Como haría el jugador: la cara del último caño hacia el tanque, en "insertar" con la llave de Ala
            world.getServer().runOnServer(server -> {
                Object pipe = server.overworld().getBlockEntity(well.offset(4, 0, 2));
                try {
                    var cycle = pipe.getClass().getMethod("cycleFaceMode", Direction.class);
                    cycle.invoke(pipe, Direction.EAST);
                    cycle.invoke(pipe, Direction.EAST);
                } catch (ReflectiveOperationException e) {
                    Quarryindustrial.LOGGER.warn("Showcase: no se pudo configurar el caño de Ala", e);
                }
            });

            // Que trabajen un rato (con energía infinita, lo que interesa es cómo se ve)
            for (int i = 0; i < 40; i++) {
                feed(world, hv, core, well);
                context.waitTicks(20);
            }
            // Más despacio para las fotos: el carro queda a mitad de recorrido y el balancín se mueve suave
            config.hv.maxBlocksPerSecond = 2;
            config.nuclear.maxBlocksPerSecond = 3;
            feed(world, hv, core, well);
            context.waitTicks(60);
            Quarryindustrial.LOGGER.info("Showcase: HV {} / nuclear {} / cigüeña {}",
                    status(world, hv), status(world, core), pumpjackStatus(world, well));

            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitTicks(2);

            BlockPos hvHead = head(world, hv, 0);
            shot(world, context, output, "01_quarry_hv",
                    hv.offset(26, 20, 10), hv.offset(0, -8, -18));
            shot(world, context, output, "02_drill_closeup",
                    hvHead.offset(2, 1, 2), hvHead.offset(0, 1, 0));
            shot(world, context, output, "03_gantry",
                    hvHead.atY(hv.getY() + 5).offset(4, 2, 4), hvHead.atY(hv.getY() + 4));
            shot(world, context, output, "04_nuclear",
                    core.offset(-24, 18, 12), core.offset(0, -6, -20));
            BlockPos nuclearHead = head(world, core, 1);
            shot(world, context, output, "05_pdc_closeup",
                    nuclearHead.offset(2, 1, 2), nuclearHead.offset(0, 1, 0));
            shot(world, context, output, "06_pumpjack",
                    well.offset(-10, 4, 2), well.offset(0, 3, 3));
            context.waitTicks(30);
            shot(world, context, output, "07_pumpjack_front",
                    well.offset(-6, 3, -6), well.offset(0, 3, 2));
            shot(world, context, output, "08_ports",
                    core.offset(4, 3, 3), core.offset(0, 1, -1));

            // GUI: con HUD, al lado de la quarry
            context.getInput().pressKey(options -> options.keyToggleGui);
            command(world, "tp @a %d %d %d", hv.getX(), hv.getY() + 1, hv.getZ() + 2);
            context.waitTicks(5);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                    .openMenu((QuarryBlockEntity) server.overworld().getBlockEntity(hv)));
            context.waitForScreen(QuarryScreen.class);
            context.waitTicks(25);
            // Al tamaño de la ventana: con withSize la GUI se dibuja con el layout de la ventana chica.
            // tools/finish_showcase.py la recorta y la amplía para la página.
            context.takeScreenshot(TestScreenshotOptions.of("09_gui_raw").withDestinationDir(output)
                    .disableCounterPrefix());
            context.setScreen(() -> null);
        }
    }

    /** Altura del suelo en (x, z), con los chunks ya cargados alrededor. */
    /**
     * Espera a que el cliente tenga el centro y las esquinas de la zona (no a que termine de bajar todo, que en un
     * mundo recién generado no vuelve nunca).
     */
    static void waitForArea(ClientGameTestContext context, int chunkX, int chunkZ) {
        context.waitFor(minecraft -> minecraft.level != null
                && minecraft.level.getChunkSource().hasChunk(chunkX, chunkZ)
                && minecraft.level.getChunkSource().hasChunk(chunkX - 4, chunkZ - 4)
                && minecraft.level.getChunkSource().hasChunk(chunkX - 4, chunkZ + 4)
                && minecraft.level.getChunkSource().hasChunk(chunkX + 4, chunkZ - 4)
                && minecraft.level.getChunkSource().hasChunk(chunkX + 4, chunkZ + 4), CHUNK_TIMEOUT);
        context.waitTicks(20);
    }

    static BlockPos surface(TestSingleplayerContext world, ClientGameTestContext context, BlockPos around) {
        world.getServer().runCommand(String.format("tp @a %d 120 %d", around.getX(), around.getZ()));
        // En un mundo normal la generación es lenta: más margen que el que trae por defecto
        waitForArea(context, around.getX() >> 4, around.getZ() >> 4);
        context.waitTicks(5);
        return world.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            level.getChunk(around.getX() >> 4, around.getZ() >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, around.getX(), around.getZ());
            return new BlockPos(around.getX(), y, around.getZ());
        });
    }

    /**
     * Busca, alrededor de {@code start}, el centro de un cuadrado de {@code size} bloques sin agua en la
     * superficie y con menos de 5 bloques de desnivel. Recorre una grilla de candidatos de cerca a lejos.
     */
    /** Centros ya elegidos: los sitios siguientes se buscan lejos de ellos para que las escenas no se pisen. */
    private static final List<BlockPos> USED_SITES = new java.util.ArrayList<>();
    private static final int SITE_SPACING = 64;

    static BlockPos findSite(TestSingleplayerContext world, ClientGameTestContext context, BlockPos start,
                                     int size) {
        BlockPos found = world.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            List<BlockPos> candidates = new java.util.ArrayList<>();
            for (int dx = -320; dx <= 320; dx += 40) {
                for (int dz = -320; dz <= 320; dz += 40) {
                    candidates.add(start.offset(dx, 0, dz));
                }
            }
            candidates.sort(java.util.Comparator.comparingDouble(c -> c.distSqr(start)));
            for (BlockPos center : candidates) {
                boolean farFromOthers = USED_SITES.stream().allMatch(used ->
                        Math.max(Math.abs(used.getX() - center.getX()), Math.abs(used.getZ() - center.getZ())) >= SITE_SPACING);
                if (farFromOthers && siteIsGood(level, center, size)) {
                    return center;
                }
            }
            return start;
        });
        USED_SITES.add(found);
        Quarryindustrial.LOGGER.info("Showcase: sitio de {}×{} en {}", size, size, found);
        return surface(world, context, found);
    }

    private static boolean siteIsGood(ServerLevel level, BlockPos center, int size) {
        int half = size / 2;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = -half; x <= half; x += 3) {
            for (int z = -half; z <= half; z += 3) {
                int bx = center.getX() + x;
                int bz = center.getZ() + z;
                level.getChunk(bx >> 4, bz >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                if (!level.getFluidState(new BlockPos(bx, y - 1, bz)).isEmpty()) {
                    return false;
                }
                min = Math.min(min, y);
                max = Math.max(max, y);
                if (max - min > 4) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Saca árboles y plantas sobre el área (lo que haría un jugador antes de poner la quarry). */
    static void clearAbove(TestSingleplayerContext world, BlockPos center, int half, int y) {
        command(world, "fill %d %d %d %d %d %d air replace #minecraft:logs", center.getX() - half, y - 2,
                center.getZ() - half, center.getX() + half, y + 14, center.getZ() + half);
        command(world, "fill %d %d %d %d %d %d air replace #minecraft:leaves", center.getX() - half, y - 2,
                center.getZ() - half, center.getX() + half, y + 14, center.getZ() + half);
    }

    /** Despeja árboles y arbustos alrededor del bloque y deja un piso de pasto parejo. */
    static void prepareSite(TestSingleplayerContext world, BlockPos pos, int radius) {
        command(world, "fill %d %d %d %d %d %d air", pos.getX() - radius, pos.getY(), pos.getZ() - radius,
                pos.getX() + radius, pos.getY() + 10, pos.getZ() + radius);
        command(world, "fill %d %d %d %d %d %d grass_block", pos.getX() - radius, pos.getY() - 1, pos.getZ() - radius,
                pos.getX() + radius, pos.getY() - 1, pos.getZ() + radius);
    }

    private static void feed(TestSingleplayerContext world, BlockPos hv, BlockPos core, BlockPos well) {
        world.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            if (level.getBlockEntity(hv) instanceof QuarryBlockEntity quarry) {
                quarry.energy.amount = quarry.energy.getCapacity();
                if (quarry.installedBit() == null) {
                    quarry.swapBit(new ItemStack(ModItems.bit(BitTier.DIAMOND)));
                }
            }
            for (BlockPos pos : NuclearStructure.positions(core, Direction.SOUTH)) {
                if (level.getBlockEntity(pos) instanceof EnergyPortBlockEntity port) {
                    port.energy.amount = port.energy.getCapacity();
                }
            }
            if (level.getBlockEntity(core) instanceof QuarryBlockEntity nuclear) {
                nuclear.energy.amount = nuclear.energy.getCapacity();
                if (nuclear.installedBit() == null) {
                    nuclear.swapBit(new ItemStack(ModItems.bit(BitTier.PDC)));
                }
            }
            if (level.getBlockEntity(well) instanceof PumpjackBlockEntity pumpjack) {
                pumpjack.energy.amount = pumpjack.energy.getCapacity();
            }
        });
    }

    static BlockPos head(TestSingleplayerContext world, BlockPos quarry, int index) {
        return world.getServer().computeOnServer(server -> {
            List<?> heads = ((QuarryBlockEntity) server.overworld().getBlockEntity(quarry)).heads();
            return ((QuarryBlockEntity) server.overworld().getBlockEntity(quarry)).heads()
                    .get(Math.min(index, heads.size() - 1)).cursor();
        });
    }

    private static String status(TestSingleplayerContext world, BlockPos pos) {
        return world.getServer().computeOnServer((MinecraftServer server) ->
                ((QuarryBlockEntity) server.overworld().getBlockEntity(pos)).status().name());
    }

    private static String pumpjackStatus(TestSingleplayerContext world, BlockPos pos) {
        return world.getServer().computeOnServer((MinecraftServer server) ->
                ((PumpjackBlockEntity) server.overworld().getBlockEntity(pos)).status().name());
    }

    /** Pone la cámara en {@code eye} mirando a {@code target} y saca una captura en 1920×1080. */
    static void shot(TestSingleplayerContext world, ClientGameTestContext context, Path output, String name,
                             BlockPos eye, BlockPos target) {
        // tp ubica los pies; la cámara queda 1.62 más arriba
        command(world, "tp @a %.2f %.2f %.2f facing %.2f %.2f %.2f", eye.getX() + 0.5, eye.getY() - 1.12,
                eye.getZ() + 0.5, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        waitForArea(context, eye.getX() >> 4, eye.getZ() >> 4);
        context.waitTicks(8);
        Path file = context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1920, 1080)
                .withDestinationDir(output).disableCounterPrefix());
        Quarryindustrial.LOGGER.info("Showcase: {}", file);
    }

    static void command(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(String.format(java.util.Locale.ROOT, format, args));
    }
}
