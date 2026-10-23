package org.example.pz.quarryindustrial.block.quarry;

import net.minecraft.network.chat.Component;

/** Por qué la quarry está (o no está) minando, para la GUI y el mensaje de estado. */
public enum QuarryStatus {
    IDLE,
    BUILDING_FRAME,
    MINING,
    NO_POWER,
    OUTPUT_FULL,
    FINISHED,
    /** Quarry nuclear con el cubo incompleto o sin puertos de energía. */
    STRUCTURE_INCOMPLETE,
    /** Sin broca (o se rompió la que tenía). */
    NO_BIT,
    /** Quarry nuclear sin refrigerante: mina, pero frenada por el calor. */
    OVERHEATING;

    public Component label() {
        return Component.translatable("status.quarryindustrial." + name().toLowerCase());
    }

    /** Si el taladro gira en el renderer. */
    public boolean drilling() {
        return this == MINING || this == OVERHEATING;
    }
}
