package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import org.example.pz.quarryindustrial.block.pumpjack.OilFluids;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.example.pz.quarryindustrial.registry.ModBlocks;

/**
 * El petróleo de la cigüeña tiene que salir por los caños de Ala Industrial hasta un tanque de Ala.
 * Cigüeña en (4, 1, 0) mirando al norte (la base ocupa x 3..5, z 0..6); un caño pegado a la base en (6, 1, 2)
 * y un tanque de Ala en (7, 1, 2).
 */
public class AlaPipeGameTests {
    private static final BlockPos WELLHEAD = new BlockPos(4, 1, 0);
    private static final BlockPos PIPE = new BlockPos(6, 1, 2);
    private static final BlockPos TANK = new BlockPos(7, 1, 2);

    @GameTest(maxTicks = 200, skyAccess = true)
    public void oilFlowsIntoAlaPipes(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return;
        }
        helper.setBlock(WELLHEAD, ModBlocks.PUMPJACK.defaultBlockState().setValue(PumpjackBlock.FACING, Direction.NORTH));
        PumpjackBlock.build(helper.getLevel(), helper.absolutePos(WELLHEAD), Direction.NORTH);
        PumpjackBlockEntity pumpjack = helper.getBlockEntity(WELLHEAD, PumpjackBlockEntity.class);
        pumpjack.tank.variant = FluidVariant.of(OilFluids.sourceFluid());
        pumpjack.tank.amount = 8 * FluidConstants.BUCKET;

        helper.setBlock(PIPE, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("alaindustrial", "fluid_pipe")));
        helper.setBlock(TANK, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("alaindustrial", "fluid_tank")));
        helper.setBlock(TANK.above(), Blocks.AIR);
        // El tanque de Ala es de doble sentido: como en el juego, la cara del caño hacia él se pone en
        // "insertar" con la llave (neutral → extraer → insertar). La cigüeña, en cambio, no necesita llave.
        Object pipe = helper.getLevel().getBlockEntity(helper.absolutePos(PIPE));
        try {
            var cycle = pipe.getClass().getMethod("cycleFaceMode", Direction.class);
            cycle.invoke(pipe, Direction.EAST);
            cycle.invoke(pipe, Direction.EAST);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("API del caño de Ala cambió", e);
        }

        helper.succeedWhen(() -> {
            Storage<FluidVariant> tank = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(TANK), Direction.UP);
            long stored = 0;
            if (tank != null) {
                for (var view : tank) {
                    stored += view.getAmount();
                }
            }
            // Más que el buffer de un caño (50 mB): el oil tiene que haber circulado
            helper.assertTrue(stored > FluidConstants.BUCKET, "oil en el tanque de Ala: " + stored
                    + " (cigüeña: " + pumpjack.oilMb() + " mB)");
        });
    }
}
