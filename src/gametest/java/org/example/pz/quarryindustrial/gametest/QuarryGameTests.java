package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.registry.ModBlocks;

/**
 * La quarry mira al sur, así que su área de 3×3 queda al norte: x 3..5, z 3..5 (relativo a la estructura).
 * El marco va un bloque por fuera (x 2..6, z 2..6): patas en las esquinas desde y=1 y anillo en y=5.
 * Encima de la quarry hay un cofre que recibe los drops.
 */
public class QuarryGameTests {
    private static final BlockPos QUARRY = new BlockPos(4, 1, 6);
    private static final BlockPos CHEST = QUARRY.above();
    private static final int FRAME_TOP = QUARRY.getY() + 4;

    private static void smallArea() {
        QuarryConfig config = QuarryConfig.get();
        config.lv.baseSide = 3;
        config.lv.maxBlocksPerSecond = 200;
        config.frameHeight = 4;
        config.frameBlocksPerTick = 8;
    }

    private static void placeRig(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
        helper.setBlock(QUARRY, ModBlocks.QUARRY_LV.defaultBlockState().setValue(QuarryBlock.FACING, Direction.SOUTH));
        helper.setBlock(CHEST, Blocks.CHEST);
    }

    private static void assertFrame(GameTestHelper helper) {
        for (int x : new int[]{2, 6}) {
            for (int z : new int[]{2, 6}) {
                for (int y = QUARRY.getY(); y <= FRAME_TOP; y++) {
                    helper.assertBlockPresent(ModBlocks.QUARRY_FRAME, x, y, z);
                }
            }
        }
        helper.assertBlockPresent(ModBlocks.QUARRY_FRAME, 4, FRAME_TOP, 2);
        helper.assertBlockPresent(ModBlocks.QUARRY_FRAME, 2, FRAME_TOP, 4);
    }

    @GameTest(maxTicks = 120)
    public void minesLayerIntoChest(GameTestHelper helper) {
        smallArea();
        placeRig(helper);
        QuarryBlockEntity quarry = helper.getBlockEntity(QUARRY, QuarryBlockEntity.class);
        quarry.energy.amount = quarry.energy.getCapacity();

        helper.succeedWhen(() -> {
            assertFrame(helper);
            for (int x = 3; x <= 5; x++) {
                for (int z = 3; z <= 5; z++) {
                    helper.assertBlockNotPresent(Blocks.STONE, x, 1, z);
                }
            }
            ChestBlockEntity chest = helper.getBlockEntity(CHEST, ChestBlockEntity.class);
            helper.assertValueEqual(chest.countItem(Items.COBBLESTONE), 9, "cobblestone en el cofre");
            helper.assertTrue(quarry.energy.amount < quarry.energy.getCapacity(), "la quarry cobró energía");
        });
    }

    /**
     * Agua pegada al borde del área (afuera, en x=2): la quarry la tiene que tapar con un dique y el área
     * tiene que quedar seca. El agua está encerrada en un hueco de piedra para que no corra a otro lado.
     */
    @GameTest(maxTicks = 120)
    public void damsWaterAtTheBorder(GameTestHelper helper) {
        smallArea();
        placeRig(helper);
        for (int x = 1; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        helper.setBlock(1, 1, 4, Blocks.STONE);
        helper.setBlock(2, 1, 3, Blocks.STONE);
        helper.setBlock(2, 1, 5, Blocks.STONE);
        helper.setBlock(2, 2, 4, Blocks.STONE);
        helper.setBlock(2, 1, 4, Blocks.WATER);
        QuarryBlockEntity quarry = helper.getBlockEntity(QUARRY, QuarryBlockEntity.class);
        quarry.energy.amount = quarry.energy.getCapacity();

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.COBBLESTONE, 2, 1, 4);
            for (int x = 3; x <= 5; x++) {
                for (int z = 3; z <= 5; z++) {
                    helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(x, 1, z))).isEmpty(),
                            "sin agua en el área en " + x + ",1," + z);
                    helper.assertBlockNotPresent(Blocks.STONE, x, 1, z);
                }
            }
        });
    }

    @GameTest(maxTicks = 40)
    public void withoutEnergyDoesNothing(GameTestHelper helper) {
        smallArea();
        placeRig(helper);
        QuarryBlockEntity quarry = helper.getBlockEntity(QUARRY, QuarryBlockEntity.class);

        helper.runAfterDelay(20, () -> {
            for (int x = 3; x <= 5; x++) {
                for (int z = 3; z <= 5; z++) {
                    helper.assertBlockPresent(Blocks.STONE, x, 1, z);
                }
            }
            helper.assertBlockNotPresent(ModBlocks.QUARRY_FRAME, 2, 1, 2);
            helper.assertValueEqual(quarry.status(), QuarryStatus.NO_POWER, "estado");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void breakingQuarryRemovesFrame(GameTestHelper helper) {
        smallArea();
        placeRig(helper);
        QuarryBlockEntity quarry = helper.getBlockEntity(QUARRY, QuarryBlockEntity.class);
        quarry.energy.amount = quarry.energy.getCapacity();

        helper.startSequence()
                .thenWaitUntil(() -> assertFrame(helper))
                .thenExecute(() -> helper.setBlock(QUARRY, Blocks.AIR))
                .thenExecuteAfter(1, () -> {
                    helper.assertBlockNotPresent(ModBlocks.QUARRY_FRAME, 2, 1, 2);
                    helper.assertBlockNotPresent(ModBlocks.QUARRY_FRAME, 4, FRAME_TOP, 2);
                    helper.assertBlockNotPresent(ModBlocks.QUARRY_FRAME, 6, FRAME_TOP, 6);
                })
                .thenSucceed();
    }
}
