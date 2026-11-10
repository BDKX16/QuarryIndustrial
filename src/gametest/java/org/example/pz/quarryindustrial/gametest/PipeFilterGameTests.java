package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.example.pz.quarryindustrial.filter.PipeFilterData;
import org.example.pz.quarryindustrial.filter.PipeFilterHolder;
import org.example.pz.quarryindustrial.registry.ModItems;

/**
 * El filtro de caño de los caños de ítems de Ala: un cofre de origen (oeste) con hierro y oro, un caño en el medio y
 * un cofre de destino (este). Las pruebas se saltean sin Ala.
 */
public class PipeFilterGameTests {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 2);
    private static final BlockPos PIPE = new BlockPos(2, 1, 2);
    private static final BlockPos TARGET = new BlockPos(3, 1, 2);

    private static boolean noAla() {
        return !FabricLoader.getInstance().isModLoaded("alaindustrial");
    }

    /** Arma origen → caño → destino con el caño extrayendo del oeste e insertando al este. */
    private static PipeFilterHolder setUp(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, Blocks.CHEST);
        helper.setBlock(PIPE, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("alaindustrial", "item_pipe")));
        ChestBlockEntity source = helper.getBlockEntity(SOURCE, ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        source.setItem(1, new ItemStack(Items.GOLD_INGOT, 5));
        BlockEntity pipe = helper.getLevel().getBlockEntity(helper.absolutePos(PIPE));
        try {
            var cycle = pipe.getClass().getMethod("cycleFaceMode", Direction.class);
            cycle.invoke(pipe, Direction.WEST); // neutral → extraer
            cycle.invoke(pipe, Direction.EAST); // neutral → extraer
            cycle.invoke(pipe, Direction.EAST); // extraer → insertar
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("API del caño de Ala cambió", e);
        }
        return (PipeFilterHolder) pipe;
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        int total = 0;
        ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(item)) {
                total += chest.getItem(i).getCount();
            }
        }
        return total;
    }

    /** Lista blanca de hierro en la cara que extrae: pasa todo el hierro y nada de oro. */
    @GameTest(maxTicks = 400)
    public void allowListOnlyLetsMarkedItemsOut(GameTestHelper helper) {
        if (noAla()) {
            helper.succeed();
            return;
        }
        PipeFilterHolder pipe = setUp(helper);
        PipeFilterData filter = new PipeFilterData();
        filter.setSample(0, new ItemStack(Items.IRON_INGOT));
        pipe.quarry$setFilter(Direction.WEST, filter);

        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, TARGET, Items.IRON_INGOT) == 5,
                    "el hierro pasó: " + count(helper, TARGET, Items.IRON_INGOT));
            helper.assertTrue(count(helper, TARGET, Items.GOLD_INGOT) == 0, "el oro no pasó");
            helper.assertTrue(count(helper, SOURCE, Items.GOLD_INGOT) == 5, "el oro sigue en el origen");
        });
    }

    /** Lista negra de hierro en la cara que inserta: llega solo el oro. */
    @GameTest(maxTicks = 400)
    public void denyListBlocksMarkedItemsIn(GameTestHelper helper) {
        if (noAla()) {
            helper.succeed();
            return;
        }
        PipeFilterHolder pipe = setUp(helper);
        PipeFilterData filter = new PipeFilterData();
        filter.allow = false;
        filter.setSample(0, new ItemStack(Items.IRON_INGOT));
        pipe.quarry$setFilter(Direction.EAST, filter);

        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, TARGET, Items.GOLD_INGOT) == 5,
                    "el oro pasó: " + count(helper, TARGET, Items.GOLD_INGOT));
            helper.assertTrue(count(helper, TARGET, Items.IRON_INGOT) == 0, "el hierro no entró");
        });
    }

    /** Sin la mejora solo cuenta el primer casillero; con ella, los cinco. */
    @GameTest(maxTicks = 400)
    public void upgradeUnlocksAllCells(GameTestHelper helper) {
        if (noAla()) {
            helper.succeed();
            return;
        }
        PipeFilterHolder pipe = setUp(helper);
        PipeFilterData filter = new PipeFilterData();
        filter.setSample(0, new ItemStack(Items.IRON_INGOT));
        filter.setSample(1, new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(filter.activeCells() == 1, "sin mejora hay un casillero activo");
        pipe.quarry$setFilter(Direction.WEST, filter);
        filter.setItem(PipeFilterData.UPGRADE_SLOT, new ItemStack(ModItems.PIPE_FILTER_UPGRADE));
        helper.assertTrue(filter.activeCells() == 5, "con mejora hay cinco");

        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, TARGET, Items.IRON_INGOT) == 5, "hierro");
            helper.assertTrue(count(helper, TARGET, Items.GOLD_INGOT) == 5, "oro");
        });
    }

    /** Comparar por tag: marcar un lingote de hierro (tag de lingotes de hierro) deja pasar los de su categoría. */
    @GameTest(maxTicks = 20)
    public void tagMatchUsesTheChosenTag(GameTestHelper helper) {
        PipeFilterData filter = new PipeFilterData();
        filter.tagMatch = true;
        filter.setSample(0, new ItemStack(Items.OAK_LOG));
        var tags = PipeFilterData.tagsOf(Items.OAK_LOG);
        helper.assertTrue(!tags.isEmpty(), "el tronco de roble tiene tags");
        // Con cada tag elegido, el propio tronco y los de su misma categoría pasan; un lingote nunca
        for (int i = 0; i < tags.size(); i++) {
            filter.tagIndex[0] = i;
            helper.assertTrue(filter.passes(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.OAK_LOG)),
                    "el tronco pasa por " + tags.get(i).location());
            helper.assertTrue(!filter.passes(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.IRON_INGOT)),
                    "el lingote no pasa por " + tags.get(i).location());
        }
        filter.allow = false;
        helper.assertTrue(!filter.passes(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.OAK_LOG)),
                "en lista negra el tronco no pasa");
        helper.succeed();
    }

    /** Se guarda y se recupera con el caño, y al romperlo el filtro cae como ítem con su configuración. */
    @GameTest(maxTicks = 40)
    public void filterDropsWithItsSettingsWhenThePipeBreaks(GameTestHelper helper) {
        if (noAla()) {
            helper.succeed();
            return;
        }
        PipeFilterHolder pipe = setUp(helper);
        PipeFilterData filter = new PipeFilterData();
        filter.setSample(0, new ItemStack(Items.DIAMOND));
        filter.allow = false;
        pipe.quarry$setFilter(Direction.WEST, filter);

        helper.runAfterDelay(2, () -> helper.destroyBlock(PIPE));
        helper.succeedWhen(() -> {
            BlockPos abs = helper.absolutePos(PIPE);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3),
                    entity -> entity.getItem().is(ModItems.PIPE_FILTER));
            helper.assertTrue(!drops.isEmpty(), "cayó el filtro");
            PipeFilterData saved = drops.getFirst().getItem().get(org.example.pz.quarryindustrial.registry.ModComponents.PIPE_FILTER);
            helper.assertTrue(saved != null && !saved.allow && saved.getItem(0).is(Items.DIAMOND),
                    "el filtro conserva su configuración");
        });
    }
}
