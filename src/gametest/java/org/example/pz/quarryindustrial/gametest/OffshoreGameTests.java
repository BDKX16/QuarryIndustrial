package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.example.pz.quarryindustrial.block.offshore.OffshoreLayout;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlock;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;

/**
 * Plataforma offshore sobre un "mar" de prueba: piso de piedra en y=0, agua de y=1 a y=4 en x/z 0..6 y el
 * controlador en (3, 5, 3). El mundo de prueba no es océano, así que se apaga ese requisito.
 */
public class OffshoreGameTests {
    private static final BlockPos CONTROLLER = new BlockPos(3, 5, 3);

    private static void sea(GameTestHelper helper) {
        QuarryConfig.Offshore config = QuarryConfig.get().offshore;
        config.requireOceanBiome = false;
        config.minWaterDepth = 4;
        config.drillDepth = 2;
        config.drillTicksPerMeter = 2;
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.WATER);
                }
            }
        }
    }

    private static OffshoreRigBlockEntity placeRig(GameTestHelper helper) {
        helper.assertTrue(OffshoreLayout.check(helper.getLevel(), helper.absolutePos(CONTROLLER)) == null,
                "el lugar sirve");
        helper.setBlock(CONTROLLER, ModBlocks.OFFSHORE_RIG);
        OffshoreRigBlock.build(helper.getLevel(), helper.absolutePos(CONTROLLER));
        OffshoreRigBlockEntity rig = helper.getBlockEntity(CONTROLLER, OffshoreRigBlockEntity.class);
        rig.energy.amount = rig.energy.getCapacity();
        return rig;
    }

    @GameTest(maxTicks = 200, skyAccess = true)
    public void drillsThenProducesOil(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return;
        }
        sea(helper);
        OffshoreRigBlockEntity rig = placeRig(helper);
        helper.assertBlockPresent(ModBlocks.OFFSHORE_DECK, 0, 5, 0);
        helper.assertBlockPresent(ModBlocks.OFFSHORE_LEG, 0, 1, 0);
        helper.assertBlockPresent(ModBlocks.OFFSHORE_LEG, 6, 4, 6);
        helper.assertValueEqual(rig.seabedY(), helper.absolutePos(CONTROLLER).getY() - 5, "lecho marino");

        helper.startSequence()
                .thenExecuteAfter(5, () -> helper.assertValueEqual(rig.status(), OffshoreStatus.NO_BIT, "sin broca"))
                .thenExecute(() -> rig.swapBit(new ItemStack(ModItems.bit(BitTier.IRON))))
                .thenWaitUntil(() -> {
                    helper.assertValueEqual(rig.status(), OffshoreStatus.PRODUCING, "estado");
                    helper.assertTrue(rig.oilMb() > 0, "oil producido");
                })
                .thenExecute(() -> {
                    helper.assertValueEqual(rig.bit().getDamageValue(), 2 * QuarryConfig.get().offshore.bitWearPerMeter,
                            "la broca se gastó por metro");
                    Storage<FluidVariant> deck = FluidStorage.SIDED.find(helper.getLevel(),
                            helper.absolutePos(new BlockPos(0, 5, 3)), Direction.UP);
                    helper.assertTrue(deck != null && deck.supportsExtraction() && !deck.supportsInsertion(),
                            "la cubierta solo da oil");
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 40, skyAccess = true)
    public void breakingTheDeckDismantlesTheRig(GameTestHelper helper) {
        sea(helper);
        placeRig(helper);
        helper.destroyBlock(new BlockPos(6, 5, 6));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ModBlocks.OFFSHORE_RIG, CONTROLLER);
            helper.assertBlockNotPresent(ModBlocks.OFFSHORE_DECK, 0, 5, 0);
            helper.assertBlockPresent(Blocks.WATER, 0, 2, 0);
            helper.assertItemEntityPresent(ModBlocks.OFFSHORE_RIG.asItem());
            helper.succeed();
        });
    }
}
