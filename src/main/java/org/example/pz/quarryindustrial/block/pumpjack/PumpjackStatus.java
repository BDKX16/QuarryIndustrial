package org.example.pz.quarryindustrial.block.pumpjack;

import net.minecraft.network.chat.Component;

/** Estado de la cigüeña, para el mensaje de estado y la animación. */
public enum PumpjackStatus {
    IDLE,
    /** No está sobre el pozo de un géiser. */
    NO_WELL,
    /** Ningún mod registró un fluido en {@code #c:oil} (falta Ala Industrial). */
    NO_OIL_FLUID,
    NO_POWER,
    OUTPUT_FULL,
    /** Vaciando el oil real que queda en el caño y en el domo. */
    DRAINING,
    /** Domo vacío: extracción infinita a caudal fijo. */
    PUMPING;

    public Component label() {
        return Component.translatable("status.quarryindustrial.pumpjack." + name().toLowerCase());
    }

    public boolean running() {
        return this == DRAINING || this == PUMPING;
    }
}
