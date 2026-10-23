package org.example.pz.quarryindustrial.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Piezas de la cigüeña (pumpjack), en píxeles con Y hacia arriba, igual que {@link QuarryRigModels}.
 *
 * <p>Marco local: X lateral, Z hacia atrás (de la cabeza de pozo al motor). La cabeza de pozo está en el
 * origen (centro del bloque, al nivel del suelo); la estructura ocupa z −8..104 (siete bloques).
 *
 * <p>Geometría del mecanismo (la usa {@link PumpjackKinematics}):
 * pivote del balancín en {@link #PIVOT_Y}/{@link #PIVOT_Z}; brazo delantero (hasta la cabeza de caballo)
 * {@link #FRONT_ARM}, brazo trasero {@link #REAR_ARM}; eje de las manivelas en {@link #CRANK_Y}/{@link #CRANK_Z}
 * con radio {@link #CRANK_RADIUS}; bielas de largo {@link #PITMAN_LENGTH}.
 */
public final class PumpjackModels {
    public static final float PIVOT_Y = 66;
    public static final float PIVOT_Z = 48;
    public static final float FRONT_ARM = 48;
    public static final float REAR_ARM = 32;
    public static final float CRANK_Y = 26;
    public static final float CRANK_Z = 80;
    public static final float CRANK_RADIUS = 6;
    /** Largo de la biela: el que deja el balancín horizontal con la manivela arriba. */
    public static final float PITMAN_LENGTH = PIVOT_Y - (CRANK_Y + CRANK_RADIUS);
    /** Lateral de las dos bielas y manivelas. */
    public static final float PITMAN_X = 10;
    /** Altura de la barra portadora con el balancín horizontal. */
    public static final float CARRIER_REST_Y = 40;
    /** Parte de arriba del prensaestopas, donde el vástago entra al pozo. */
    public static final float STUFFING_BOX_TOP = 13;

    // Franjas de material de la textura de 128×256 (textures/entity/pumpjack.png)
    private static final int PAINT = 0;
    private static final int YELLOW = 96;
    private static final int DARK = 128;
    private static final int STEEL = 160;
    private static final int BLACK = 192;
    private static final int GREEN = 224;
    public static final int TEXTURE_WIDTH = 128;
    public static final int TEXTURE_HEIGHT = 256;

    private PumpjackModels() {
    }

    private static CubeListBuilder cubes(int v) {
        return CubeListBuilder.create().texOffs(0, v);
    }

    private static ModelPart bake(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT).bakeRoot();
    }

    private static void octagon(PartDefinition parent, String name, int material, float y, float radius,
                                float height) {
        float size = radius * 2;
        parent.addOrReplaceChild(name, cubes(material).addBox(-radius, y, -radius, size, height, size),
                PartPose.ZERO);
        parent.addOrReplaceChild(name + "_45", cubes(material).addBox(-radius, y, -radius, size, height, size),
                PartPose.rotation(0, (float) (Math.PI / 4), 0));
    }

    /**
     * Una barra recta de sección {@code t}×{@code t} entre dos puntos. Alinea el eje Y de la caja con la
     * dirección {@code to - from}: primero inclina en X y después en Z (ModelPart aplica X, Y, Z en ese orden).
     */
    private static void strut(PartDefinition parent, String name, int material, float t,
                              float fx, float fy, float fz, float tx, float ty, float tz) {
        float dx = tx - fx;
        float dy = ty - fy;
        float dz = tz - fz;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float xRot = (float) Math.asin(dz / length);
        float zRot = (float) Math.atan2(-dx, dy);
        parent.addOrReplaceChild(name, cubes(material).addBox(-t / 2, 0, -t / 2, t, length, t),
                PartPose.offsetAndRotation(fx, fy, fz, xRot, 0, zRot));
    }

    /** Todo lo que no se mueve: patín, cabeza de pozo, poste Samson, reductor, motor y guardacorrea. */
    public static ModelPart frame() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Patín: dos vigas a lo largo, en tramos para que la UV entre en la textura, y travesaños
        for (int i = 0; i < 4; i++) {
            float z = -8 + i * 28;
            root.addOrReplaceChild("skid_l" + i, cubes(DARK).addBox(-12, 0, z, 4, 4, 28), PartPose.ZERO);
            root.addOrReplaceChild("skid_r" + i, cubes(DARK).addBox(8, 0, z, 4, 4, 28), PartPose.ZERO);
        }
        for (float z : new float[]{12, 40, 68, 98}) {
            root.addOrReplaceChild("cross" + (int) z, cubes(DARK).addBox(-8, 0, z - 2, 16, 3, 4), PartPose.ZERO);
        }

        // Cabeza de pozo: cabezal del casing, brida, cabezal de tubing, prensaestopas y la línea de flujo
        octagon(root, "casing_head", STEEL, 0, 4, 5);
        octagon(root, "flange", DARK, 5, 5, 1);
        octagon(root, "tubing_head", STEEL, 6, 3, 3);
        octagon(root, "stuffing_box", DARK, 9, 2, STUFFING_BOX_TOP - 9);
        root.addOrReplaceChild("flowline", cubes(STEEL).addBox(2, 6.5f, -1.5f, 9, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("valve", cubes(YELLOW).addBox(6, 9.5f, -2, 2, 2, 4), PartPose.ZERO);

        // Poste Samson: cuatro patas inclinadas hasta el rodamiento del balancín, con riostras
        float topY = PIVOT_Y - 4;
        strut(root, "leg_fl", PAINT, 3, -10, 4, PIVOT_Z - 18, -2.5f, topY, PIVOT_Z - 2);
        strut(root, "leg_fr", PAINT, 3, 10, 4, PIVOT_Z - 18, 2.5f, topY, PIVOT_Z - 2);
        strut(root, "leg_bl", PAINT, 3, -10, 4, PIVOT_Z + 18, -2.5f, topY, PIVOT_Z + 2);
        strut(root, "leg_br", PAINT, 3, 10, 4, PIVOT_Z + 18, 2.5f, topY, PIVOT_Z + 2);
        root.addOrReplaceChild("brace_f", cubes(PAINT).addBox(-7, 28, PIVOT_Z - 11, 14, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("brace_b", cubes(PAINT).addBox(-7, 28, PIVOT_Z + 9, 14, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("saddle", cubes(DARK).addBox(-4, topY, PIVOT_Z - 4, 8, 3, 8), PartPose.ZERO);

        // Reductor sobre su base, con el eje de las manivelas saliendo a los costados
        root.addOrReplaceChild("gear_base", cubes(DARK).addBox(-9, 3, CRANK_Z - 10, 18, 1, 22), PartPose.ZERO);
        root.addOrReplaceChild("gearbox", cubes(PAINT).addBox(-7, 4, CRANK_Z - 8, 14, CRANK_Y - 1, 18),
                PartPose.ZERO);
        root.addOrReplaceChild("crank_shaft", cubes(STEEL)
                .addBox(-PITMAN_X - 1, CRANK_Y - 1.5f, CRANK_Z - 1.5f, (PITMAN_X + 1) * 2, 3, 3), PartPose.ZERO);

        // Motor eléctrico atrás y la guarda de la correa hasta el reductor
        root.addOrReplaceChild("motor", cubes(GREEN).addBox(-5, 4, 92, 10, 10, 11), PartPose.ZERO);
        root.addOrReplaceChild("motor_box", cubes(DARK).addBox(5, 6, 95, 2, 4, 4), PartPose.ZERO);
        strut(root, "belt_guard", GREEN, 4, 8, 6, 97, 8, CRANK_Y - 4, CRANK_Z + 4);
        return bake(mesh);
    }

    /**
     * El balancín con la cabeza de caballo, en coordenadas del pivote (el renderer lo gira en X).
     * La cabeza de caballo es un arco centrado en el pivote: así el cable cuelga siempre vertical sobre el
     * pozo, que es justamente para lo que tiene esa forma.
     */
    public static ModelPart walkingBeam() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 4; i++) {
            float z = -FRONT_ARM + 4 + i * 20;
            root.addOrReplaceChild("beam" + i, cubes(PAINT).addBox(-2, -3, z, 4, 6, Math.min(20, REAR_ARM + 2 - z)),
                    PartPose.ZERO);
        }
        root.addOrReplaceChild("flange_top", cubes(DARK).addBox(-3, 3, -FRONT_ARM + 6, 6, 1, 20), PartPose.ZERO);
        root.addOrReplaceChild("flange_top2", cubes(DARK).addBox(-3, 3, -FRONT_ARM + 26, 6, 1, 20), PartPose.ZERO);
        root.addOrReplaceChild("flange_top3", cubes(DARK).addBox(-3, 3, -FRONT_ARM + 46, 6, 1, 20), PartPose.ZERO);
        root.addOrReplaceChild("flange_top4", cubes(DARK).addBox(-3, 3, -FRONT_ARM + 66, 6, 1, 14), PartPose.ZERO);
        // Barra igualadora en la cola, donde se enganchan las dos bielas
        root.addOrReplaceChild("equalizer", cubes(DARK).addBox(-PITMAN_X - 1, -2, REAR_ARM - 2, (PITMAN_X + 1) * 2, 4, 4),
                PartPose.ZERO);
        root.addOrReplaceChild("tail_bearing", cubes(STEEL).addBox(-3, -4, REAR_ARM - 3, 6, 2, 6), PartPose.ZERO);

        // Cabeza de caballo: cinco dovelas sobre un arco de radio FRONT_ARM, ±18° (cubre el recorrido de ±22°
        // del balancín porque el cable cuelga del punto del arco que queda a la altura del pivote)
        for (int i = -2; i <= 2; i++) {
            float beta = (float) Math.toRadians(i * 9);
            float y = (float) (FRONT_ARM * Math.sin(beta));
            float z = (float) (-FRONT_ARM * Math.cos(beta));
            root.addOrReplaceChild("horsehead" + (i + 2), cubes(YELLOW).addBox(-4.5f, -4, 0, 9, 8, 6),
                    PartPose.offsetAndRotation(0, y, z, beta, 0, 0));
        }
        root.addOrReplaceChild("horsehead_neck", cubes(YELLOW).addBox(-3.5f, -7, -FRONT_ARM + 5, 7, 14, 8),
                PartPose.ZERO);
        return bake(mesh);
    }

    /** Una manivela con su contrapeso; el pasador de la biela queda a CRANK_RADIUS sobre el eje. */
    public static ModelPart crank() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("arm", cubes(DARK).addBox(-1.5f, -13, -3, 3, 13 + CRANK_RADIUS + 2, 6), PartPose.ZERO);
        root.addOrReplaceChild("counterweight", cubes(BLACK).addBox(-2.5f, -17, -8, 5, 9, 16), PartPose.ZERO);
        root.addOrReplaceChild("wrist_pin", cubes(STEEL).addBox(-2.5f, CRANK_RADIUS - 1, -1, 5, 2, 2), PartPose.ZERO);
        return bake(mesh);
    }

    /** Biela: barra de PITMAN_LENGTH a lo largo de +Y desde el pasador de la manivela. */
    public static ModelPart pitman() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("rod", cubes(PAINT).addBox(-1, 0, -1, 2, PITMAN_LENGTH, 2), PartPose.ZERO);
        return bake(mesh);
    }

    /** Un píxel de largo en Y: el renderer lo estira para el cable y el vástago. */
    public static ModelPart cable() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("wire", cubes(STEEL).addBox(-0.3f, 0, -0.3f, 0.6f, 1, 0.6f), PartPose.ZERO);
        return bake(mesh);
    }

    public static ModelPart polishedRod() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("rod", cubes(STEEL).addBox(-0.6f, 0, -0.6f, 1.2f, 1, 1.2f), PartPose.ZERO);
        return bake(mesh);
    }

    /** Barra portadora con la grampa del vástago. */
    public static ModelPart carrierBar() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bar", cubes(DARK).addBox(-4, -1, -1, 8, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("clamp", cubes(STEEL).addBox(-1.25f, 1, -1.25f, 2.5f, 2, 2.5f), PartPose.ZERO);
        return bake(mesh);
    }
}
