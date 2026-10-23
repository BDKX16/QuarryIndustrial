package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.NuclearStructure;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;

/**
 * Quarry nuclear de prueba: core en (4, 2, 1) mirando al norte, cubo en x 3..5, y 1..3, z 1..3, con dos
 * puertos de energía arriba. El área (3×3) empieza tres bloques atrás: x 3..5, z 4..6. Cofre adelante del core.
 */
public class NuclearQuarryGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 1);
    private static final BlockPos CHEST = CORE.north();
    private static final BlockPos PORT_A = new BlockPos(3, 3, 2);
    private static final BlockPos PORT_B = new BlockPos(5, 3, 2);

    private static void smallNuclear() {
        QuarryConfig config = QuarryConfig.get();
        config.nuclear.baseSide = 3;
        config.nuclear.maxBlocksPerSecond = 200;
        config.frameHeight = 4;
        config.frameBlocksPerTick = 8;
    }

    /** Arma el cubo; si {@code skip} no es null, deja ese hueco sin carcasa. */
    private static void build(GameTestHelper helper, BlockPos skip) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 4; z <= 6; z++) {
                helper.setBlock(x, 2, z, Blocks.STONE);
            }
        }
        helper.setBlock(CORE, ModBlocks.QUARRY_CORE.defaultBlockState().setValue(QuarryBlock.FACING, Direction.NORTH));
        BlockPos absCore = helper.absolutePos(CORE);
        for (BlockPos abs : NuclearStructure.positions(absCore, Direction.NORTH)) {
            BlockPos rel = abs.subtract(absCore).offset(CORE);
            if (rel.equals(skip)) {
                continue;
            }
            boolean port = rel.equals(PORT_A) || rel.equals(PORT_B);
            helper.setBlock(rel, port ? ModBlocks.ENERGY_PORT : ModBlocks.QUARRY_CASING);
        }
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.getBlockEntity(CORE, QuarryBlockEntity.class).swapBit(new ItemStack(ModItems.bit(BitTier.PDC)));
    }

    private static void fillPorts(GameTestHelper helper) {
        for (BlockPos port : List.of(PORT_A, PORT_B)) {
            EnergyPortBlockEntity be = helper.getBlockEntity(port, EnergyPortBlockEntity.class);
            be.energy.amount = be.energy.getCapacity();
        }
    }

    @GameTest(maxTicks = 160)
    public void twoPortsFeedTheCoreAndItMines(GameTestHelper helper) {
        smallNuclear();
        build(helper, null);

        // Cada puerto acepta como mucho 512 EU/t por la API de energía (lo mismo que empuja un cable HV)
        long accepted = 0;
        try (Transaction transaction = Transaction.openOuter()) {
            for (BlockPos port : List.of(PORT_A, PORT_B)) {
                EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(port), Direction.UP);
                helper.assertTrue(storage != null, "el puerto expone energía");
                accepted += storage.insert(10_000, transaction);
            }
            transaction.abort();
        }
        helper.assertValueEqual(accepted, 1024L, "EU aceptados en un tick por 2 puertos");
        helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(CORE), Direction.NORTH) == null,
                "el core no acepta cables directos");

        fillPorts(helper);
        QuarryBlockEntity core = helper.getBlockEntity(CORE, QuarryBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(core.structureFormed(), "estructura formada");
            helper.assertValueEqual(core.energyPortCount(), 2, "puertos");
            for (int x = 3; x <= 5; x++) {
                for (int z = 4; z <= 6; z++) {
                    helper.assertBlockNotPresent(Blocks.STONE, x, 2, z);
                }
            }
            ChestBlockEntity chest = helper.getBlockEntity(CHEST, ChestBlockEntity.class);
            helper.assertValueEqual(chest.countItem(Items.COBBLESTONE), 9, "cobblestone en el cofre");
        });
    }

    /** Con la broca al rojo (y sin agua para enfriarla) mina, pero frenada. */
    @GameTest(maxTicks = 160)
    public void hotBitThrottles(GameTestHelper helper) {
        smallNuclear();
        build(helper, null);
        fillPorts(helper);
        QuarryBlockEntity core = helper.getBlockEntity(CORE, QuarryBlockEntity.class);
        helper.succeedWhen(() -> {
            core.setHeat(1000);
            helper.assertValueEqual(core.status(), QuarryStatus.OVERHEATING, "estado");
        });
    }

    @GameTest(maxTicks = 60)
    public void missingCasingStopsTheQuarry(GameTestHelper helper) {
        smallNuclear();
        build(helper, new BlockPos(4, 1, 3));
        fillPorts(helper);
        QuarryBlockEntity core = helper.getBlockEntity(CORE, QuarryBlockEntity.class);

        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(core.status(), QuarryStatus.STRUCTURE_INCOMPLETE, "estado");
            helper.assertBlockNotPresent(ModBlocks.QUARRY_FRAME, 2, 2, 3);
            helper.assertBlockPresent(Blocks.STONE, 4, 2, 5);
            helper.succeed();
        });
    }
}
