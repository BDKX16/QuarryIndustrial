package org.example.pz.quarryindustrial.client.render;

import static org.example.pz.quarryindustrial.client.render.PumpjackModels.CRANK_RADIUS;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.CRANK_Y;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.CRANK_Z;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.PITMAN_LENGTH;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.PIVOT_Y;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.PIVOT_Z;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.REAR_ARM;

/**
 * Mecanismo de cuatro barras de la cigüeña, en el plano lateral (Y, Z) y en píxeles:
 * manivela (eje fijo C, radio r) → biela (largo L) → cola del balancín (brazo trasero R, pivote fijo P).
 * Dado el ángulo de la manivela θ, busca el ángulo φ del balancín que deja la biela con su largo exacto,
 * así nada se estira ni "patina" como pasaría con una oscilación senoidal.
 *
 * <p>φ positivo = cabeza de caballo arriba (cola abajo).
 */
public final class PumpjackKinematics {
    private static final int ITERATIONS = 24;
    private static final double MAX_ANGLE = 0.8;

    private PumpjackKinematics() {
    }

    /** Pasador de la manivela para el ángulo θ (θ = 0: pasador arriba del eje). */
    public static double crankPinY(double theta) {
        return CRANK_Y + CRANK_RADIUS * Math.cos(theta);
    }

    public static double crankPinZ(double theta) {
        return CRANK_Z + CRANK_RADIUS * Math.sin(theta);
    }

    public static double tailY(double phi) {
        return PIVOT_Y - REAR_ARM * Math.sin(phi);
    }

    public static double tailZ(double phi) {
        return PIVOT_Z + REAR_ARM * Math.cos(phi);
    }

    /**
     * Ángulo del balancín. Al subir φ la cola baja y se acerca al pasador, así que la distancia
     * cola–pasador decrece con φ: alcanza una bisección.
     */
    public static double beamAngle(double theta) {
        double ky = crankPinY(theta);
        double kz = crankPinZ(theta);
        double lo = -MAX_ANGLE;
        double hi = MAX_ANGLE;
        for (int i = 0; i < ITERATIONS; i++) {
            double mid = (lo + hi) / 2;
            double dy = tailY(mid) - ky;
            double dz = tailZ(mid) - kz;
            if (Math.sqrt(dy * dy + dz * dz) > PITMAN_LENGTH) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }
}
