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
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.quarry.QuarryArea;
import org.example.pz.quarryindustrial.block.quarry.NuclearStructure;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlock;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dibuja el pórtico móvil de la quarry: por cada cabezal, un puente que corre en Z sobre el marco, el carro
 * que corre en X sobre el puente y, colgando del carro, el kelly, la sarta de tubos y la broca tricónica.
 * El marco fijo son bloques reales; acá solo va lo que se mueve.
 */
public class QuarryRenderer implements BlockEntityRenderer<QuarryBlockEntity, QuarryRenderState> {
    public static final Identifier TEXTURE = Quarryindustrial.id("textures/entity/quarry_rig.png");
    /** La textura es propia (no está en un atlas), así que las UV del modelo van tal cual. */
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
    /** Altura de la parte de arriba de las vigas del marco, sobre la que apoyan los carros del puente. */
    private static final float RAIL_TOP = 12 * PX;
    /** Cara de abajo de la mesa rotativa del carro, de donde cuelga el kelly. */
    private static final float TABLE_BOTTOM = RAIL_TOP - 3 * PX;
    /** Los conos ruedan contra la roca más rápido que el giro de la broca. */
    private static final float CONE_SPIN_RATIO = -2.4f;
    /** Cuánto se hunden las puntas de los conos en el bloque que se está minando. */
    private static final float BIT_BITE = 0.1f;

    private final ModelPart bridgeSegment = QuarryRigModels.bridgeSegment();
    private final ModelPart bridgeTruck = QuarryRigModels.bridgeTruck();
    private final ModelPart carriage = QuarryRigModels.carriage();
    private final ModelPart beacon = QuarryRigModels.beacon();
    private final ModelPart pipeSegment = QuarryRigModels.pipeSegment();
    private final ModelPart kelly = QuarryRigModels.kelly();
    private final ModelPart triconeBody = QuarryRigModels.triconeBody();
    private final ModelPart cone = QuarryRigModels.cone();
    private final ModelPart pdcBit = QuarryRigModels.pdcBit();
    private final ModelPart ghostBlock = QuarryRigModels.ghostBlock();
    /** Tinte del holograma: verde "plano técnico", semitransparente (ARGB). */
    private static final int GHOST_TINT = 0x8840FF90;

    public QuarryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public QuarryRenderState createRenderState() {
        return new QuarryRenderState();
    }

    @Override
    public void extractRenderState(QuarryBlockEntity quarry, QuarryRenderState state, float partialTick,
                                   Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(quarry, state, partialTick, cameraPos, crumbling);
        QuarryArea area = quarry.area();
        Level level = quarry.getLevel();
        state.pdcBit = quarry.tier().multiblock();
        extractGhosts(quarry, state, level);
        int count = quarry.visualHeadCount();
        state.hasRig = area != null && quarry.frameBuilt() && level != null && count > 0;
        if (!state.hasRig) {
            return;
        }
        BlockPos pos = quarry.getBlockPos();
        state.frameMinX = area.minX() - 1 - pos.getX();
        state.frameMaxX = area.maxX() + 1 - pos.getX();
        state.topY = area.topY() - pos.getY();
        state.headCount = count;
        if (state.heads.length != count * 3) {
            state.heads = new float[count * 3];
            state.headLights = new int[count * 2];
        }
        for (int i = 0; i < count; i++) {
            double x = quarry.visualX(i, partialTick);
            double y = quarry.visualY(i, partialTick);
            double z = quarry.visualZ(i, partialTick);
            state.heads[i * 3] = (float) (x - pos.getX());
            state.heads[i * 3 + 1] = (float) (y - pos.getY());
            state.heads[i * 3 + 2] = (float) (z - pos.getZ());
            // Luz del carro (arriba, al aire libre) y de la broca (en el fondo del pozo)
            state.headLights[i * 2] = LightCoordsUtil.getLightCoords(level,
                    BlockPos.containing(x, area.topY() + 1, z));
            state.headLights[i * 2 + 1] = LightCoordsUtil.getLightCoords(level,
                    BlockPos.containing(x, y + 1.5, z));
        }
        state.drillAngle = quarry.drillAngle(partialTick);
    }

    /**
     * Quarry nuclear con el cubo incompleto: el cliente mira él mismo qué bloques faltan (son 26 lecturas,
     * no hace falta sincronizar nada) y los muestra como holograma.
     */
    private static void extractGhosts(QuarryBlockEntity quarry, QuarryRenderState state, @Nullable Level level) {
        if (!quarry.tier().multiblock() || level == null) {
            state.ghosts = new float[0];
            return;
        }
        BlockPos pos = quarry.getBlockPos();
        List<BlockPos> missing = NuclearStructure.scan(level, pos,
                quarry.getBlockState().getValue(QuarryBlock.FACING)).missing();
        if (state.ghosts.length != missing.size() * 3) {
            state.ghosts = new float[missing.size() * 3];
        }
        for (int i = 0; i < missing.size(); i++) {
            BlockPos ghost = missing.get(i);
            state.ghosts[i * 3] = ghost.getX() - pos.getX();
            state.ghosts[i * 3 + 1] = ghost.getY() - pos.getY();
            state.ghosts[i * 3 + 2] = ghost.getZ() - pos.getZ();
        }
    }

