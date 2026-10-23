package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackStatus;
import org.example.pz.quarryindustrial.registry.ModBlocks;

/**
 * Prueba con un géiser real: genera el feature {@code alaindustrial:oil_geyser} de Ala Industrial dentro de
 * un bloque de piedra, pone la cigüeña sobre la boca y comprueba que lo reconozca y empiece a vaciarlo.
 * Saca capturas del balancín en dos momentos de su recorrido.
 */
public class PumpjackClientGameTest implements FabricClientGameTest {
    private static final BlockPos WELLHEAD = new BlockPos(0, 1, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            // El mundo de prueba es plano: hace falta roca hasta la superficie para que entre el domo (Y -45)
            world.getServer().runCommand("fill -16 -60 -16 16 -30 16 minecraft:stone");
            world.getServer().runCommand("fill -16 -29 -16 16 0 16 minecraft:stone");
            world.getServer().runCommand("place feature alaindustrial:oil_geyser 0 1 0");

            boolean geyser = world.getServer().computeOnServer(server ->
                    server.overworld().getFluidState(new BlockPos(0, -45, 0)).isSource());
            Quarryindustrial.LOGGER.info("Géiser generado: domo con oil en y=-45: {}", geyser);
            if (!geyser) {
                throw new AssertionError("El feature de Ala no generó el géiser");
            }

            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(WELLHEAD, ModBlocks.PUMPJACK.defaultBlockState()
                        .setValue(PumpjackBlock.FACING, Direction.NORTH));
                PumpjackBlock.build(level, WELLHEAD, Direction.NORTH);
                PumpjackBlockEntity pumpjack = (PumpjackBlockEntity) level.getBlockEntity(WELLHEAD);
                pumpjack.energy.amount = pumpjack.energy.getCapacity();
            });
            world.getServer().runCommand("gamemode spectator @a");

            for (int i = 0; i < 6; i++) {
                context.waitTicks(20);
                world.getServer().runOnServer(server -> {
                    PumpjackBlockEntity pumpjack = (PumpjackBlockEntity) server.overworld().getBlockEntity(WELLHEAD);
                    pumpjack.energy.amount = pumpjack.energy.getCapacity();
                });
            }
            String report = world.getServer().computeOnServer(server -> {
                PumpjackBlockEntity pumpjack = (PumpjackBlockEntity) server.overworld().getBlockEntity(WELLHEAD);
                return pumpjack.status() + " pozo=" + pumpjack.well() + " oil=" + pumpjack.oilMb() + " mB";
            });
            Quarryindustrial.LOGGER.info("Cigüeña sobre el géiser: {}", report);
            PumpjackStatus status = world.getServer().computeOnServer(server ->
                    ((PumpjackBlockEntity) server.overworld().getBlockEntity(WELLHEAD)).status());
            if (status != PumpjackStatus.DRAINING) {
                throw new AssertionError("Se esperaba DRAINING sobre un géiser real, fue " + report);
            }

            // De costado (desde el este), un poco por encima del balancín
            world.getServer().runCommand("tp @a 9.5 2.2 3.5 90 8");
            world.getConnection().waitForChunksRender();
            context.waitTicks(5);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pumpjack_side_a"));
            context.waitTicks(37);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pumpjack_side_b"));

            // Tres cuartos desde adelante, mirando la cabeza de caballo y el pozo
            world.getServer().runCommand("tp @a 5.5 3.5 -5.5 35 15");
            context.waitTicks(5);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pumpjack_front"));
        }
    }
}
