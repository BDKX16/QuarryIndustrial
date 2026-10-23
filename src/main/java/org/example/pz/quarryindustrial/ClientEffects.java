package org.example.pz.quarryindustrial;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

/**
 * Ganchos para lo que solo pasa en el cliente (sonidos). El código común llama a estos ganchos desde los
 * ticks de cliente de los block entities; el cliente los reemplaza al iniciar. En el servidor no hacen nada.
 */
public final class ClientEffects {
    /** Cada tick de cliente de un generador, cigüeña o plataforma: arranca o corta su loop de sonido. */
    public static Consumer<BlockEntity> machineTick = machine -> {
    };
    /** La cigüeña llegó al fondo de la carrera: el golpe del vástago. */
    public static Consumer<BlockEntity> pumpjackStroke = pumpjack -> {
    };

    private ClientEffects() {
    }
}
