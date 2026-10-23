package org.example.pz.quarryindustrial.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.example.pz.quarryindustrial.item.BitTier;
import org.jetbrains.annotations.Nullable;

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
    /** La broca instalada (null = ninguna), su desgaste de 0 a 1 y el tinte del acero por el calor. */
    public @Nullable BitTier bit;
    public float bitWear;
    public int bodyTint = 0xFFFFFFFF;
    /** Por cabezal: el último bloque minado, que se dibuja como la carga de la tolva. */
    public ItemStackRenderState[] hopperItems = new ItemStackRenderState[0];
    public float hopperFill;
    /** Multibloque incompleto: posiciones (x, y, z relativas) de las piezas que faltan, para el holograma. */
    public float[] ghosts = new float[0];
}
