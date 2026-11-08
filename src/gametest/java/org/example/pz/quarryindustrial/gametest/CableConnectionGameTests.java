package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import org.example.pz.quarryindustrial.registry.ModBlocks;

/** Los cables de Ala dibujan brazo hacia nuestros bloques de energía (mixin sobre CableBlock). */
public class CableConnectionGameTests {
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
}
