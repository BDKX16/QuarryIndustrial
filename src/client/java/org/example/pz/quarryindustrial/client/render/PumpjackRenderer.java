package org.example.pz.quarryindustrial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.jetbrains.annotations.Nullable;

import static org.example.pz.quarryindustrial.client.render.PumpjackModels.CARRIER_REST_Y;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.CRANK_Y;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.CRANK_Z;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.FRONT_ARM;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.PITMAN_X;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.PIVOT_Y;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.PIVOT_Z;
import static org.example.pz.quarryindustrial.client.render.PumpjackModels.STUFFING_BOX_TOP;

/**
 * Dibuja la cigüeña completa desde la cabeza de pozo. Todo el modelo está en el marco local de
 * {@link PumpjackModels}; acá se lo lleva al centro del bloque y se lo gira según el facing.
 */
public class PumpjackRenderer implements BlockEntityRenderer<PumpjackBlockEntity, PumpjackRenderer.State> {
    public static final Identifier TEXTURE = Quarryindustrial.id("textures/entity/pumpjack.png");
    private static final float PX = 1 / 16f;
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

    private final ModelPart frame = PumpjackModels.frame();
    private final ModelPart walkingBeam = PumpjackModels.walkingBeam();
    private final ModelPart crank = PumpjackModels.crank();
    private final ModelPart pitman = PumpjackModels.pitman();
    private final ModelPart cable = PumpjackModels.cable();
    private final ModelPart polishedRod = PumpjackModels.polishedRod();
    private final ModelPart carrierBar = PumpjackModels.carrierBar();

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float crankAngle;
        public int light;
    }

    public PumpjackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PumpjackBlockEntity pumpjack, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pumpjack, state, partialTick, cameraPos, crumbling);
        state.facing = pumpjack.getBlockState().getValue(PumpjackBlock.FACING);
        state.crankAngle = pumpjack.crankAngle(partialTick);
        // La luz del bloque de la cabeza de pozo es baja (está pegada al suelo): se usa la de un poco más arriba
        state.light = pumpjack.getLevel() != null
                ? LightCoordsUtil.getLightCoords(pumpjack.getLevel(), pumpjack.getBlockPos().above(2))
                : state.lightCoords;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        RenderType renderType = RenderTypes.entityCutout(TEXTURE);
        int light = state.light;
        float theta = state.crankAngle;
        double phi = PumpjackKinematics.beamAngle(theta);

        poseStack.pushPose();
        // Centro del bloque; el marco local tiene +Z hacia atrás (hacia el motor)
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.rotate(Axis.YP, (float) Math.toRadians(180 - state.facing.toYRot()));

        submit(collector, frame, poseStack, renderType, light);

        // Balancín y cabeza de caballo, girando sobre el pivote
        poseStack.pushPose();
        poseStack.translate(0, PIVOT_Y * PX, PIVOT_Z * PX);
        poseStack.rotate(Axis.XP, (float) phi);
        submit(collector, walkingBeam, poseStack, renderType, light);
        poseStack.popPose();

        // Manivelas con contrapesos y bielas, a los dos lados del reductor
        double pinY = PumpjackKinematics.crankPinY(theta);
        double pinZ = PumpjackKinematics.crankPinZ(theta);
        double tailY = PumpjackKinematics.tailY(phi);
        double tailZ = PumpjackKinematics.tailZ(phi);
        float pitmanTilt = (float) Math.atan2(tailZ - pinZ, tailY - pinY);
        for (float side : new float[]{-PITMAN_X, PITMAN_X}) {
            poseStack.pushPose();
            poseStack.translate(side * PX, CRANK_Y * PX, CRANK_Z * PX);
            poseStack.rotate(Axis.XP, theta);
            submit(collector, crank, poseStack, renderType, light);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(side * PX, (float) pinY * PX, (float) pinZ * PX);
            poseStack.rotate(Axis.XP, pitmanTilt);
            submit(collector, pitman, poseStack, renderType, light);
            poseStack.popPose();
        }

        // Cable (bridle) colgando de la cabeza de caballo, barra portadora y vástago pulido.
        // El cable se enrolla sobre el arco: la barra sube lo que recorre el arco, FRONT_ARM · φ.
        float carrierY = (float) (CARRIER_REST_Y + FRONT_ARM * phi);
        float bridleLength = PIVOT_Y - carrierY;
        for (float x : new float[]{-2, 2}) {
            poseStack.pushPose();
            poseStack.translate(x * PX, carrierY * PX, 0);
            poseStack.scale(1, bridleLength, 1);
            submit(collector, cable, poseStack, renderType, light);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0, carrierY * PX, 0);
        submit(collector, carrierBar, poseStack, renderType, light);
        poseStack.popPose();

        float rodBottom = STUFFING_BOX_TOP - 2;
        poseStack.pushPose();
        poseStack.translate(0, rodBottom * PX, 0);
        poseStack.scale(1, carrierY + 5 - rodBottom, 1);
        submit(collector, polishedRod, poseStack, renderType, light);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void submit(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack,
                               RenderType renderType, int light) {
        collector.submitModelPart(part, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, DIRECT_UV);
    }

    /** La cigüeña mide siete bloques: se dibuja aunque la cabeza de pozo quede fuera de cámara. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
