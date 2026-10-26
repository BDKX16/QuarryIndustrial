package org.example.pz.quarryindustrial.registry;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.quarry.QuarryMenu;
import org.example.pz.quarryindustrial.menu.InfoMenu;

public final class ModMenus {
    /** Al abrir, el servidor manda la posición: el cliente la usa para leer el área y los cabezales del minimapa. */
    public static final ExtendedMenuType<QuarryMenu, BlockPos> QUARRY = Registry.register(BuiltInRegistries.MENU,
            Quarryindustrial.id("quarry"), new ExtendedMenuType<>(QuarryMenu::new, BlockPos.STREAM_CODEC));

    /** GUI de solo lectura del pad, la cigüeña, la plataforma offshore y los generadores. */
    public static final ExtendedMenuType<InfoMenu, InfoMenu.Open> INFO = Registry.register(BuiltInRegistries.MENU,
            Quarryindustrial.id("info"), new ExtendedMenuType<>(InfoMenu::new, InfoMenu.Open.CODEC));

    private ModMenus() {
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
