package org.example.pz.quarryindustrial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.block.offshore.OffshoreLayout;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreStatus;
import org.example.pz.quarryindustrial.item.BitTier;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * La plataforma offshore por encima de la cubierta: torre de perforación en celosía (cónica, con riostras en
 * cruz), bloque de corona, aparejo móvil con sus cables, casilla del perforador, sala de máquinas, barandas y
 * la antorcha de gas. Por debajo, la sarta baja por el agua desde la mesa rotativa hasta el lecho marino.
 */
public class OffshoreRenderer implements BlockEntityRenderer<OffshoreRigBlockEntity, OffshoreRenderer.State> {
    private static final UvMapping DIRECT_UV = new UvMapping() {
        @Override
        public float getU(float u) {
            return u;
        }

        @Override
        public float getV(float v) {
            return v;
        }
    };
    private static final float PX = 1 / 16f;
    /** Torre: base de 2.8 × 2.8 bloques sobre la cubierta, se angosta hasta 0.9 arriba, 12 de alto. */
    private static final float BASE_HALF = 1.4f;
    private static final float TOP_HALF = 0.45f;
    private static final float HEIGHT = 12;
    private static final float DECK_TOP = 1;
    private static final int BAYS = 6;
    private static final float CONE_SPIN_RATIO = -2.4f;

    private final ModelPart yellow = OffshoreModels.unit(OffshoreModels.YELLOW);
    private final ModelPart steel = OffshoreModels.unit(OffshoreModels.STEEL);
    private final ModelPart dark = OffshoreModels.unit(OffshoreModels.DARK);
    private final ModelPart crown = OffshoreModels.crownBlock();
    private final ModelPart travelingBlock = OffshoreModels.travelingBlock();
    private final ModelPart doghouse = OffshoreModels.doghouse();
    private final ModelPart engineHouse = OffshoreModels.engineHouse();
    private final ModelPart pipe = QuarryRigModels.pipeSegment();
    private final ModelPart kelly = QuarryRigModels.kelly();
    private final ModelPart triconeBody = QuarryRigModels.triconeBody();
    private final ModelPart cone = QuarryRigModels.cone();
    private final ModelPart pdcBit = QuarryRigModels.pdcBit();

