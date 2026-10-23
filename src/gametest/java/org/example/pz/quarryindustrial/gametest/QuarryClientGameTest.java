package org.example.pz.quarryindustrial.gametest;

import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.client.screen.QuarryScreen;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlocks;

/**
 * Prueba visual: arma una quarry HV sobre un bloque de piedra, la deja minar unos segundos y saca capturas
 * del pórtico y de la broca. Las capturas quedan en {@code run/screenshots} para revisarlas a ojo.
 */
public class QuarryClientGameTest implements FabricClientGameTest {
    private static final BlockPos QUARRY = new BlockPos(0, -49, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        QuarryConfig config = QuarryConfig.get();
        // Rápido al principio para cavar un pozo de varias capas
        config.hv.maxBlocksPerSecond = 400;
        config.maxSkipsPerTick = 2048;
        config.frameBlocksPerTick = 16;

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule advance_weather false");
            world.getServer().runCommand("weather clear");
            // Un cubo de piedra para que la quarry tenga algo que excavar (el mundo de prueba es plano)
            world.getServer().runCommand("fill -20 -60 -36 19 -50 1 minecraft:stone");
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(QUARRY, ModBlocks.QUARRY_HV.defaultBlockState()
                        .setValue(QuarryBlock.FACING, Direction.SOUTH));
                QuarryBlockEntity quarry = (QuarryBlockEntity) level.getBlockEntity(QUARRY);
                quarry.energy.amount = quarry.energy.getCapacity();
                // Sin cofre: el void se come la piedra para que el buffer no se llene y el pozo baje
                quarry.installUpgrade(UpgradeType.VOID);
                quarry.installUpgrade(UpgradeType.SPEED);
                quarry.installUpgrade(UpgradeType.SPEED);
                quarry.installUpgrade(UpgradeType.FORTUNE);
                quarry.swapBit(new ItemStack(ModItems.bit(BitTier.DIAMOND)));
            });
            world.getServer().runCommand("gamemode spectator @a");

            // Recarga energía mientras trabaja, para que no se frene durante la prueba
            for (int i = 0; i < 25; i++) {
                context.waitTicks(20);
                world.getServer().runOnServer(server -> {
                    QuarryBlockEntity quarry = (QuarryBlockEntity) server.overworld().getBlockEntity(QUARRY);
                    quarry.energy.amount = quarry.energy.getCapacity();
                });
            }

            // Casi quieta para las capturas: el carro avanza un bloque por segundo y la cámara lo alcanza
            config.hv.maxBlocksPerSecond = 1;
            context.waitTicks(40);

            world.getServer().runCommand("tp @a 26 -34 12 140 28");
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("quarry_overview"));

            BlockPos head = world.getServer().computeOnServer(server -> {
                QuarryBlockEntity quarry = (QuarryBlockEntity) server.overworld().getBlockEntity(QUARRY);
                return quarry.heads().getFirst().cursor();
            });
            Quarryindustrial.LOGGER.info("Cabezal 0 en {}", head);
            world.getServer().runCommand(String.format("tp @a %.2f %.2f %.2f 135 20",
                    head.getX() + 1.45, head.getY() + 0.4, head.getZ() + 1.45));
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("quarry_drill_bit"));

            world.getServer().runCommand(String.format("tp @a %.1f %.1f %.1f 135 22",
                    head.getX() + 2.5, QUARRY.getY() + 5.6, head.getZ() + 2.5));
            context.waitTicks(5);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("quarry_carriage"));

            // La GUI, abierta como si el jugador hiciera clic derecho
            // Cerca de la quarry: a más de 8 bloques el menú se cierra solo (stillValid)
            world.getServer().runCommand(String.format("tp @a %d %d %d", QUARRY.getX(), QUARRY.getY() + 1, QUARRY.getZ() + 2));
            context.waitTicks(5);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                    .openMenu((QuarryBlockEntity) server.overworld().getBlockEntity(QUARRY)));
            context.waitForScreen(QuarryScreen.class);
            context.waitTicks(25);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("quarry_gui"));
            context.setScreen(() -> null);

        }
    }
}
