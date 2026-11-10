package org.example.pz.quarryindustrial.filter;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Lo que el mixin le agrega al caño de ítems de Ala Industrial: un filtro opcional por cara. Es una interfaz del mod
 * para no tocar clases de Ala desde fuera de los mixins (la dependencia es blanda).
 */
public interface PipeFilterHolder {
    @Nullable
    PipeFilterData quarry$getFilter(Direction face);

    /** Pone (o quita, con nulo) el filtro de una cara. */
    void quarry$setFilter(Direction face, @Nullable PipeFilterData data);

    Map<Direction, PipeFilterData> quarry$filters();
}