    @Override
    public void submit(QuarryRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.ghosts.length > 0) {
            RenderType ghostType = RenderTypes.entityTranslucent(TEXTURE);
            for (int i = 0; i < state.ghosts.length; i += 3) {
                poseStack.pushPose();
                poseStack.translate(state.ghosts[i], state.ghosts[i + 1], state.ghosts[i + 2]);
                collector.submitModelPart(ghostBlock, poseStack, ghostType, LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, DIRECT_UV, GHOST_TINT, 0);
                poseStack.popPose();
            }
        }
        if (!state.hasRig) {
            return;
        }
        RenderType renderType = RenderTypes.entityCutout(TEXTURE);
        for (int i = 0; i < state.headCount; i++) {
            float hx = state.heads[i * 3];
            float hy = state.heads[i * 3 + 1];
            float hz = state.heads[i * 3 + 2];
            int topLight = state.headLights[i * 2];
            int bitLight = state.headLights[i * 2 + 1];
            float railY = state.topY + RAIL_TOP;

            submitBridge(state, poseStack, collector, renderType, hz, railY, topLight);

            // Carro sobre el puente
            poseStack.pushPose();
            poseStack.translate(hx, railY, hz);
            submit(collector, carriage, poseStack, renderType, topLight);
            submit(collector, beacon, poseStack, renderType, LightCoordsUtil.FULL_BRIGHT);
            poseStack.popPose();

            submitDrillString(state, poseStack, collector, renderType, hx, hy, hz, railY + TABLE_BOTTOM - RAIL_TOP,
                    topLight, bitLight);
        }
    }

    /** El puente: una viga de lado a lado del marco, con un carro de rodadura en cada punta. */
    private void submitBridge(QuarryRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                              RenderType renderType, float z, float railY, int light) {
        int segments = (int) (state.frameMaxX - state.frameMinX) + 1;
        poseStack.pushPose();
        poseStack.translate(state.frameMinX, railY, z);
        for (int s = 0; s < segments; s++) {
            submit(collector, bridgeSegment, poseStack, renderType, light);
            poseStack.translate(1, 0, 0);
        }
        poseStack.popPose();
        for (float x : new float[]{state.frameMinX + 0.5f, state.frameMaxX + 0.5f}) {
            poseStack.pushPose();
            poseStack.translate(x, railY, z);
            submit(collector, bridgeTruck, poseStack, renderType, light);
            poseStack.popPose();
        }
    }

    /**
     * Kelly + tubería + broca, todo girando sobre el eje del cabezal. La broca apoya en el bloque que se
     * está minando; la tubería cubre el resto hasta la mesa rotativa, con el último tramo recortado.
     */
    private void submitDrillString(QuarryRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                   RenderType renderType, float x, float headY, float z, float tableY,
                                   int topLight, int bitLight) {
        float angle = state.drillAngle;
        // La broca apoya sobre el bloque que está perforando, con las puntas de los conos apenas metidas
        float bitBase = headY + 1 - BIT_BITE;
        float bitTop = bitBase + 1;
        float total = tableY - bitTop;

        if (total > 0) {
            // El kelly es el tramo de arriba (hasta un bloque); la tubería normal va debajo
            float kellyLength = Math.min(1, total);
            poseStack.pushPose();
            poseStack.translate(x, tableY - kellyLength, z);
            poseStack.rotate(Axis.YP, angle);
            poseStack.scale(1, kellyLength, 1);
            submit(collector, kelly, poseStack, renderType, topLight);
            poseStack.popPose();

            float remaining = total - kellyLength;
            float y = bitTop;
            while (remaining > 0) {
                float length = Math.min(1, remaining);
                int light = y > tableY - 3 ? topLight : bitLight;
                poseStack.pushPose();
                poseStack.translate(x, y, z);
                poseStack.rotate(Axis.YP, angle);
                poseStack.scale(1, length, 1);
                submit(collector, pipeSegment, poseStack, renderType, light);
                poseStack.popPose();
                y += length;
                remaining -= length;
            }
        }

        poseStack.pushPose();
        poseStack.translate(x, bitBase, z);
        poseStack.rotate(Axis.YP, angle);
        if (state.pdcBit) {
            submit(collector, pdcBit, poseStack, renderType, bitLight);
            poseStack.popPose();
            return;
        }
        submit(collector, triconeBody, poseStack, renderType, bitLight);
        for (int leg = 0; leg < 3; leg++) {
            poseStack.pushPose();
            poseStack.rotate(Axis.YP, (float) (leg * Math.PI * 2 / 3));
            poseStack.translate(0, QuarryRigModels.CONE_MOUNT_Y * PX, QuarryRigModels.CONE_MOUNT_Z * PX);
            poseStack.rotate(Axis.XP, QuarryRigModels.CONE_TILT);
            poseStack.rotate(Axis.YP, angle * CONE_SPIN_RATIO);
            submit(collector, cone, poseStack, renderType, bitLight);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void submit(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack,
                               RenderType renderType, int light) {
        collector.submitModelPart(part, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, DIRECT_UV);
    }

    /** El pórtico es mucho más grande que el bloque: hay que dibujarlo aunque la quarry quede fuera de cámara. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
