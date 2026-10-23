package org.example.pz.quarryindustrial.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.compat.MachineInfo;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.util.List;

/**
 * Plugin de Jade. Del lado del servidor arma las líneas de {@link MachineInfo} y las manda en los datos del
 * bloque; el cliente ({@code client.compat.JadeClient}, cargado por reflexión porque usa clases que solo existen
 * en el cliente) las muestra. Solo se carga si Jade está instalado.
 */
@WailaPlugin(Quarryindustrial.MOD_ID)
public class QuarryJadePlugin implements IWailaPlugin {
    public static final Identifier MACHINE = Quarryindustrial.id("machine");
    public static final String LINES = "QuarryLines";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new IServerDataProvider<>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                List<Component> lines = MachineInfo.lines(accessor.getBlockEntity());
                if (!lines.isEmpty()) {
                    data.store(LINES, ComponentSerialization.CODEC.listOf(),
                            accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), lines);
                }
            }

            @Override
            public Identifier getUid() {
                return MACHINE;
            }
        }, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        try {
            Class.forName("org.example.pz.quarryindustrial.client.compat.JadeClient")
                    .getMethod("register", IWailaClientRegistration.class).invoke(null, registration);
        } catch (ReflectiveOperationException e) {
            Quarryindustrial.LOGGER.warn("No se pudo registrar la parte de cliente de Jade", e);
        }
    }
}
