package org.example.pz.quarryindustrial.block.offshore;

import net.minecraft.network.chat.Component;

/** Estado de la plataforma offshore. */
public enum OffshoreStatus {
    IDLE,
    /** Ningún mod registró un fluido en {@code #c:oil}. */
    NO_OIL_FLUID,
    /** Falta una pata o un bloque de la cubierta. */
    DAMAGED,
    NO_POWER,
    /** Perforando: hace falta una broca. */
    NO_BIT,
    OUTPUT_FULL,
    /** Bajando la sarta hasta el reservorio debajo del lecho marino. */
    DRILLING,
    /** Reservorio alcanzado: produce para siempre. */
    PRODUCING;

    public Component label() {
        return Component.translatable("status.quarryindustrial.offshore." + name().toLowerCase());
    }

    public boolean running() {
        return this == DRILLING || this == PRODUCING;
    }
}
