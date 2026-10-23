package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import org.example.pz.quarryindustrial.block.generator.DieselGeneratorBlockEntity;
import org.example.pz.quarryindustrial.block.generator.SteamTurbineBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.registry.ModBlocks;

/** Generador diésel, turbina de vapor con la Steam Nozzle de Ala (la cigüeña diésel está en PumpjackGameTests). */
public class GeneratorGameTests {
    private static Fluid ala(String name) {
        return BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("alaindustrial", name));
    }

    private static boolean noAla(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return true;
        }
        return false;
    }

    private static long insert(Storage<FluidVariant> storage, Fluid fluid, long amount) {
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(FluidVariant.of(fluid), amount, transaction);
            transaction.commit();
            return inserted;
        }
    }

    /** Diésel por caño → el generador lo quema y le empuja la energía a la quarry pegada. */
    @GameTest(maxTicks = 100)
    public void dieselGeneratorPowersANeighbour(GameTestHelper helper) {
        if (noAla(helper)) {
            return;
        }
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos quarryPos = new BlockPos(2, 1, 1);
        helper.setBlock(generatorPos, ModBlocks.DIESEL_GENERATOR);
        helper.setBlock(quarryPos, ModBlocks.QUARRY_LV.defaultBlockState().setValue(QuarryBlock.FACING, Direction.WEST));
        Storage<FluidVariant> input = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(generatorPos), Direction.UP);
        helper.assertTrue(input != null, "el generador expone su tanque");
        helper.assertValueEqual(insert(input, ala("oil"), FluidConstants.BUCKET), 0L, "no acepta crudo");
        helper.assertValueEqual(insert(input, ala("diesel"), FluidConstants.BUCKET), FluidConstants.BUCKET,
                "acepta diésel");
        DieselGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos, DieselGeneratorBlockEntity.class);
        QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(quarry.energy.amount > 1000, "la quarry recibió energía: " + quarry.energy.amount);
            helper.assertTrue(generator.fuel.amountMb() < 1000, "quemó diésel");
            helper.assertTrue(generator.running(), "está encendido");
        });
    }

    /**
     * Vapor dentro de la Steam Nozzle de Ala (como si viniera del reactor): la turbina pegada se lo saca antes
     * de que la nozzle lo ventee y genera EU.
     */
    @GameTest(maxTicks = 100)
    public void turbineTakesSteamFromAlaNozzle(GameTestHelper helper) {
        if (noAla(helper)) {
            return;
        }
        BlockPos nozzlePos = new BlockPos(1, 1, 1);
        // La boca de la nozzle (su "facing") apunta a la turbina: así la tapa y deja de ventear
        BlockState nozzleState = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("alaindustrial", "steam_nozzle")).defaultBlockState();
        Direction mouth = Direction.NORTH;
        for (Property<?> property : nozzleState.getProperties()) {
            if (property.getName().equals("facing") && nozzleState.getValue(property) instanceof Direction current) {
                mouth = current;
            }
        }
        BlockPos turbinePos = nozzlePos.relative(mouth);
        helper.setBlock(nozzlePos, nozzleState);
        helper.setBlock(turbinePos, ModBlocks.STEAM_TURBINE);
        SteamTurbineBlockEntity turbine = helper.getBlockEntity(turbinePos, SteamTurbineBlockEntity.class);
        helper.succeedWhen(() -> {
            // Se rellena cada tick: la nozzle ventea sola lo que no le sacan
            for (Direction side : Direction.values()) {
                Storage<FluidVariant> nozzle = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(nozzlePos), side);
                if (nozzle != null && insert(nozzle, ala("steam"), FluidConstants.BUCKET) > 0) {
                    break;
                }
            }
            helper.assertTrue(turbine.energy.amount > 0 || turbine.averageOutput() > 0,
                    "la turbina generó: " + turbine.energy.amount + " EU, vapor " + turbine.steamMb() + " mB");
        });
    }
}
