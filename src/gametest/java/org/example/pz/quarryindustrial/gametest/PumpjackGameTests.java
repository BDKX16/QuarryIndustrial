package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackLayout;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.example.pz.quarryindustrial.block.pumpjack.OilFluids;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.registry.ModBlocks;

/**
 * Un pozo de géiser en miniatura (el de verdad mide más de 30 bloques y no entra en una estructura de
 * prueba): roca hasta y=4, un caño de 1×1 en (4, 3..4, 1) y una cavidad en x 2..6, y 1..2, z 0..3, con
 * oil real de Ala Industrial adentro. La cabeza de pozo va en (4, 5, 1) mirando al norte, así la cigüeña
 * (3×7×5) se arma hacia el sur dentro de la estructura.
 */
public class PumpjackGameTests {
    private static final BlockPos WELLHEAD = new BlockPos(4, 5, 1);

    private static void tinyWellThresholds() {
        QuarryConfig.Pumpjack config = QuarryConfig.get().pumpjack;
        config.minShaftLength = 1;
        config.minCavityRadius = 1;
        config.maxCavityCenterY = 10_000;
        config.drainTicksPerBucket = 1;
    }

    private static void placeRock(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                for (int y = 0; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.STONE);
                }
            }
        }
    }

    private static void carveWell(GameTestHelper helper, boolean withCavity) {
        helper.setBlock(4, 4, 1, Blocks.AIR);
        helper.setBlock(4, 3, 1, Blocks.AIR);
        if (withCavity) {
            for (int x = 2; x <= 6; x++) {
                for (int z = 0; z <= 3; z++) {
                    helper.setBlock(x, 1, z, Blocks.AIR);
                    helper.setBlock(x, 2, z, Blocks.AIR);
                }
            }
        }
    }

    private static PumpjackBlockEntity placePumpjack(GameTestHelper helper) {
        helper.setBlock(WELLHEAD, ModBlocks.PUMPJACK.defaultBlockState().setValue(PumpjackBlock.FACING, Direction.NORTH));
        PumpjackBlock.build(helper.getLevel(), helper.absolutePos(WELLHEAD), Direction.NORTH);
        PumpjackBlockEntity pumpjack = helper.getBlockEntity(WELLHEAD, PumpjackBlockEntity.class);
        pumpjack.energy.amount = pumpjack.energy.getCapacity();
        return pumpjack;
    }

    @GameTest(maxTicks = 200, skyAccess = true)
    public void drainsReservoirThenPumpsForever(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            // Sin Ala no hay fluido en #c:oil: no hay nada que vaciar (la cigüeña queda en NO_OIL_FLUID)
            helper.succeed();
            return;
        }
        tinyWellThresholds();
        Fluid oil = OilFluids.sourceFluid();
        helper.assertTrue(oil != null, "Ala Industrial tiene que registrar un fluido en #c:oil");
        placeRock(helper);
        carveWell(helper, true);
        BlockState oilBlock = oil.defaultFluidState().createLegacyBlock();
        BlockPos[] sources = {new BlockPos(2, 1, 0), new BlockPos(6, 1, 3), new BlockPos(3, 2, 2)};
        for (BlockPos source : sources) {
            helper.setBlock(source, oilBlock);
        }
        PumpjackBlockEntity pumpjack = placePumpjack(helper);

        helper.succeedWhen(() -> {
            for (BlockPos source : sources) {
                helper.assertTrue(!OilFluids.isOilSource(helper.getBlockState(source)), "fuente vaciada " + source);
            }
            helper.assertTrue(pumpjack.domeDrained(), "domo marcado como vacío");
            helper.assertValueEqual(pumpjack.status(), PumpjackStatus.PUMPING, "estado");
            // 3 cubos del vaciado + algo del bombeo infinito
            helper.assertTrue(pumpjack.oilMb() > 3000, "oil en el tanque: " + pumpjack.oilMb());
        });
    }

    /**
     * Sin un EU en el buffer pero con diésel en la fila del motor: la cigüeña bombea igual, quemando
     * combustible. El frente de la base sigue siendo solo salida de oil.
     */
    @GameTest(maxTicks = 200, skyAccess = true)
    public void runsOnDieselWithoutPower(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return;
        }
        tinyWellThresholds();
        placeRock(helper);
        carveWell(helper, true);
        PumpjackBlockEntity pumpjack = placePumpjack(helper);
        pumpjack.energy.amount = 0;
        BlockPos head = helper.absolutePos(WELLHEAD);
        Storage<FluidVariant> motor = FluidStorage.SIDED.find(helper.getLevel(),
                PumpjackLayout.toWorld(head, Direction.NORTH, 0, 0, PumpjackLayout.LENGTH - 1), Direction.UP);
        Storage<FluidVariant> front = FluidStorage.SIDED.find(helper.getLevel(),
                PumpjackLayout.toWorld(head, Direction.NORTH, 1, 0, 2), Direction.UP);
        Fluid diesel = BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("alaindustrial", "diesel"));
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(front.insert(FluidVariant.of(diesel), FluidConstants.BUCKET, transaction), 0L,
                    "el frente no toma diésel");
            helper.assertValueEqual(motor.insert(FluidVariant.of(diesel), 4 * FluidConstants.BUCKET, transaction),
                    4 * FluidConstants.BUCKET, "el motor toma diésel");
            transaction.commit();
        }
        helper.assertTrue(!motor.supportsExtraction(), "del motor no se saca nada");

        helper.succeedWhen(() -> {
            helper.assertValueEqual(pumpjack.status(), PumpjackStatus.PUMPING, "estado");
            helper.assertTrue(pumpjack.onDiesel(), "anda a diésel");
            helper.assertTrue(pumpjack.fuel.amountMb() < 4000, "quemó diésel: " + pumpjack.fuel.amountMb());
            helper.assertTrue(pumpjack.oilMb() > 100, "bombeó oil: " + pumpjack.oilMb());
        });
    }

    @GameTest(maxTicks = 40, skyAccess = true)
    public void plainHoleIsNotAWell(GameTestHelper helper) {
        tinyWellThresholds();
        placeRock(helper);
        carveWell(helper, false);
        PumpjackBlockEntity pumpjack = placePumpjack(helper);

        helper.runAfterDelay(10, () -> {
            // Sin Ala, lo primero que falta es el fluido; con Ala, el pozo
            PumpjackStatus expected = FabricLoader.getInstance().isModLoaded("alaindustrial")
                    ? PumpjackStatus.NO_WELL : PumpjackStatus.NO_OIL_FLUID;
            helper.assertValueEqual(pumpjack.status(), expected, "estado");
            helper.assertValueEqual(pumpjack.oilMb(), 0L, "oil");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 20, skyAccess = true)
    public void breakingAPartDismantlesEverything(GameTestHelper helper) {
        placeRock(helper);
        placePumpjack(helper);
        helper.assertBlockPresent(ModBlocks.PUMPJACK_PART, 3, 9, 7);

        helper.destroyBlock(new BlockPos(5, 5, 4));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ModBlocks.PUMPJACK, WELLHEAD);
            helper.assertBlockNotPresent(ModBlocks.PUMPJACK_PART, 3, 9, 7);
            helper.assertBlockNotPresent(ModBlocks.PUMPJACK_PART, 4, 5, 2);
            helper.assertItemEntityPresent(ModBlocks.PUMPJACK.asItem());
            helper.succeed();
        });
    }
}
