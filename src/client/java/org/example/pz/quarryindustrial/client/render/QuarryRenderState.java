package org.example.pz.quarryindustrial.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Foto de la quarry para un frame. Todas las coordenadas son relativas a la esquina del bloque de la
 * quarry, que es donde arranca el {@code PoseStack} del renderer.
 */
public class QuarryRenderState extends BlockEntityRenderState {
    public boolean hasRig;
    /** Bordes del marco (un bloque por fuera del área) y altura del anillo de vigas. */
    public float frameMinX;
    public float frameMaxX;
    public float topY;
    public int headCount;
    /** Por cabezal: x, y, z del bloque que está minando (centro en x/z, base en y). */
    public float[] heads = new float[0];
    public int[] headLights = new int[0];
    public float drillAngle;
    /** Tier nuclear: broca PDC en vez de tricónica. */
    public boolean pdcBit;
    /** Multibloque incompleto: posiciones (x, y, z relativas) de las piezas que faltan, para el holograma. */
    public float[] ghosts = new float[0];
}
