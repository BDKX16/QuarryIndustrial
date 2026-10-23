package org.example.pz.quarryindustrial.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeManager;
import org.example.pz.quarryindustrial.Quarryindustrial;

import java.util.List;

/**
 * Cada item tiene dos recetas: la de Ala (si Ala está cargado) y la vanilla (si no). En el entorno de prueba
 * Ala está, así que tienen que estar todas las de Ala y ninguna vanilla. Si una receta tiene un id mal
 * escrito, Minecraft la descarta al cargar y este test lo detecta.
 */
public class RecipeGameTests {
    private static final List<String> ITEMS = List.of("quarry_lv", "quarry_mv", "quarry_hv", "quarry_core",
            "quarry_casing", "energy_port", "pumpjack", "upgrade_area", "upgrade_speed", "upgrade_fortune",
            "upgrade_silk_touch", "upgrade_void");

    @GameTest
    public void recipeVariantMatchesInstalledMods(GameTestHelper helper) {
        boolean ala = FabricLoader.getInstance().isModLoaded("alaindustrial");
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (String item : ITEMS) {
            boolean withAla = recipes.byKey(key(item)).isPresent();
            boolean vanilla = recipes.byKey(key(item + "_vanilla")).isPresent();
            helper.assertTrue(withAla == ala, "receta con Ala de " + item + " cargada=" + withAla);
            helper.assertTrue(vanilla == !ala, "receta vanilla de " + item + " cargada=" + vanilla);
        }
        helper.succeed();
    }

    private static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Quarryindustrial.id(path));
    }
}
