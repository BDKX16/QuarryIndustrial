package org.example.pz.quarryindustrial.item;

import org.example.pz.quarryindustrial.config.QuarryConfig;

/**
 * Las brocas, de la más barata a la del endgame. Las tres primeras son tricónicas (conos con insertos);
 * la PDC es de aletas con cortadores de diamante. La velocidad multiplica el tope de bloques por segundo
 * de la quarry; la durabilidad es en "puntos de desgaste" (piedra = 1, pizarra profunda = 2, obsidiana = 17).
 */
public enum BitTier {
    IRON(0xFFC4CAD2),
    DIAMOND(0xFF6FF0F0),
    NETHERITE(0xFF5B4E57),
    PDC(0xFFE8F4FF);

    /** Tinte de los insertos / cortadores en el renderer (ARGB). */
    private final int teethTint;

    BitTier(int teethTint) {
        this.teethTint = teethTint;
    }

    public int teethTint() {
        return teethTint;
    }

    public boolean pdc() {
        return this == PDC;
    }

    public QuarryConfig.Bit settings() {
        QuarryConfig.Bits bits = QuarryConfig.get().bits;
        return switch (this) {
            case IRON -> bits.iron;
            case DIAMOND -> bits.diamond;
            case NETHERITE -> bits.netherite;
            case PDC -> bits.pdc;
        };
    }

    public static BitTier byOrdinal(int ordinal) {
        BitTier[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
