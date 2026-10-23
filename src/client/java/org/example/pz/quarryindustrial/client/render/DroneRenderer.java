package org.example.pz.quarryindustrial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.example.pz.quarryindustrial.drone.DroneEntity;
import org.example.pz.quarryindustrial.drone.DroneKind;

/** El dron en vuelo: hélices a toda velocidad, inclinado hacia donde va y con una sombra chica. */
public class DroneRenderer extends EntityRenderer<DroneEntity, DroneRenderer.State> {
    private final DroneModel model = new DroneModel();

    public static class State extends EntityRenderState {
        DroneKind kind = DroneKind.CARGO;
        boolean loaded;
        float yaw;
        float tilt;
    }

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DroneEntity drone, State state, float partialTick) {
        super.extractRenderState(drone, state, partialTick);
        state.kind = drone.kind();
        state.loaded = drone.loaded();
        state.yaw = drone.getYRot(partialTick);
        // Se inclina para adelante cuando avanza en horizontal
        double dx = drone.getX() - drone.xo;
        double dz = drone.getZ() - drone.zo;
        state.tilt = (float) Math.min(0.25, Math.sqrt(dx * dx + dz * dz) * 0.4);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotate(Axis.YP, (float) Math.toRadians(-state.yaw));
        poseStack.rotate(Axis.XP, state.tilt);
        model.submit(poseStack, collector, state.kind, state.loaded, state.ageInTicks * 1.6f, state.ageInTicks,
                state.lightCoords);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
