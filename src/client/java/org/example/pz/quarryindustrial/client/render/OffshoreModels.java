package org.example.pz.quarryindustrial.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Piezas de la plataforma offshore, con la textura del pórtico de la quarry ({@code quarry_rig.png}).
 * La torre en celosía se arma con "vigas": un cubo de 1 px que el renderer estira y orienta entre dos puntos.
 */
public final class OffshoreModels {
    static final int YELLOW = 0;
    static final int STEEL = 32;
    static final int DARK = 64;
    static final int LAMP = 112;

    private OffshoreModels() {
    }

    private static ModelPart bake(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, QuarryRigModels.TEXTURE_SIZE, QuarryRigModels.TEXTURE_SIZE).bakeRoot();
    }

    /** Cubo de 1×1×1 px con la base en el origen y el largo hacia +Y: la viga unitaria. */
    public static ModelPart unit(int material) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("beam", CubeListBuilder.create().texOffs(0, material)
                .addBox(-0.5f, 0, -0.5f, 1, 1, 1), PartPose.ZERO);
        return bake(mesh);
    }

    /** Bloque de corona (poleas fijas arriba de la torre). */
    public static ModelPart crownBlock() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, DARK).addBox(-9, 0, -9, 18, 4, 18),
                PartPose.ZERO);
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("sheave" + i, CubeListBuilder.create().texOffs(0, STEEL)
                    .addBox(-6 + i * 4.5f, 4, -4, 3, 6, 8), PartPose.ZERO);
        }
        root.addOrReplaceChild("beacon", CubeListBuilder.create().texOffs(0, LAMP).addBox(-1, 10, -1, 2, 2, 2),
                PartPose.ZERO);
        return bake(mesh);
    }

    /** Aparejo móvil (traveling block) con el gancho: sube y baja colgado de los cables. */
    public static ModelPart travelingBlock() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("block", CubeListBuilder.create().texOffs(0, YELLOW).addBox(-4, 0, -3, 8, 12, 6),
                PartPose.ZERO);
        root.addOrReplaceChild("hook", CubeListBuilder.create().texOffs(0, DARK).addBox(-1.5f, -6, -1.5f, 3, 6, 3),
                PartPose.ZERO);
        root.addOrReplaceChild("swivel", CubeListBuilder.create().texOffs(0, STEEL).addBox(-3, -10, -3, 6, 4, 6),
                PartPose.ZERO);
        return bake(mesh);
    }

    /** Casilla del perforador (doghouse) en una esquina de la cubierta. */
    public static ModelPart doghouse() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cabin", CubeListBuilder.create().texOffs(0, YELLOW).addBox(0, 0, 0, 32, 28, 26),
                PartPose.ZERO);
        root.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(0, DARK).addBox(-1, 28, -1, 34, 2, 28),
                PartPose.ZERO);
        root.addOrReplaceChild("window", CubeListBuilder.create().texOffs(0, LAMP).addBox(6, 14, -0.5f, 20, 8, 1),
                PartPose.ZERO);
        return bake(mesh);
    }

    /** Sala de máquinas (generadores diésel) con su escape. */
    public static ModelPart engineHouse() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("house", CubeListBuilder.create().texOffs(0, DARK).addBox(0, 0, 0, 30, 20, 28),
                PartPose.ZERO);
        root.addOrReplaceChild("stripe", CubeListBuilder.create().texOffs(0, YELLOW).addBox(-0.5f, 16, -0.5f, 31, 2, 29),
                PartPose.ZERO);
        root.addOrReplaceChild("stack", CubeListBuilder.create().texOffs(0, STEEL).addBox(6, 20, 6, 4, 14, 4),
                PartPose.ZERO);
        return bake(mesh);
    }
}
