package org.example.pz.quarryindustrial.block.drone;

import net.minecraft.network.chat.Component;

/** Qué está haciendo el pad (y su dron). */
public enum DronePadStatus {
    NO_DRONE,
    /** Dron de transporte sin pad de destino vinculado. */
    NO_LINK,
    /** El destino ya no es un pad, o quedó demasiado lejos. */
    BAD_LINK,
    /** No alcanza la energía para el viaje. */
    NO_POWER,
    /** Juntando carga antes de salir. */
    LOADING,
    /** El pad de destino está lleno. */
    DESTINATION_FULL,
    /** Mantenimiento: nada que atender (o no hay brocas de repuesto). */
    STANDBY,
    /** Explorador listo: clic derecho en el pad para mandarlo. */
    READY,
    IN_FLIGHT,
    /** No se supo más del dron: se dio por perdido y volvió al pad (sin la carga). */
    LOST;

    public Component label() {
        return Component.translatable("status.quarryindustrial.drone_pad." + name().toLowerCase());
    }
}
