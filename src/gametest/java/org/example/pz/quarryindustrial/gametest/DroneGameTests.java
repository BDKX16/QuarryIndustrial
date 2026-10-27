package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.OilFluids;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.compat.AlaDistillery;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.drone.DroneNetwork;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.network.ConsolePayloads;
import org.example.pz.quarryindustrial.network.DroneConsoleService;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModComponents;
import org.example.pz.quarryindustrial.registry.ModEntities;
import org.example.pz.quarryindustrial.registry.ModItems;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;
import java.util.Optional;

/**
 * Drones entre dos pads de prueba: A en (1, 1, 1) y B en (6, 1, 6). Para que las pruebas sean cortas los
 * drones vuelan rápido y bajo.
 */
public class DroneGameTests {
    private static final BlockPos PAD_A = new BlockPos(1, 1, 1);
    private static final BlockPos PAD_B = new BlockPos(6, 1, 6);

    private static void fastDrones() {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        config.speed = 1.5;
        config.cruiseAltitude = 4;
        config.launchIdleTicks = 1;
        config.surveyRadius = 12;
        config.surveyScanRadius = 4;
    }

    /** Para los mensajes de error: dónde están los drones de la prueba. */
    private static String drones(GameTestHelper helper) {
        return helper.getLevel().getEntities(ModEntities.DRONE, entity -> true).stream()
                .map(drone -> helper.relativeVec(drone.position()).toString()).toList().toString();
    }

    private static DronePadBlockEntity pad(GameTestHelper helper, BlockPos pos, DroneKind kind) {
        helper.setBlock(pos, ModBlocks.DRONE_PAD);
        DronePadBlockEntity pad = helper.getBlockEntity(pos, DronePadBlockEntity.class);
        pad.energy.amount = pad.energy.getCapacity();
        if (kind != null) {
            pad.insertDrone(new ItemStack(ModItems.drone(kind)));
        }
        return pad;
    }

    private static DronePadBlockEntity linkedPair(GameTestHelper helper, DroneKind kind) {
        fastDrones();
        DronePadBlockEntity source = pad(helper, PAD_A, kind);
        pad(helper, PAD_B, null);
        source.setLink(helper.absolutePos(PAD_B));
        return source;
    }

