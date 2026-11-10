package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.filter.PipeFilterData;
import org.example.pz.quarryindustrial.filter.PipeFilterHolder;

/** Captura del filtro de caño enganchado a las dos caras de un caño de Ala, entre dos cofres. */
public class PipeFilterClientGameTest implements FabricClientGameTest {
    private static final BlockPos PIPE = new BlockPos(0, 1, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded("alaindustrial")) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                level.setBlockAndUpdate(PIPE.west(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(PIPE.east(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(PIPE, BuiltInRegistries.BLOCK
                        .getValue(Identifier.fromNamespaceAndPath("alaindustrial", "item_pipe")).defaultBlockState());
                BlockEntity pipe = level.getBlockEntity(PIPE);
                try {
                    var cycle = pipe.getClass().getMethod("cycleFaceMode", Direction.class);
                    cycle.invoke(pipe, Direction.WEST);
                    cycle.invoke(pipe, Direction.EAST);
                    cycle.invoke(pipe, Direction.EAST);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
                ((PipeFilterHolder) pipe).quarry$setFilter(Direction.WEST, new PipeFilterData());
                ((PipeFilterHolder) pipe).quarry$setFilter(Direction.EAST, new PipeFilterData());
            });
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("tp @a 0.5 3.0 3.2 180 35");
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pipe_filter_front"));
            world.getServer().runCommand("tp @a -2.6 2.4 1.8 -120 25");
            context.waitTicks(5);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pipe_filter_side"));

            // La GUI sin expansión (un casillero) y con ella (cinco), con algo marcado
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("tp @a 0.5 3.0 3.2 180 35");
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                BlockEntity pipe = server.overworld().getBlockEntity(PIPE);
                PipeFilterData data = ((PipeFilterHolder) pipe).quarry$getFilter(Direction.WEST);
                data.setSample(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
                org.example.pz.quarryindustrial.filter.PipeFilterEvents.open(server.getPlayerList().getPlayers().getFirst(),
                        pipe, Direction.WEST, data);
            });
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pipe_filter_gui_basic"));
            world.getServer().runOnServer(server -> {
                BlockEntity pipe = server.overworld().getBlockEntity(PIPE);
                PipeFilterData data = ((PipeFilterHolder) pipe).quarry$getFilter(Direction.WEST);
                data.setItem(PipeFilterData.UPGRADE_SLOT,
                        new net.minecraft.world.item.ItemStack(org.example.pz.quarryindustrial.registry.ModItems.PIPE_FILTER_UPGRADE));
            });
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pipe_filter_gui_upgrade_only"));
            world.getServer().runOnServer(server -> {
                BlockEntity pipe = server.overworld().getBlockEntity(PIPE);
                PipeFilterData data = ((PipeFilterHolder) pipe).quarry$getFilter(Direction.WEST);
                data.setSample(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT));
                data.setSample(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG));
            });
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pipe_filter_gui_expanded"));
            // Modo categoría con un tronco: el panel de la derecha lista los troncos
            world.getServer().runOnServer(server -> {
                PipeFilterData data = ((PipeFilterHolder) server.overworld().getBlockEntity(PIPE)).quarry$getFilter(Direction.WEST);
                data.tagMatch = true;
                data.setSample(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG));
                data.setChanged();
            });
            context.waitTicks(10);
            Quarryindustrial.LOGGER.info("Captura: {}", context.takeScreenshot("pipe_filter_gui_category"));
        }
    }
}
