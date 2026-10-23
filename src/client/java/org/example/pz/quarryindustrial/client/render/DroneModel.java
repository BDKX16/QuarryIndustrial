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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.util.LightCoordsUtil;
import org.example.pz.quarryindustrial.drone.DroneKind;

/**
 * Cuadricóptero de carga: cuerpo con una franja del color de su tipo, cuatro brazos en X con motores y hélices,
 * patines de aterrizaje, luces de navegación y, colgando abajo, lo que lleva según el tipo (cajón, tanque,
 * batería, brazo con garra o cámara). Lo usan el renderer de la entidad y el del pad (el dron posado).
 * Unidades en píxeles, Y hacia arriba, con la textura del pórtico de la quarry.
 */
public final class DroneModel {
    private static final int YELLOW = 0;
    private static final int STEEL = 32;
    private static final int DARK = 64;
    private static final int CARBIDE = 80;
    private static final int LAMP = 112;
    private static final float ARM = 6.5f;
    private static final float EIGHTH = (float) (Math.PI / 4);
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

    private final ModelPart frame = frame();
    private final ModelPart stripe = single(STEEL, -4.25f, 4.5f, -4.25f, 8.5f, 1, 8.5f);
    private final ModelPart rotor = rotor();
    private final ModelPart lamp = single(LAMP, -0.6f, 0, -0.6f, 1.2f, 1.2f, 1.2f);
    private final ModelPart crate = crate();
    private final ModelPart tank = tank();
    private final ModelPart battery = battery();
    private final ModelPart claw = claw();
    private final ModelPart camera = camera();

