package org.example.pz.quarryindustrial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.jetbrains.annotations.Nullable;

/** El dron posado en su pad, con las hélices quietas. En vuelo lo dibuja su propia entidad. */
public class DronePadRenderer implements BlockEntityRenderer<DronePadBlockEntity, DronePadRenderer.State> {
    private final DroneModel model = new DroneModel();

    public static class State extends BlockEntityRenderState {
        @Nullable DroneKind kind;
        float age;
    }

    public DronePadRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DronePadBlockEntity pad, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pad, state, partialTick, cameraPos, crumbling);
        state.kind = pad.droneDocked() ? pad.droneKind() : null;
        state.age = pad.getLevel() != null ? pad.getLevel().getGameTime() + partialTick : 0;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.kind == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, 4 / 16f, 0.5f);
        model.submit(poseStack, collector, state.kind, false, 0, state.age, state.lightCoords);
        poseStack.popPose();
    }
}
