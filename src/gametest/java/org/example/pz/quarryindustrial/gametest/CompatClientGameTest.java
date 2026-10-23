package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;

import java.nio.file.Path;

/**
 * Con Jade instalado ({@code -PwithJade}): mira una quarry trabajando y saca una captura con el cartel de Jade,
 * para ver que el plugin manda y muestra las líneas.
 */
public class CompatClientGameTest implements FabricClientGameTest {
    private static final BlockPos QUARRY = new BlockPos(0, -60, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded("jade")) {
            return;
        }
        Path output = Path.of(System.getenv().getOrDefault("QUARRY_SHOWCASE_DIR", "screenshots-showcase"))
                .toAbsolutePath();
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("time set 1500");
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(QUARRY, ModBlocks.QUARRY_HV.defaultBlockState()
                        .setValue(QuarryBlock.FACING, Direction.SOUTH));
                QuarryBlockEntity quarry = (QuarryBlockEntity) level.getBlockEntity(QUARRY);
                quarry.energy.amount = quarry.energy.getCapacity() / 2;
                ItemStack bit = new ItemStack(ModItems.bit(BitTier.DIAMOND));
                bit.setDamageValue(bit.getMaxDamage() / 3);
                quarry.swapBit(bit);
            });
            // Parado al frente, mirando la cara de la quarry
            world.getServer().runCommand(String.format("tp @a %d %d %d facing %d %d %d", QUARRY.getX(),
                    QUARRY.getY(), QUARRY.getZ() + 3, QUARRY.getX(), QUARRY.getY(), QUARRY.getZ()));
            world.getConnection().waitForChunksRender();
            context.waitTicks(40);
            Path file = context.takeScreenshot(TestScreenshotOptions.of("20_jade_raw").withDestinationDir(output)
                    .disableCounterPrefix());
            Quarryindustrial.LOGGER.info("Captura de Jade: {}", file);
        }
    }
}
