package org.example.pz.quarryindustrial.client.compat;

import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import org.example.pz.quarryindustrial.block.drone.DronePadBlock;
import org.example.pz.quarryindustrial.block.generator.GeneratorBlock;
import org.example.pz.quarryindustrial.block.offshore.OffshoreDeckBlock;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackPartBlock;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.compat.jade.QuarryJadePlugin;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

/** Parte de cliente del plugin de Jade: muestra las líneas que mandó el servidor. */
public final class JadeClient {
    private static final IBlockComponentProvider LINES = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            List<Component> lines = accessor.getServerData().read(QuarryJadePlugin.LINES,
                    ComponentSerialization.CODEC.listOf(),
                    accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE)).orElse(List.of());
            lines.forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return QuarryJadePlugin.MACHINE;
        }
    };

    private JadeClient() {
    }

    public static void register(IWailaClientRegistration registration) {
        for (Class<? extends net.minecraft.world.level.block.Block> block : List.of(QuarryBlock.class,
                EnergyPortBlock.class, PumpjackBlock.class, PumpjackPartBlock.class, OffshoreRigBlock.class,
                OffshoreDeckBlock.class, GeneratorBlock.class, DronePadBlock.class)) {
            registration.registerBlockComponent(LINES, block);
        }
    }
}