    public static class State extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        float angle;
        /** Y relativa de la punta de la broca (debajo del lecho marino). */
        float bitY;
        /** Y relativa del lecho marino (la cara de arriba del fondo). */
        float seabedY;
        /** Altura relativa del aparejo móvil. */
        float travelY;
        int topLight;
        int waterLight;
        @Nullable BitTier bit;
    }

    public OffshoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(OffshoreRigBlockEntity rig, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(rig, state, partialTick, cameraPos, crumbling);
        BlockPos pos = rig.getBlockPos();
        Level level = rig.getLevel();
        state.angle = rig.drillAngle(partialTick);
        state.seabedY = rig.seabedY() + 1 - pos.getY();
        state.bitY = state.seabedY - (float) rig.drilled();
        state.bit = rig.visualBit();
        // Perforando, el aparejo baja con cada metro y vuelve a subir para agregar un tubo; produciendo, quieto
        float travel = rig.status() == OffshoreStatus.DRILLING ? 1 - (float) (rig.drilled() % 1.0) : 0.35f;
        state.travelY = DECK_TOP + 2.2f + (HEIGHT - 4.5f) * travel;
        if (level != null) {
            state.topLight = LightCoordsUtil.getLightCoords(level, pos.above(2));
            state.waterLight = LightCoordsUtil.getLightCoords(level, pos.below(2));
        } else {
            state.topLight = LightCoordsUtil.FULL_BRIGHT;
            state.waterLight = LightCoordsUtil.FULL_BRIGHT;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        RenderType type = RenderTypes.entityCutout(QuarryRenderer.TEXTURE);
        int light = state.topLight;
        submitDerrick(poseStack, collector, type, light);
        submitTackle(state, poseStack, collector, type, light);
        submitDeckEquipment(poseStack, collector, type, light);
        submitFlare(poseStack, collector, type, light);
        submitDrillString(state, poseStack, collector, type);
    }

    /** Cuatro patas cónicas, anillos horizontales cada tramo y cruces en cada cara. */
    private void submitDerrick(PoseStack poseStack, SubmitNodeCollector collector, RenderType type, int light) {
        Vector3f[] previous = null;
        for (int bay = 0; bay <= BAYS; bay++) {
            float t = bay / (float) BAYS;
            float y = DECK_TOP + HEIGHT * t;
            float half = BASE_HALF + (TOP_HALF - BASE_HALF) * t;
            Vector3f[] ring = {
                    new Vector3f(0.5f - half, y, 0.5f - half), new Vector3f(0.5f + half, y, 0.5f - half),
                    new Vector3f(0.5f + half, y, 0.5f + half), new Vector3f(0.5f - half, y, 0.5f + half)};
            for (int i = 0; i < 4; i++) {
                if (bay > 0) {
                    beam(poseStack, collector, yellow, type, light, ring[i], ring[(i + 1) % 4], 1.5f);
                }
                if (previous != null) {
                    // Pata (montante) y cruz de la cara
                    beam(poseStack, collector, yellow, type, light, previous[i], ring[i], 3);
                    beam(poseStack, collector, steel, type, light, previous[i], ring[(i + 1) % 4], 1);
                    beam(poseStack, collector, steel, type, light, previous[(i + 1) % 4], ring[i], 1);
                }
            }
            previous = ring;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, DECK_TOP + HEIGHT, 0.5f);
        submit(collector, crown, poseStack, type, light);
        poseStack.popPose();
    }

    /** Cables de la corona al aparejo, el aparejo con el gancho y el kelly que baja a la mesa rotativa. */
    private void submitTackle(State state, PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
                              int light) {
        float top = DECK_TOP + HEIGHT;
        for (float dx : new float[]{-0.25f, 0.25f}) {
            beam(poseStack, collector, dark, type, light, new Vector3f(0.5f + dx, top, 0.5f),
                    new Vector3f(0.5f + dx * 0.5f, state.travelY + 12 * PX, 0.5f), 0.5f);
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, state.travelY, 0.5f);
        submit(collector, travelingBlock, poseStack, type, light);
        poseStack.popPose();
        // Kelly girando, desde el gancho hasta la mesa rotativa (la cara de arriba del controlador)
        float kellyTop = state.travelY - 10 * PX;
        float kellyLength = kellyTop - DECK_TOP;
        if (kellyLength > 0) {
            poseStack.pushPose();
            poseStack.translate(0.5f, DECK_TOP, 0.5f);
            poseStack.rotate(Axis.YP, state.angle);
            poseStack.scale(1, kellyLength, 1);
            submit(collector, kelly, poseStack, type, light);
            poseStack.popPose();
        }
    }

    private void submitDeckEquipment(PoseStack poseStack, SubmitNodeCollector collector, RenderType type, int light) {
        poseStack.pushPose();
        poseStack.translate(2.0f, DECK_TOP, -2.9f);
        submit(collector, doghouse, poseStack, type, light);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(-2.6f, DECK_TOP, 1.6f);
        submit(collector, engineHouse, poseStack, type, light);
        poseStack.popPose();
        // Barandas alrededor de la cubierta: pasamanos y postes cada dos bloques
        float min = -OffshoreLayout.HALF + 0.1f;
        float max = OffshoreLayout.HALF + 0.9f;
        float rail = DECK_TOP + 0.9f;
        Vector3f[] corners = {new Vector3f(min, rail, min), new Vector3f(max, rail, min),
                new Vector3f(max, rail, max), new Vector3f(min, rail, max)};
        for (int i = 0; i < 4; i++) {
            Vector3f a = corners[i];
            Vector3f b = corners[(i + 1) % 4];
            beam(poseStack, collector, yellow, type, light, a, b, 1);
            for (int post = 0; post <= 3; post++) {
                Vector3f top = new Vector3f(a).lerp(b, post / 3f);
                beam(poseStack, collector, yellow, type, light, new Vector3f(top.x, DECK_TOP, top.z), top, 1);
            }
        }
    }

    /** Brazo de la antorcha: dos vigas en celosía desde la esquina hasta el quemador. */
    private void submitFlare(PoseStack poseStack, SubmitNodeCollector collector, RenderType type, int light) {
        Vector3f tip = new Vector3f((float) (0.5 + OffshoreLayout.FLARE_TIP_X), (float) OffshoreLayout.FLARE_TIP_Y,
                (float) (0.5 + OffshoreLayout.FLARE_TIP_Z));
        Vector3f baseA = new Vector3f(-2.6f, DECK_TOP, 3.6f);
        Vector3f baseB = new Vector3f(-2.9f, DECK_TOP + 0.6f, 3.9f);
        beam(poseStack, collector, yellow, type, light, baseA, tip, 2);
        beam(poseStack, collector, yellow, type, light, baseB, tip, 2);
        for (int i = 1; i < 5; i++) {
            Vector3f a = new Vector3f(baseA).lerp(tip, i / 5f);
            Vector3f b = new Vector3f(baseB).lerp(tip, (i + 0.5f) / 5f);
            beam(poseStack, collector, steel, type, light, a, b, 1);
        }
        poseStack.pushPose();
        poseStack.translate(tip.x, tip.y - 0.3f, tip.z);
        poseStack.scale(5, 5, 5);
        submit(collector, dark, poseStack, type, light);
        poseStack.popPose();
    }

    /** La sarta debajo de la cubierta: tubos desde el controlador hasta la broca, en el fondo del pozo. */
    private void submitDrillString(State state, PoseStack poseStack, SubmitNodeCollector collector, RenderType type) {
        float bitBase = state.bitY;
        float bitTop = bitBase + 1;
        float y = bitTop;
        while (y < 0) {
            float length = Math.min(1, -y);
            // Dentro del lecho marino no se ve; con luz 0 no hace falta saber más
            int light = y > state.seabedY ? state.waterLight : 0;
            poseStack.pushPose();
            poseStack.translate(0.5f, y, 0.5f);
            poseStack.rotate(Axis.YP, state.angle);
            poseStack.scale(1, length, 1);
            submit(collector, pipe, poseStack, type, light);
            poseStack.popPose();
            y += length;
        }
        BitTier bit = state.bit;
        if (bit == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, bitBase, 0.5f);
        poseStack.rotate(Axis.YP, state.angle);
        int light = state.waterLight;
        if (bit != null && bit.pdc()) {
            submit(collector, pdcBit, poseStack, type, light);
        } else {
            submit(collector, triconeBody, poseStack, type, light);
            for (int leg = 0; leg < 3; leg++) {
                poseStack.pushPose();
                poseStack.rotate(Axis.YP, (float) (leg * Math.PI * 2 / 3));
                poseStack.translate(0, QuarryRigModels.CONE_MOUNT_Y * PX, QuarryRigModels.CONE_MOUNT_Z * PX);
                poseStack.rotate(Axis.XP, QuarryRigModels.CONE_TILT);
                poseStack.rotate(Axis.YP, state.angle * CONE_SPIN_RATIO);
                submit(collector, cone, poseStack, type, light);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    /** Una viga de {@code thickness} px de lado entre dos puntos (coordenadas relativas, en bloques). */
    private static void beam(PoseStack poseStack, SubmitNodeCollector collector, ModelPart part, RenderType type,
                             int light, Vector3f from, Vector3f to, float thickness) {
        Vector3f direction = new Vector3f(to).sub(from);
        float length = direction.length();
        if (length < 1e-4f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        poseStack.rotate(new Quaternionf().rotationTo(0, 1, 0, direction.x, direction.y, direction.z));
        poseStack.scale(thickness, length * 16, thickness);
        submit(collector, part, poseStack, type, light);
        poseStack.popPose();
    }

    private static void submit(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack, RenderType type,
                               int light) {
        collector.submitModelPart(part, poseStack, type, light, OverlayTexture.NO_OVERLAY, DIRECT_UV);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
