package org.example.pz.quarryindustrial.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.client.screen.DroneConsoleScreen;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.drone.DroneNetwork;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.CHUNK_TIMEOUT;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.command;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.prepareSite;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.surface;

/**
 * Capturas de la centralita de drones en un mundo real (la semilla del showcase): una red de pads alrededor,
 * drones volando entre ellos, un explorador barriendo y géiseres en el mapa. Saca las tres pestañas y la ficha
 * de un pad. Solo corre con {@code QUARRY_CONSOLE=1}; deja las capturas en {@code QUARRY_SHOWCASE_DIR}.
 */
public class DroneConsoleClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("QUARRY_CONSOLE") == null) {
            return;
        }
        Path output = Path.of(System.getenv().getOrDefault("QUARRY_SHOWCASE_DIR", "screenshots-showcase"))
                .toAbsolutePath();
        QuarryConfig config = QuarryConfig.get();
        config.drones.speed = 0.25;
        config.drones.launchIdleTicks = 20;
        config.drones.surveyRadius = 48;
        config.drones.powerLaunchEu = 20_000;

        context.runOnClient(minecraft -> minecraft.options.renderDistance().set(12));
        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(settings -> {
                    settings.setWorldType(settings.getNormalPresetList().getFirst());
                    settings.setSeed(ShowcaseClientGameTest.SEED);
                    settings.setGenerateStructures(false);
                })
                .create()) {
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("time set 1500");
            world.getServer().runCommand("gamemode spectator @a");

            BlockPos plains = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
                        biome -> biome.is(Biomes.PLAINS), level.getRespawnData().pos(), 6400, 32, 64);
                return found != null ? found.getFirst() : level.getRespawnData().pos();
            });
            BlockPos console = surface(world, context, plains);
            prepareSite(world, console, 4);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(console, ModBlocks.DRONE_CONSOLE.defaultBlockState());
                // Dos rutas de carga, una de energía, mantenimiento, un pad vacío y el explorador
                BlockPos cargoA = pad(level, console.offset(-46, 0, 22), DroneKind.CARGO, Items.COBBLESTONE);
                BlockPos cargoB = pad(level, console.offset(38, 0, -30), null, null);
                link(level, cargoA, cargoB);
                BlockPos cargoC = pad(level, console.offset(52, 0, 40), DroneKind.CARGO, Items.OAK_LOG);
                link(level, cargoC, pad(level, console.offset(-20, 0, -52), null, null));
                BlockPos power = pad(level, console.offset(-64, 0, -18), DroneKind.POWER, null);
                link(level, power, cargoB);
                pad(level, console.offset(14, 0, 12), DroneKind.MAINTENANCE, null);
                pad(level, console.offset(70, 0, 4), DroneKind.TANKER, null);
                BlockPos survey = pad(level, console.offset(-12, 0, 34), DroneKind.SURVEY, null);
                ((DronePadBlockEntity) level.getBlockEntity(survey)).requestSurvey();
                DroneNetwork.get(level).addSites(List.of(ground(level, console.offset(-58, 0, 58)),
                        ground(level, console.offset(24, 0, 70)), ground(level, console.offset(80, 0, -48)),
                        ground(level, console.offset(-84, 0, -60))), survey);
            });
            // Que despeguen
            context.waitTicks(80);

            command(world, "tp @a %d %d %d", console.getX() + 2, console.getY() + 1, console.getZ() + 2);
            world.getConnection().waitForChunksRender(true, CHUNK_TIMEOUT);
            context.setScreen(() -> new DroneConsoleScreen(console));
            context.waitTicks(40);
            screenshot(context, output, "30_console_map");
            context.getInput().pressKey(InputConstants.KEY_2);
            context.waitTicks(4);
            screenshot(context, output, "31_console_drones");
            // "Ver" en la primera fila: elige el pad más cercano y vuelve al mapa con su ficha
            context.runOnClient(minecraft -> {
                double guiScale = minecraft.getWindow().getGuiScale();
                int width = minecraft.getWindow().getGuiScaledWidth();
                int height = minecraft.getWindow().getGuiScaledHeight();
                int panelW = Math.min(440, width - 16);
                int panelH = Math.min(270, height - 16);
                int right = (width - panelW) / 2 + 6 + panelW - 12 - 6 - 4;
                int y = (height - panelH) / 2 + 24 + 12;
                context.getInput().setCursorPos((right - 8) * guiScale, (y + 6) * guiScale);
            });
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(4);
            screenshot(context, output, "32_console_selected");
            context.getInput().pressKey(InputConstants.KEY_3);
            context.waitTicks(4);
            screenshot(context, output, "33_console_wells");
            context.setScreen(() -> null);
        }
    }

    private static void screenshot(ClientGameTestContext context, Path output, String name) {
        Path file = context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1920, 1080)
                .withDestinationDir(output).disableCounterPrefix());
        Quarryindustrial.LOGGER.info("Centralita: {}", file);
    }

    private static BlockPos ground(ServerLevel level, BlockPos pos) {
        return new BlockPos(pos.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(),
                pos.getZ()), pos.getZ());
    }

    /** Un pad con energía en el suelo, con su dron y un cofre al lado (con algo para llevar, si se pasa). */
    private static BlockPos pad(ServerLevel level, BlockPos around, @Nullable DroneKind kind,
                                @Nullable Item cargo) {
        BlockPos pos = ground(level, around);
        level.setBlockAndUpdate(pos, ModBlocks.DRONE_PAD.defaultBlockState());
        DronePadBlockEntity pad = (DronePadBlockEntity) level.getBlockEntity(pos);
        pad.energy.amount = pad.energy.getCapacity();
        if (kind != null) {
            pad.insertDrone(new ItemStack(ModItems.drone(kind)));
        }
        if (cargo != null) {
            level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) level.getBlockEntity(pos.east())).setItem(0, new ItemStack(cargo, 64));
        }
        return pos;
    }

    private static void link(ServerLevel level, BlockPos source, BlockPos target) {
        ((DronePadBlockEntity) level.getBlockEntity(source)).setLink(target);
    }
}
