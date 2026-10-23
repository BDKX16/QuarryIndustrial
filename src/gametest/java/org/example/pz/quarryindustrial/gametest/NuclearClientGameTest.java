package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.client.screen.QuarryScreen;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.NuclearStructure;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlocks;

import java.util.List;

/**
 * Prueba visual de la quarry nuclear: holograma con una carcasa faltante, después el cubo completo con
 * cuatro puertos minando con sus cuatro cabezales, y un primer plano de la broca PDC.
 */
public class NuclearClientGameTest implements FabricClientGameTest {
    private static final BlockPos CORE = new BlockPos(0, -49, 0);
    private static final BlockPos HOLE = CORE.relative(Direction.NORTH, 2).above();

    @Override
    public void runTest(ClientGameTestContext context) {
        QuarryConfig config = QuarryConfig.get();
        config.nuclear.baseSide = 24;
        config.nuclear.maxBlocksPerSecond = 120;
        config.frameBlocksPerTick = 32;
        config.maxSkipsPerTick = 2048;

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -15 -60 -30 14 -50 -1 minecraft:stone");
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                // Mira al sur: el cubo y el área quedan hacia el norte
                level.setBlockAndUpdate(CORE, ModBlocks.QUARRY_CORE.defaultBlockState()
                        .setValue(QuarryBlock.FACING, Direction.SOUTH));
                List<BlockPos> cube = NuclearStructure.positions(CORE, Direction.SOUTH);
                for (BlockPos pos : cube) {
                    boolean port = pos.getY() == CORE.getY() + 1 && !pos.equals(HOLE);
                    level.setBlockAndUpdate(pos, (port ? ModBlocks.ENERGY_PORT : ModBlocks.QUARRY_CASING)
                            .defaultBlockState());
                }
                level.setBlockAndUpdate(HOLE, Blocks.AIR.defaultBlockState());
                ((QuarryBlockEntity) level.getBlockEntity(CORE)).installUpgrade(UpgradeType.VOID);
            });
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("tp @a 4.5 -46 4.5 145 30");
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("nuclear_hologram"));

            world.getServer().runOnServer(server ->
                    server.overworld().setBlockAndUpdate(HOLE, ModBlocks.QUARRY_CASING.defaultBlockState()));
            for (int i = 0; i < 20; i++) {
                world.getServer().runOnServer(server -> {
                    ServerLevel level = server.overworld();
                    for (BlockPos pos : NuclearStructure.positions(CORE, Direction.SOUTH)) {
                        if (level.getBlockEntity(pos) instanceof EnergyPortBlockEntity port) {
                            port.energy.amount = port.energy.getCapacity();
                        }
                    }
                    QuarryBlockEntity core = (QuarryBlockEntity) level.getBlockEntity(CORE);
                    core.energy.amount = core.energy.getCapacity();
                });
                context.waitTicks(20);
            }
            String report = world.getServer().computeOnServer(server -> {
                QuarryBlockEntity core = (QuarryBlockEntity) server.overworld().getBlockEntity(CORE);
                return core.status() + " puertos=" + core.energyPortCount() + " cabezales=" + core.heads().size()
                        + " y=" + core.currentY();
            });
            Quarryindustrial.LOGGER.info("Quarry nuclear: {}", report);

            config.nuclear.maxBlocksPerSecond = 2;
            context.waitTicks(40);
            world.getServer().runCommand("tp @a 22 -34 12 140 30");
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("nuclear_overview"));

            BlockPos head = world.getServer().computeOnServer(server ->
                    ((QuarryBlockEntity) server.overworld().getBlockEntity(CORE)).heads().get(1).cursor());
            world.getServer().runCommand(String.format("tp @a %.2f %.2f %.2f 135 20",
                    head.getX() + 1.45, head.getY() + 0.4, head.getZ() + 1.45));
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("nuclear_pdc_bit"));

            // La GUI, abierta como si el jugador hiciera clic derecho
            // Cerca de la quarry: a más de 8 bloques el menú se cierra solo (stillValid)
            world.getServer().runCommand(String.format("tp @a %d %d %d", CORE.getX(), CORE.getY() + 1, CORE.getZ() + 2));
            context.waitTicks(5);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                    .openMenu((QuarryBlockEntity) server.overworld().getBlockEntity(CORE)));
            context.waitForScreen(QuarryScreen.class);
            context.waitTicks(25);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("nuclear_gui"));
            context.setScreen(() -> null);

        }
    }
}
