package org.example.pz.quarryindustrial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.filter.PipeFilterHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Dibuja el filtro enganchado a cada cara de un caño de ítems de Ala: un manguito de hierro con una banda celeste
 * sobre el brazo, justo antes del inventario. El caño de Ala no tiene renderer propio, así que este se le suma.
 */
public class PipeFilterRenderer implements BlockEntityRenderer<BlockEntity, PipeFilterRenderer.State> {
    private static final Identifier TEXTURE = Quarryindustrial.id("textures/entity/pipe_filter.png");
    private static final float PX = 1 / 16f;
    /** Del centro del bloque al centro del manguito, en píxeles: el núcleo llega a 2 y el brazo mide 6. */
    private static final float OFFSET = 4.5f;
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

    private final ModelPart sleeve;
    private final ModelPart band;

    public static class State extends BlockEntityRenderState {
        /** Caras con filtro, un bit por {@link Direction#get3DDataValue()}. */
        int faces;
    }

    public PipeFilterRenderer(BlockEntityRendererProvider.Context context) {
        // Textura de 32×32: arriba el hierro del manguito, abajo el celeste de la banda
        MeshDefinition sleeveMesh = new MeshDefinition();
        PartDefinition sleeveRoot = sleeveMesh.getRoot();
        sleeveRoot.addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -3.5f, -1.5f, 7, 7, 3),
                PartPose.ZERO);
        sleeve = LayerDefinition.create(sleeveMesh, 32, 32).bakeRoot();
        MeshDefinition bandMesh = new MeshDefinition();
        bandMesh.getRoot().addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 16).addBox(-3.75f, -3.75f, -0.5f, 7.5f, 7.5f, 1),
                PartPose.ZERO);
        band = LayerDefinition.create(bandMesh, 32, 32).bakeRoot();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntity pipe, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTick, cameraPos, crumbling);
        state.faces = 0;
        if (pipe instanceof PipeFilterHolder holder) {
            for (Direction face : holder.quarry$filters().keySet()) {
                state.faces |= 1 << face.get3DDataValue();
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.faces == 0) {
            return;
        }
        RenderType type = RenderTypes.entityCutout(TEXTURE);
        for (Direction face : Direction.values()) {
            if ((state.faces & (1 << face.get3DDataValue())) == 0) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.5f + face.getStepX() * OFFSET * PX, 0.5f + face.getStepY() * OFFSET * PX,
                    0.5f + face.getStepZ() * OFFSET * PX);
            // El modelo es fino en Z: se lo gira para que su eje quede a lo largo del brazo
            switch (face.getAxis()) {
                case X -> poseStack.rotate(Axis.YP, (float) (Math.PI / 2));
                case Y -> poseStack.rotate(Axis.XP, (float) (Math.PI / 2));
                default -> { }
            }
            collector.submitModelPart(sleeve, poseStack, type, state.lightCoords, OverlayTexture.NO_OVERLAY, DIRECT_UV);
            // La banda brilla: se la dibuja a pleno brillo para que se distinga aun en la oscuridad
            collector.submitModelPart(band, poseStack, type, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    DIRECT_UV);
            poseStack.popPose();
        }
    }
}
