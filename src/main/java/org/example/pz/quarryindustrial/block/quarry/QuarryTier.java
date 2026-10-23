package org.example.pz.quarryindustrial.block.quarry;

import org.example.pz.quarryindustrial.config.QuarryConfig;

/**
 * Los tiers de la quarry, alineados con los voltajes de Ala Industrial (LV 32, MV 128, HV 512 EU/t).
 * NUCLEAR es el multibloque: recibe energía por varios puertos HV, uno por reactor.
 */
public enum QuarryTier {
    LV,
    MV,
    HV,
    NUCLEAR;

    public boolean multiblock() {
        return this == NUCLEAR;
    }

    /** Cuántos bloques detrás del controlador empieza el área (el multibloque mide 3 de fondo). */
    public int areaOffset() {
        return multiblock() ? NuclearStructure.DEPTH : 1;
    }

    public QuarryConfig.Tier settings() {
        QuarryConfig config = QuarryConfig.get();
        return switch (this) {
            case LV -> config.lv;
            case MV -> config.mv;
            case HV -> config.hv;
            case NUCLEAR -> config.nuclear;
        };
    }
}
