package org.example.pz.quarryindustrial.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import org.example.pz.quarryindustrial.Quarryindustrial;

import java.util.ArrayList;
import java.util.List;

/** Componentes de datos de los items del mod. */
public final class ModComponents {
    /** Vinculador: el pad de origen elegido, esperando el clic en el de destino. */
    public static final DataComponentType<BlockPos> LINK_SOURCE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, Quarryindustrial.id("link_source"),
            new DataComponentType.Builder<BlockPos>()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build());
    /** Informe del dron explorador: dónde vio géiseres de petróleo. */
    public static final DataComponentType<List<BlockPos>> SURVEY_SITES = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, Quarryindustrial.id("survey_sites"),
            new DataComponentType.Builder<List<BlockPos>>()
                    .persistent(BlockPos.CODEC.listOf())
                    .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)))
                    .build());

    private ModComponents() {
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
