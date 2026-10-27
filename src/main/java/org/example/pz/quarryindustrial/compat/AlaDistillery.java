package org.example.pz.quarryindustrial.compat;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * La columna de destilación de Ala Industrial se ensucia (coquiza) con el uso y se limpia con la llave inglesa, que
 * suelta carbón. Ala no es una dependencia de compilación, así que se la usa por reflexión: sin Ala, ninguna
 * columna existe y todo esto responde que no hay nada que limpiar.
 */
public final class AlaDistillery {
    private static final String COLUMN = "dev.alaindustrial.block.entity.DistillationColumnBlockEntity";

    private static boolean resolved;
    private static @Nullable Class<?> column;
    private static @Nullable Method fouling;
    private static @Nullable Method clean;

    private AlaDistillery() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            column = Class.forName(COLUMN, false, AlaDistillery.class.getClassLoader());
            fouling = column.getMethod("getFouling");
            clean = column.getMethod("cleanFouling");
        } catch (ReflectiveOperationException | LinkageError e) {
            column = null;
        }
    }

    public static boolean isColumn(@Nullable BlockEntity be) {
        resolve();
        return column != null && column.isInstance(be);
    }

    /** Cuánto está ensuciada la columna, de 0 a 100 (0 si no es una columna). */
    public static int fouling(BlockEntity be) {
        if (!isColumn(be) || fouling == null) {
            return 0;
        }
        try {
            return (int) fouling.invoke(be);
        } catch (ReflectiveOperationException e) {
            Quarryindustrial.LOGGER.warn("No se pudo leer el ensuciamiento de la columna de destilación", e);
            return 0;
        }
    }

    /** Limpia la columna y devuelve cuánto carbón rinde la limpieza (0 si no había nada). */
    public static int clean(BlockEntity be) {
        if (!isColumn(be) || clean == null) {
            return 0;
        }
        try {
            return (int) clean.invoke(be);
        } catch (ReflectiveOperationException e) {
            Quarryindustrial.LOGGER.warn("No se pudo limpiar la columna de destilación", e);
            return 0;
        }
    }
}