    /** Un cofre al lado de A con piedra → el dron se la lleva → aparece en el cofre al lado de B. */
    @GameTest(maxTicks = 300, skyAccess = true)
    public void cargoDroneCarriesItemsBetweenChests(GameTestHelper helper) {
        DronePadBlockEntity source = linkedPair(helper, DroneKind.CARGO);
        helper.setBlock(PAD_A.east(), Blocks.CHEST);
        helper.setBlock(PAD_B.west(), Blocks.CHEST);
        helper.getBlockEntity(PAD_A.east(), ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        helper.succeedWhen(() -> {
            ChestBlockEntity destination = helper.getBlockEntity(PAD_B.west(), ChestBlockEntity.class);
            helper.assertValueEqual(destination.countItem(Items.COBBLESTONE), 20, "piedra entregada");
            helper.assertTrue(source.droneDocked(), "el dron volvió al pad: " + source.status());
            helper.assertTrue(source.energy.amount < source.energy.getCapacity(), "el viaje costó energía");
        });
    }

    /** Agua en el buzón de salida de A → llega al buzón de llegada de B, que la deja sacar. */
    @GameTest(maxTicks = 300, skyAccess = true)
    public void tankerDroneCarriesFluid(GameTestHelper helper) {
        DronePadBlockEntity source = linkedPair(helper, DroneKind.TANKER);
        Storage<FluidVariant> in = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(PAD_A), Direction.UP);
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(in.insert(FluidVariant.of(Fluids.WATER), 2 * FluidConstants.BUCKET, transaction),
                    2 * FluidConstants.BUCKET, "agua al pad de origen");
            transaction.commit();
        }
        helper.succeedWhen(() -> {
            Storage<FluidVariant> out = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(PAD_B), Direction.UP);
            long arrived;
            try (Transaction transaction = Transaction.openOuter()) {
                arrived = out.extract(FluidVariant.of(Fluids.WATER), 10 * FluidConstants.BUCKET, transaction);
            }
            helper.assertValueEqual(arrived, 2 * FluidConstants.BUCKET, "agua en el destino");
            helper.assertTrue(source.droneDocked(), "el dron volvió");
        });
    }

    /** La energía del pad A vuela hasta una quarry pegada al pad B. */
    @GameTest(maxTicks = 300, skyAccess = true)
    public void powerDroneFeedsAQuarry(GameTestHelper helper) {
        QuarryConfig.get().drones.powerLaunchEu = 50_000;
        linkedPair(helper, DroneKind.POWER);
        helper.setBlock(PAD_B.west(), ModBlocks.QUARRY_LV.defaultBlockState().setValue(QuarryBlock.FACING, Direction.EAST));
        QuarryBlockEntity quarry = helper.getBlockEntity(PAD_B.west(), QuarryBlockEntity.class);
        helper.succeedWhen(() -> helper.assertTrue(quarry.energy.amount > 1000,
                "la quarry recibió energía por dron: " + quarry.energy.amount));
    }

    /** Una quarry sin broca cerca de un pad con dron de mantenimiento y una broca de repuesto. */
    @GameTest(maxTicks = 600, skyAccess = true)
    public void maintenanceDroneInstallsABit(GameTestHelper helper) {
        fastDrones();
        QuarryConfig config = QuarryConfig.get();
        config.lv.baseSide = 3;
        config.frameBlocksPerTick = 8;
        // Que no vaya a atender las quarries de las otras pruebas
        config.drones.maintenanceRadius = 10;
        DronePadBlockEntity pad = pad(helper, PAD_A, DroneKind.MAINTENANCE);
        Storage<ItemVariant> box = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(PAD_A), Direction.UP);
        try (Transaction transaction = Transaction.openOuter()) {
            box.insert(ItemVariant.of(ModItems.bit(BitTier.DIAMOND)), 1, transaction);
            transaction.commit();
        }
        BlockPos quarryPos = new BlockPos(4, 1, 6);
        helper.setBlock(quarryPos, ModBlocks.QUARRY_LV.defaultBlockState().setValue(QuarryBlock.FACING, Direction.SOUTH));
        QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
        quarry.energy.amount = quarry.energy.getCapacity();
        helper.succeedWhen(() -> {
            helper.assertTrue(quarry.installedBit() == BitTier.DIAMOND, "broca instalada por el dron: " + quarry.installedBit() + " " + quarry.status() + " pad " + pad.status() + " drones " + drones(helper));
            helper.assertTrue(pad.droneDocked(), "el dron volvió");
        });
    }

    /** Una columna de destilación de Ala sucia cerca de un pad con dron de mantenimiento (sin brocas): la limpia. */
    @GameTest(maxTicks = 600, skyAccess = true)
    public void maintenanceDroneCleansADistillationColumn(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return;
        }
        fastDrones();
        // Que no vaya a atender las máquinas de las otras pruebas
        QuarryConfig.get().drones.maintenanceRadius = 10;
        QuarryConfig.get().drones.maintenanceFouling = 50;
        DronePadBlockEntity pad = pad(helper, PAD_A, DroneKind.MAINTENANCE);
        BlockPos columnPos = new BlockPos(4, 1, 6);
        helper.setBlock(columnPos, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(
                "alaindustrial", "distillation_column")).defaultBlockState());
        BlockEntity column = helper.getBlockEntity(columnPos, BlockEntity.class);
        try {
            java.lang.reflect.Field fouling = column.getClass().getDeclaredField("fouling");
            fouling.setAccessible(true);
            fouling.setInt(column, 80);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        helper.assertTrue(AlaDistillery.fouling(column) == 80, "la columna quedó sucia: " + AlaDistillery.fouling(column));
        helper.succeedWhen(() -> {
            helper.assertTrue(AlaDistillery.fouling(column) == 0, "el dron limpió la columna: " + AlaDistillery.fouling(column)
                    + " pad " + pad.status() + " drones " + drones(helper));
            helper.assertTrue(pad.droneDocked(), "el dron volvió");
            helper.assertTrue(pad.peekInbox().is(Items.COAL), "trajo el carbón: " + pad.peekInbox());
        });
    }

    /** La centralita informa el alcance del dron según las mejoras instaladas. */
    @GameTest(maxTicks = 100)
    public void consoleReportsDroneRange(GameTestHelper helper) {
        QuarryConfig.Drones config = QuarryConfig.get().drones;
        config.surveyRadius = 100;
        config.surveyRangeBonus = 50;
        DronePadBlockEntity pad = pad(helper, PAD_A, DroneKind.SURVEY);
        pad.installSurveyUpgrade();
        pad.installSurveyUpgrade();
        BlockPos consolePos = new BlockPos(3, 1, 3);
        helper.setBlock(consolePos, ModBlocks.DRONE_CONSOLE);
        BlockPos console = helper.absolutePos(consolePos);
        BlockPos a = helper.absolutePos(PAD_A);
        helper.succeedWhen(() -> {
            ConsolePayloads.Pad seenPad = seen(DroneConsoleService.snapshot(helper.getLevel(), console), a)
                    .orElseThrow(() -> helper.assertionException(Component.literal("la centralita no ve el pad")));
            helper.assertValueEqual(seenPad.range(), 200, "alcance del explorador con 2 mejoras");
        });
    }

    /** Un charco de petróleo en la superficie: el explorador lo ve y vuelve con el informe. */
    @GameTest(maxTicks = 600, skyAccess = true)
    public void surveyDroneFindsAGeyser(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            helper.succeed();
            return;
        }
        fastDrones();
        DronePadBlockEntity pad = pad(helper, PAD_A, DroneKind.SURVEY);
        BlockPos oil = new BlockPos(5, 1, 3);
        helper.setBlock(oil, OilFluids.sourceFluid().defaultFluidState().createLegacyBlock());
        pad.requestSurvey();
        BlockPos expected = helper.absolutePos(oil);
        helper.succeedWhen(() -> {
            helper.assertTrue(pad.droneDocked(), "el explorador volvió: " + pad.status() + " drones " + drones(helper));
            ItemStack report = pad.peekInbox();
            helper.assertTrue(report.is(ModItems.SURVEY_REPORT), "hay informe en el buzón: " + report);
            List<BlockPos> sites = report.getOrDefault(ModComponents.SURVEY_SITES, List.of());
            helper.assertTrue(sites.contains(expected), "el informe marca el géiser " + expected + ": " + sites);
            helper.assertTrue(DroneNetwork.get(helper.getLevel()).sites().stream()
                    .anyMatch(site -> site.pos().equals(expected)), "la centralita ve el géiser en el mapa");
        });
    }

    private static Optional<ConsolePayloads.Pad> seen(ConsolePayloads.Snapshot snapshot, BlockPos pos) {
        return snapshot.pads().stream().filter(pad -> pad.pos().equals(pos)).findFirst();
    }

    /** La centralita ve los pads de la zona con su dron y su vínculo; al romper un pad, lo deja de ver. */
    @GameTest(maxTicks = 100)
    public void consoleSeesPadsAndLinks(GameTestHelper helper) {
        linkedPair(helper, DroneKind.CARGO);
        BlockPos consolePos = new BlockPos(3, 1, 3);
        helper.setBlock(consolePos, ModBlocks.DRONE_CONSOLE);
        BlockPos console = helper.absolutePos(consolePos);
        BlockPos a = helper.absolutePos(PAD_A);
        BlockPos b = helper.absolutePos(PAD_B);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ConsolePayloads.Snapshot snapshot = DroneConsoleService.snapshot(helper.getLevel(), console);
                    ConsolePayloads.Pad source = seen(snapshot, a).orElseThrow(() -> helper.assertionException(
                            Component.literal("la centralita no ve el pad A: " + snapshot.pads())));
                    helper.assertValueEqual(source.kind(), DroneKind.CARGO.ordinal(), "dron del pad A");
                    helper.assertValueEqual(source.link(), Optional.of(b), "vínculo de A");
                    helper.assertTrue(seen(snapshot, b).isPresent(), "la centralita ve el pad B");
                })
                .thenExecute(() -> helper.destroyBlock(PAD_B))
                .thenWaitUntil(() -> helper.assertTrue(
                        seen(DroneConsoleService.snapshot(helper.getLevel(), console), b).isEmpty(),
                        "el pad roto desaparece de la centralita"))
                .thenSucceed();
    }
}
