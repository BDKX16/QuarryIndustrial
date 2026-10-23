package org.example.pz.quarryindustrial.gametest;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.block.generator.DieselGeneratorBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreLayout;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlock;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.NuclearStructure;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlockEntity;
import org.example.pz.quarryindustrial.client.manual.ManualScreen;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.drone.DroneEntity;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModEntities;
import org.example.pz.quarryindustrial.registry.ModItems;

import java.nio.file.Path;
import java.util.List;

import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.CHUNK_TIMEOUT;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.command;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.findSite;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.prepareSite;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.clearAbove;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.shot;
import static org.example.pz.quarryindustrial.gametest.ShowcaseClientGameTest.surface;

/**
 * Capturas de la expansión en un mundo real (misma semilla que el showcase): desgaste y tolva de la broca,
 * la PDC al rojo, generadores, drones (en el pad y en vuelo), la plataforma offshore en un océano de verdad y
 * el manual. Solo corre con {@code QUARRY_EXPANSION=1}; deja las capturas en {@code QUARRY_SHOWCASE_DIR}.
 */
public class ExpansionShowcaseClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("QUARRY_EXPANSION") == null) {
            return;
        }
        Path output = Path.of(System.getenv().getOrDefault("QUARRY_SHOWCASE_DIR", "screenshots-showcase"))
                .toAbsolutePath();
        QuarryConfig config = QuarryConfig.get();
        config.hv.maxBlocksPerSecond = 400;
        config.nuclear.baseSide = 16;
        config.nuclear.maxBlocksPerSecond = 500;
        config.frameBlocksPerTick = 32;
        config.maxSkipsPerTick = 2048;
        config.offshore.drillDepth = 6;
        config.offshore.drillTicksPerMeter = 10;
        config.drones.speed = 0.35;
        config.drones.launchIdleTicks = 20;

        context.runOnClient(minecraft -> {
            minecraft.options.renderDistance().set(8);
            minecraft.options.fov().set(70);
        });

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(settings -> {
                    settings.setWorldType(settings.getNormalPresetList().getFirst());
                    settings.setSeed(ShowcaseClientGameTest.SEED);
                    settings.setGenerateStructures(false);
                })
                .create()) {
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule advance_weather false");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("time set 1500");
            world.getServer().runCommand("gamemode spectator @a");

            BlockPos plains = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
                        biome -> biome.is(Biomes.PLAINS), level.getRespawnData().pos(), 6400, 32, 64);
                return found != null ? found.getFirst() : level.getRespawnData().pos();
            });

            // ── Quarry HV con broca de diamante gastada, agua y la tolva ──
            BlockPos hvArea = findSite(world, context, plains, 40);
            BlockPos hv = surface(world, context, hvArea.offset(0, 0, 18));
            clearAbove(world, hvArea, 20, hv.getY());
            prepareSite(world, hv, 6);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(hv, ModBlocks.QUARRY_HV.defaultBlockState().setValue(QuarryBlock.FACING, Direction.SOUTH));
                QuarryBlockEntity quarry = (QuarryBlockEntity) level.getBlockEntity(hv);
                ItemStack bit = new ItemStack(ModItems.bit(BitTier.DIAMOND));
                bit.setDamageValue(bit.getMaxDamage() * 6 / 10);
                quarry.swapBit(bit);
                quarry.installUpgrade(UpgradeType.SPEED);
            });
            command(world, "setblock %d %d %d minecraft:chest", hv.getX(), hv.getY() + 1, hv.getZ());

            // ── Quarry nuclear sin agua, al rojo ──
            BlockPos coreArea = findSite(world, context, hvArea.offset(80, 0, 0), 24);
            BlockPos core = surface(world, context, coreArea.offset(0, 0, 12));
            clearAbove(world, coreArea, 12, core.getY());
            prepareSite(world, core, 5);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(core, ModBlocks.QUARRY_CORE.defaultBlockState().setValue(QuarryBlock.FACING, Direction.SOUTH));
                for (BlockPos pos : NuclearStructure.positions(core, Direction.SOUTH)) {
                    boolean port = pos.getY() == core.getY() + 1 && !pos.equals(core.above());
                    level.setBlockAndUpdate(pos, (port ? ModBlocks.ENERGY_PORT : ModBlocks.QUARRY_CASING).defaultBlockState());
                }
                QuarryBlockEntity nuclear = (QuarryBlockEntity) level.getBlockEntity(core);
                nuclear.swapBit(new ItemStack(ModItems.bit(BitTier.PDC)));
                nuclear.installUpgrade(UpgradeType.VOID);
            });

            // ── Generadores: diésel y turbina sobre una Steam Nozzle de Ala ──
            BlockPos gen = surface(world, context, findSite(world, context, hvArea.offset(0, 0, 70), 12));
            prepareSite(world, gen, 5);
            Direction mouth = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                level.setBlockAndUpdate(gen, ModBlocks.DIESEL_GENERATOR.defaultBlockState());
                BlockState nozzle = BuiltInRegistries.BLOCK.getValue(
                        Identifier.fromNamespaceAndPath("alaindustrial", "steam_nozzle")).defaultBlockState();
                Direction facing = Direction.NORTH;
                for (Property<?> property : nozzle.getProperties()) {
                    if (property.getName().equals("facing") && nozzle.getValue(property) instanceof Direction d) {
                        facing = d;
                    }
                }
                level.setBlockAndUpdate(gen.east(3), nozzle);
                level.setBlockAndUpdate(gen.east(3).relative(facing), ModBlocks.STEAM_TURBINE.defaultBlockState());
                return facing;
            });

            // ── Drones: una fila de pads con los cinco tipos, y un dron de carga entre dos pads ──
            BlockPos pads = surface(world, context, findSite(world, context, hvArea.offset(80, 0, 70), 30));
            prepareSite(world, pads, 12);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.overworld();
                DroneKind[] kinds = DroneKind.values();
                for (int i = 0; i < kinds.length; i++) {
                    BlockPos pad = pads.offset(i * 2 - 4, 0, 0);
                    level.setBlockAndUpdate(pad, ModBlocks.DRONE_PAD.defaultBlockState());
                    DronePadBlockEntity be = (DronePadBlockEntity) level.getBlockEntity(pad);
                    be.insertDrone(new ItemStack(ModItems.drone(kinds[i])));
                }
                BlockPos from = pads.offset(-6, 0, 6);
                BlockPos to = pads.offset(6, 0, 6);
                level.setBlockAndUpdate(from, ModBlocks.DRONE_PAD.defaultBlockState());
                level.setBlockAndUpdate(to, ModBlocks.DRONE_PAD.defaultBlockState());
                DronePadBlockEntity source = (DronePadBlockEntity) level.getBlockEntity(from);
                source.insertDrone(new ItemStack(ModItems.drone(DroneKind.CARGO)));
                source.setLink(to);
                source.energy.amount = source.energy.getCapacity();
                level.setBlockAndUpdate(from.west(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(to.east(), Blocks.CHEST.defaultBlockState());
            });

            // ── Plataforma offshore en el océano profundo más cercano ──
            BlockPos ocean = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
                        biome -> biome.is(BiomeTags.IS_DEEP_OCEAN), plains, 6400, 32, 64);
                return found != null ? found.getFirst() : plains;
            });
            surface(world, context, ocean);
            BlockPos rig = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                BlockPos pos = new BlockPos(ocean.getX(), level.getSeaLevel(), ocean.getZ());
                Quarryindustrial.LOGGER.info("Expansión: plataforma en {} ({})", pos, OffshoreLayout.check(level, pos));
                level.setBlockAndUpdate(pos, ModBlocks.OFFSHORE_RIG.defaultBlockState());
                OffshoreRigBlock.build(level, pos);
                OffshoreRigBlockEntity be = (OffshoreRigBlockEntity) level.getBlockEntity(pos);
                be.swapBit(new ItemStack(ModItems.bit(BitTier.DIAMOND)));
                be.installSpeedUpgrade();
                return pos;
            });

            // Que trabajen un rato
            for (int i = 0; i < 40; i++) {
                feed(world, hv, core, gen, pads, rig);
                context.waitTicks(20);
            }
            config.hv.maxBlocksPerSecond = 2;
            config.nuclear.maxBlocksPerSecond = 3;
            feed(world, hv, core, gen, pads, rig);
            context.waitTicks(60);

            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitTicks(2);

            BlockPos hvHead = ShowcaseClientGameTest.head(world, hv, 0);
            shot(world, context, output, "10_bit_wear", hvHead.offset(2, 1, 2), hvHead.offset(0, 1, 0));
            shot(world, context, output, "11_hopper", hvHead.atY(hv.getY() + 5).offset(3, 2, 3), hvHead.atY(hv.getY() + 4));
            BlockPos nuclearHead = ShowcaseClientGameTest.head(world, core, 1);
            feed(world, hv, core, gen, pads, rig);
            shot(world, context, output, "12_hot_pdc", nuclearHead.offset(2, 1, 2), nuclearHead.offset(0, 1, 0));
            shot(world, context, output, "13_generators", gen.offset(-3, 3, 5), gen.offset(2, 0, 0));
            shot(world, context, output, "14_drone_pads", pads.offset(0, 3, -6), pads);

            // El dron de carga: con piedra en el cofre sale enseguida
            world.getServer().runOnServer(server -> ((ChestBlockEntity) server.overworld()
                    .getBlockEntity(pads.offset(-7, 0, 6))).setItem(0, new ItemStack(Items.COBBLESTONE, 64)));
            Vec3 drone = null;
            for (int i = 0; i < 40 && drone == null; i++) {
                context.waitTicks(5);
                drone = world.getServer().computeOnServer(server -> {
                    List<? extends DroneEntity> flying = server.overworld().getEntities(ModEntities.DRONE,
                            entity -> entity.getY() > pads.getY() + 4);
                    return flying.isEmpty() ? null : flying.getFirst().position();
                });
            }
            if (drone != null) {
                BlockPos at = BlockPos.containing(drone);
                shot(world, context, output, "15_drone_flight", at.offset(3, 1, -4), at);
            }

            shot(world, context, output, "16_offshore", rig.offset(-22, 12, 18), rig.offset(0, 4, 0));
            shot(world, context, output, "17_offshore_deck", rig.offset(-6, 4, 6), rig.offset(0, 4, 0));
            shot(world, context, output, "18_offshore_under", rig.offset(-6, -4, 5), rig.offset(0, -8, 0));

            // El manual, con el HUD
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.setScreen(ManualScreen::new);
            context.waitTicks(10);
            context.takeScreenshot(TestScreenshotOptions.of("19_manual_raw").withDestinationDir(output)
                    .disableCounterPrefix());
            context.setScreen(() -> null);
            Quarryindustrial.LOGGER.info("Expansión: boca de la nozzle {}", mouth);
        }
    }

    /** Energía para todo, y vapor para la nozzle y diésel para el generador. */
    private static void feed(TestSingleplayerContext world, BlockPos hv, BlockPos core, BlockPos gen, BlockPos pads,
                             BlockPos rig) {
        world.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            if (level.getBlockEntity(hv) instanceof QuarryBlockEntity quarry) {
                quarry.energy.amount = quarry.energy.getCapacity();
                insert(level, hv, Fluids.WATER, 2);
            }
            for (BlockPos pos : NuclearStructure.positions(core, Direction.SOUTH)) {
                if (level.getBlockEntity(pos) instanceof EnergyPortBlockEntity port) {
                    port.energy.amount = port.energy.getCapacity();
                }
            }
            if (level.getBlockEntity(core) instanceof QuarryBlockEntity nuclear) {
                nuclear.energy.amount = nuclear.energy.getCapacity();
                nuclear.setHeat(1000);
            }
            if (level.getBlockEntity(gen) instanceof DieselGeneratorBlockEntity) {
                insert(level, gen, ala("diesel"), 1);
            }
            insert(level, gen.east(3), ala("steam"), 1);
            if (level.getBlockEntity(rig) instanceof OffshoreRigBlockEntity offshore) {
                offshore.energy.amount = offshore.energy.getCapacity();
            }
        });
    }

    private static Fluid ala(String name) {
        return BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("alaindustrial", name));
    }

    private static void insert(ServerLevel level, BlockPos pos, Fluid fluid, int buckets) {
        for (Direction side : Direction.values()) {
            Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
            if (storage == null || !storage.supportsInsertion()) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                if (storage.insert(FluidVariant.of(fluid), buckets * FluidConstants.BUCKET, transaction) > 0) {
                    transaction.commit();
                    return;
                }
            }
        }
    }
}
