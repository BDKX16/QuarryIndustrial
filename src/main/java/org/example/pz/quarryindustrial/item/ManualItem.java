package org.example.pz.quarryindustrial.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Manual del operador: el libro del mod. Clic derecho abre la pantalla con todos los capítulos (quarries,
 * brocas, nuclear, petróleo, offshore, drones, generadores). El contenido está en
 * {@code assets/quarryindustrial/manual/<idioma>.json}.
 */
public class ManualItem extends Item {
    /** Lo engancha el cliente para abrir la pantalla; en el servidor no hace nada. */
    public static Runnable openScreen = () -> {
    };

    public ManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreen.run();
        }
        return InteractionResult.SUCCESS;
    }
}
