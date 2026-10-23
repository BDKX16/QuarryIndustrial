package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;

/**
 * Brocas, refrigerante y buscavetas. Mismo armado que {@link QuarryGameTests}: quarry LV en (4, 1, 6) mirando
 * al sur, área de 3×3 en x 3..5, z 3..5 (una sola capa de piedra en y=1) y un cofre encima de la quarry.
 */
public class DrillBitGameTests {
    private static final BlockPos QUARRY = new BlockPos(4, 1, 6);
    private static final BlockPos CHEST = QUARRY.above();

    private static QuarryBlockEntity rig(GameTestHelper helper, ItemStack bit) {
        QuarryConfig config = QuarryConfig.get();
        config.lv.baseSide = 3;
        config.lv.maxBlocksPerSecond = 200;
        config.frameHeight = 4;
        config.frameBlocksPerTick = 8;
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
        helper.setBlock(QUARRY, ModBlocks.QUARRY_LV.defaultBlockState().setValue(QuarryBlock.FACING, Direction.SOUTH));
        helper.setBlock(CHEST, Blocks.CHEST);
        QuarryBlockEntity quarry = helper.getBlockEntity(QUARRY, QuarryBlockEntity.class);
        quarry.energy.amount = quarry.energy.getCapacity();
        quarry.swapBit(bit);
        return quarry;
    }

    private static int stoneLeft(GameTestHelper helper) {
        int count = 0;
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.STONE)) {
                    count++;
                }
            }
        }
        return count;
    }

    @GameTest(maxTicks = 80)
    public void withoutBitBuildsFrameButDoesNotMine(GameTestHelper helper) {
        QuarryBlockEntity quarry = rig(helper, ItemStack.EMPTY);
        helper.runAfterDelay(60, () -> {
            helper.assertBlockPresent(ModBlocks.QUARRY_FRAME, 2, 1, 2);
            helper.assertValueEqual(quarry.status(), QuarryStatus.NO_BIT, "estado");
            helper.assertValueEqual(stoneLeft(helper), 9, "piedra intacta");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void bitWearsOutAndBreaks(GameTestHelper helper) {
        ItemStack bit = new ItemStack(ModItems.bit(BitTier.IRON));
        // Le quedan 4 puntos: la piedra gasta 1 por bloque, así que rompe 4 bloques y se parte con el cuarto
        bit.setDamageValue(bit.getMaxDamage() - 4);
        QuarryBlockEntity quarry = rig(helper, bit);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(quarry.status(), QuarryStatus.NO_BIT, "estado");
            helper.assertTrue(quarry.bitSlot().getItem(0).isEmpty(), "la broca se rompió");
            helper.assertValueEqual(stoneLeft(helper), 5, "piedra que quedó");
        });
    }

    @GameTest(maxTicks = 120)
    public void waterHalvesTheWear(GameTestHelper helper) {
        QuarryBlockEntity quarry = rig(helper, new ItemStack(ModItems.bit(BitTier.IRON)));
        Storage<FluidVariant> port = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(QUARRY), Direction.EAST);
        helper.assertTrue(port != null, "la quarry expone el tanque de agua");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(port.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction), 0L,
                    "no acepta lava");
            helper.assertValueEqual(port.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction),
                    FluidConstants.BUCKET, "acepta agua");
            transaction.commit();
        }
        helper.succeedWhen(() -> {
            helper.assertValueEqual(stoneLeft(helper), 0, "piedra que quedó");
            // Debajo de la capa sigue el mundo plano (tierra): todo lo que se mina es blando, así que cada
            // bloque gasta 1 mB de agua y medio punto de broca
            long used = 1000 - quarry.coolantMb();
            helper.assertTrue(used >= 9, "agua usada: " + used);
            helper.assertValueEqual(quarry.bitSlot().getItem(0).getDamageValue(), (int) (used / 2), "desgaste");
        });
    }

    @GameTest(maxTicks = 120)
    public void oreSeekerOnlyBreaksOres(GameTestHelper helper) {
        QuarryBlockEntity quarry = rig(helper, new ItemStack(ModItems.bit(BitTier.DIAMOND)));
        helper.setBlock(4, 1, 4, Blocks.IRON_ORE);
        quarry.installUpgrade(UpgradeType.ORE_SEEKER);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, 4, 1, 4);
            helper.assertValueEqual(stoneLeft(helper), 8, "la roca queda en su lugar");
            ChestBlockEntity chest = helper.getBlockEntity(CHEST, ChestBlockEntity.class);
            helper.assertValueEqual(chest.countItem(Items.RAW_IRON), 1, "hierro crudo en el cofre");
            helper.assertValueEqual(chest.countItem(Items.COBBLESTONE), 0, "nada de adoquín");
        });
    }
}
