package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import team.reborn.energy.api.EnergyStorage;

/** Los cables de Ala dibujan brazo hacia nuestros bloques de energía (mixin sobre CableBlock). */
public class CableConnectionGameTests {
    /** Distribuidor con la entrada hacia el oeste. */
    private static net.minecraft.world.level.block.state.BlockState west(
            org.example.pz.quarryindustrial.block.splitter.EnergySplitterBlock block) {
        return block.defaultBlockState()
                .setValue(org.example.pz.quarryindustrial.block.splitter.EnergySplitterBlock.FACING, Direction.WEST);
    }

    @GameTest(maxTicks = 20)
    public void alaCablesConnectToOurBlocks(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return;
        }
        Block cable = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("alaindustrial", "insulated_electrum_cable"));
        Block[] targets = {ModBlocks.DRONE_PAD, ModBlocks.QUARRY_HV, ModBlocks.ENERGY_SPLITTER, ModBlocks.ENERGY_PORT};
        for (int i = 0; i < targets.length; i++) {
            BlockPos cablePos = new BlockPos(1 + i * 2, 1, 1);
            helper.setBlock(cablePos, cable);
            helper.setBlock(cablePos.east(), targets[i]);
        }
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < targets.length; i++) {
                BlockPos cablePos = new BlockPos(1 + i * 2, 1, 1);
                helper.assertTrue(helper.getBlockState(cablePos).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)),
                        "el cable no se conectó a " + BuiltInRegistries.BLOCK.getKey(targets[i]));
            }
            helper.succeed();
        });
    }

    /** Cadena: fuente → splitter A → splitter B → splitter C → pad. La energía tiene que llegar hasta el pad. */
    @GameTest(maxTicks = 100)
    public void splittersPassEnergyAlongTheChain(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = a.east();
        BlockPos c = b.east();
        BlockPos pad = c.east();
        helper.setBlock(a, west(ModBlocks.ENERGY_SPLITTER));
        helper.setBlock(b, west(ModBlocks.ENERGY_SPLITTER));
        helper.setBlock(c, west(ModBlocks.ENERGY_SPLITTER));
        helper.setBlock(pad, ModBlocks.DRONE_PAD);
        DronePadBlockEntity padEntity = helper.getBlockEntity(pad, DronePadBlockEntity.class);
        padEntity.energy.amount = 0;
        helper.runAtTickTime(2, () -> {
            // Como un cable: entra por la cara oeste de A, todos los ticks
            var source = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(a), Direction.WEST);
            helper.assertTrue(source != null, "el splitter A no expone energía por el oeste");
        });
        for (int tick = 2; tick < 60; tick++) {
            helper.runAtTickTime(tick, () -> {
                var source = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(a), Direction.WEST);
                try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction tx =
                             net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    source.insert(512, tx);
                    tx.commit();
                }
            });
        }
        helper.succeedWhen(() -> helper.assertTrue(padEntity.energy.amount > 0,
                "el pad no recibió energía: A=" + helper.getBlockEntity(a,
                        org.example.pz.quarryindustrial.block.splitter.EnergySplitterBlockEntity.class).energy.amount
                        + " B=" + helper.getBlockEntity(b,
                        org.example.pz.quarryindustrial.block.splitter.EnergySplitterBlockEntity.class).energy.amount
                        + " C=" + helper.getBlockEntity(c,
                        org.example.pz.quarryindustrial.block.splitter.EnergySplitterBlockEntity.class).energy.amount));
    }

    /** Tu montaje: A (2 pads) → B (2 pads) → C (2 pads), con o sin un cable de Ala entre cada par. */
    private void treeLayout(GameTestHelper helper, boolean withCables) {
        Block cable = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("alaindustrial", "insulated_electrum_cable"));
        int gap = withCables ? 2 : 1;
        BlockPos a = new BlockPos(1, 1, 3);
        BlockPos b = a.east(gap);
        BlockPos c = b.east(gap);
        BlockPos[] splitters = {a, b, c};
        java.util.List<DronePadBlockEntity> pads = new java.util.ArrayList<>();
        for (BlockPos splitter : splitters) {
            helper.setBlock(splitter, west(ModBlocks.ENERGY_SPLITTER));
            for (BlockPos padPos : new BlockPos[]{splitter.north(), splitter.south()}) {
                helper.setBlock(padPos, ModBlocks.DRONE_PAD);
                DronePadBlockEntity pad = helper.getBlockEntity(padPos, DronePadBlockEntity.class);
                pad.energy.amount = 0;
                pads.add(pad);
            }
        }
        if (withCables) {
            helper.setBlock(a.east(), cable);
            helper.setBlock(b.east(), cable);
        }
        for (int tick = 2; tick < 120; tick++) {
            helper.runAtTickTime(tick, () -> {
                var source = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(a), Direction.WEST);
                try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction tx =
                             net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    source.insert(512, tx);
                    tx.commit();
                }
            });
        }
        helper.runAtTickTime(119, () -> {
            StringBuilder report = new StringBuilder("pads (A,A,B,B,C,C): ");
            boolean all = true;
            for (DronePadBlockEntity pad : pads) {
                report.append(pad.energy.amount).append(' ');
                all &= pad.energy.amount > 0;
            }
            if (!all) {
                helper.fail(Component.literal(report.toString()));
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(maxTicks = 130)
    public void splitterTreeDirect(GameTestHelper helper) {
        treeLayout(helper, false);
    }

    @GameTest(maxTicks = 130)
    public void splitterTreeWithCables(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return;
        }
        treeLayout(helper, true);
    }
}