    private static ModelPart bake(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, QuarryRigModels.TEXTURE_SIZE, QuarryRigModels.TEXTURE_SIZE).bakeRoot();
    }

    private static CubeListBuilder cubes(int v) {
        return CubeListBuilder.create().texOffs(0, v);
    }

    private static ModelPart single(int material, float x, float y, float z, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", cubes(material).addBox(x, y, z, w, h, d), PartPose.ZERO);
        return bake(mesh);
    }

    private static ModelPart frame() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", cubes(DARK).addBox(-4, 2.5f, -4, 8, 2, 8), PartPose.ZERO);
        root.addOrReplaceChild("canopy", cubes(STEEL).addBox(-2.5f, 5.5f, -2.5f, 5, 1, 5), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            PartDefinition arm = root.addOrReplaceChild("arm" + i, CubeListBuilder.create(),
                    PartPose.rotation(0, EIGHTH + i * (float) (Math.PI / 2), 0));
            arm.addOrReplaceChild("beam", cubes(STEEL).addBox(-0.6f, 3.5f, 2, 1.2f, 1.2f, ARM * 1.414f - 2), PartPose.ZERO);
            arm.addOrReplaceChild("motor", cubes(DARK).addBox(-1.2f, 3, ARM * 1.414f - 1.2f, 2.4f, 2.5f, 2.4f),
                    PartPose.ZERO);
        }
        for (float side : new float[]{-3.5f, 2.5f}) {
            root.addOrReplaceChild("skid" + side, cubes(STEEL).addBox(side, 0, -4.5f, 1, 0.8f, 9), PartPose.ZERO);
            root.addOrReplaceChild("strut_f" + side, cubes(STEEL).addBox(side, 0.8f, -2.5f, 1, 1.7f, 0.8f), PartPose.ZERO);
            root.addOrReplaceChild("strut_b" + side, cubes(STEEL).addBox(side, 0.8f, 1.7f, 1, 1.7f, 0.8f), PartPose.ZERO);
        }
        return bake(mesh);
    }

    /** Hélice de dos palas, centrada en el eje del motor. */
    private static ModelPart rotor() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hub", cubes(STEEL).addBox(-0.5f, 0, -0.5f, 1, 0.8f, 1), PartPose.ZERO);
        root.addOrReplaceChild("blade_a", cubes(DARK).addBox(-4, 0.4f, -0.5f, 8, 0.3f, 1),
                PartPose.rotation(0.12f, 0, 0));
        return bake(mesh);
    }

    private static ModelPart crate() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("box", cubes(YELLOW).addBox(-3, -3.5f, -3, 6, 5, 6), PartPose.ZERO);
        root.addOrReplaceChild("band", cubes(DARK).addBox(-3.25f, -1.5f, -3.25f, 6.5f, 1, 6.5f), PartPose.ZERO);
        root.addOrReplaceChild("hook", cubes(STEEL).addBox(-0.5f, 1.5f, -0.5f, 1, 1.5f, 1), PartPose.ZERO);
        return bake(mesh);
    }

    private static ModelPart tank() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("shell", cubes(STEEL).addBox(-2.25f, -3, -4, 4.5f, 4.5f, 8), PartPose.ZERO);
        root.addOrReplaceChild("shell_45", cubes(STEEL).addBox(-2.25f, -3, -4, 4.5f, 4.5f, 8),
                PartPose.offsetAndRotation(0, -0.75f, 0, 0, 0, EIGHTH));
        root.addOrReplaceChild("valve", cubes(DARK).addBox(-0.75f, 1.5f, 2, 1.5f, 1, 1.5f), PartPose.ZERO);
        return bake(mesh);
    }

    private static ModelPart battery() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cell", cubes(DARK).addBox(-3, -3, -2.5f, 6, 4.5f, 5), PartPose.ZERO);
        root.addOrReplaceChild("plus", cubes(CARBIDE).addBox(-2.5f, 1.5f, -0.75f, 1.5f, 1, 1.5f), PartPose.ZERO);
        root.addOrReplaceChild("minus", cubes(CARBIDE).addBox(1, 1.5f, -0.75f, 1.5f, 1, 1.5f), PartPose.ZERO);
        return bake(mesh);
    }

    /** Brazo con garra del dron de mantenimiento (con la broca, cuando la lleva). */
    private static ModelPart claw() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("arm", cubes(STEEL).addBox(-0.75f, -3, -0.75f, 1.5f, 5.5f, 1.5f), PartPose.ZERO);
        for (int i = 0; i < 3; i++) {
            PartDefinition finger = root.addOrReplaceChild("finger" + i, CubeListBuilder.create(),
                    PartPose.rotation(0, i * (float) (Math.PI * 2 / 3), 0));
            finger.addOrReplaceChild("tip", cubes(DARK).addBox(-0.5f, -5, 0.8f, 1, 2.5f, 1),
                    PartPose.rotation(-0.35f, 0, 0));
        }
        return bake(mesh);
    }

    private static ModelPart camera() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("gimbal", cubes(STEEL).addBox(-0.75f, 0, -0.75f, 1.5f, 2.5f, 1.5f), PartPose.ZERO);
        root.addOrReplaceChild("head", cubes(DARK).addBox(-2, -2.5f, -2, 4, 3, 4), PartPose.ZERO);
        root.addOrReplaceChild("lens", cubes(LAMP).addBox(-1, -1.75f, -2.5f, 2, 1.5f, 0.6f), PartPose.ZERO);
        return bake(mesh);
    }

    /**
     * Dibuja el dron con el origen en el centro de los patines. {@code rotorAngle} gira las hélices (0 en el
     * pad); {@code loaded} cuelga la carga; las luces parpadean con {@code ageInTicks}.
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, DroneKind kind, boolean loaded,
                       float rotorAngle, float ageInTicks, int light) {
        RenderType type = RenderTypes.entityCutout(QuarryRenderer.TEXTURE);
        float px = 1 / 16f;
        submit(collector, frame, poseStack, type, light, 0xFFFFFFFF);
        submit(collector, stripe, poseStack, type, light, kind.color());
        for (int i = 0; i < 4; i++) {
            float angle = EIGHTH + i * (float) (Math.PI / 2);
            float x = (float) Math.sin(angle) * ARM * 1.414f * px;
            float z = (float) Math.cos(angle) * ARM * 1.414f * px;
            poseStack.pushPose();
            poseStack.translate(x, 5.5f * px, z);
            // Cada par gira para su lado, como en un cuadricóptero de verdad
            poseStack.rotate(Axis.YP, (i % 2 == 0 ? 1 : -1) * rotorAngle + i);
            submit(collector, rotor, poseStack, type, light, 0xFFFFFFFF);
            poseStack.popPose();
            // Luz de navegación en cada motor: adelante del color del tipo, atrás blanca; parpadea
            boolean on = ((int) ageInTicks / 10 + i) % 4 != 0;
            if (on) {
                poseStack.pushPose();
                poseStack.translate(x, 2.6f * px, z);
                submit(collector, lamp, poseStack, type, LightCoordsUtil.FULL_BRIGHT, i < 2 ? kind.color() : 0xFFFFFFFF);
                poseStack.popPose();
            }
        }
        ModelPart cargo = switch (kind) {
            case CARGO -> loaded ? crate : null;
            case TANKER -> loaded ? tank : null;
            case POWER -> loaded ? battery : null;
            case MAINTENANCE -> claw;
            case SURVEY -> camera;
        };
        if (cargo != null) {
            poseStack.pushPose();
            poseStack.translate(0, 1.2f * px, 0);
            submit(collector, cargo, poseStack, type, light, kind == DroneKind.TANKER || kind == DroneKind.POWER
                    ? kind.color() | 0xFF808080 : 0xFFFFFFFF);
            poseStack.popPose();
        }
    }

    private static void submit(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack, RenderType type,
                               int light, int tint) {
        collector.submitModelPart(part, poseStack, type, light, OverlayTexture.NO_OVERLAY, DIRECT_UV, tint);
    }
}
