package org.example.pz.quarryindustrial.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Las piezas móviles de la quarry, armadas como {@link ModelPart} (lo mismo que exporta Blockbench como
 * "Modded Entity"). Unidades en píxeles (16 = un bloque) y, a diferencia de los modelos de entidades,
 * con el eje Y hacia arriba: el renderer las dibuja sin invertir la escala.
 *
 * <p>Todas usan la misma textura de 128×128 ({@code textures/entity/quarry_rig.png}), dividida en franjas
 * por material. Cada cubo toma su UV del comienzo de la franja de su material.
 */
public final class QuarryRigModels {
    public static final int TEXTURE_SIZE = 128;
    // Franjas de material en la textura (coordenada V donde empieza cada una)
    private static final int YELLOW = 0;
    private static final int STEEL = 32;
    private static final int DARK = 64;
    private static final int CARBIDE = 80;
    private static final int CONE = 96;
    private static final int LAMP = 112;

    private static final float TAU = (float) (Math.PI * 2);
    private static final float EIGHTH = (float) (Math.PI / 4);

    private QuarryRigModels() {
    }

    private static CubeListBuilder cubes(int v) {
        return CubeListBuilder.create().texOffs(0, v);
    }

    private static ModelPart bake(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE).bakeRoot();
    }

    /**
     * Prisma "octogonal": dos cajas iguales, una girada 45°. A esta escala se lee como un cilindro y
     * cuesta solo dos cubos.
     */
    private static void octagon(PartDefinition parent, String name, int material, float y, float radius,
                                float height) {
        float size = radius * 2;
        parent.addOrReplaceChild(name, cubes(material).addBox(-radius, y, -radius, size, height, size),
                PartPose.ZERO);
        parent.addOrReplaceChild(name + "_45", cubes(material).addBox(-radius, y, -radius, size, height, size),
                PartPose.rotation(0, EIGHTH, 0));
    }

    /** Un bloque de puente: viga cajón de 6×6 px a lo largo de X, con el riel del carro encima. */
    public static ModelPart bridgeSegment() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("beam", cubes(YELLOW).addBox(0, 0, -3, 16, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("rail_n", cubes(STEEL).addBox(0, 6, -2.5f, 16, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("rail_s", cubes(STEEL).addBox(0, 6, 1.5f, 16, 1, 1), PartPose.ZERO);
        return bake(mesh);
    }

    /** Carro de rodadura en cada punta del puente, apoyado sobre la viga del marco. */
    public static ModelPart bridgeTruck() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("block", cubes(YELLOW).addBox(-3, -1, -5, 6, 8, 10), PartPose.ZERO);
        root.addOrReplaceChild("wheel_n", cubes(DARK).addBox(-3.5f, -3, -4, 7, 3, 2), PartPose.ZERO);
        root.addOrReplaceChild("wheel_s", cubes(DARK).addBox(-3.5f, -3, 2, 7, 3, 2), PartPose.ZERO);
        return bake(mesh);
    }

    /**
     * Carro del cabezal: caja amarilla que abraza el puente, motor con aletas de refrigeración arriba,
     * mesa rotativa abajo (de donde cuelga el kelly) y una baliza.
     */
    public static ModelPart carriage() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", cubes(YELLOW).addBox(-7, -1, -7, 14, 8, 14), PartPose.ZERO);
        root.addOrReplaceChild("motor", cubes(DARK).addBox(-4, 7, -5, 8, 6, 10), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("fin_w" + i, cubes(DARK).addBox(-4.75f, 8, -4.5f + i * 2.5f, 0.75f, 4, 1.5f),
                    PartPose.ZERO);
            root.addOrReplaceChild("fin_e" + i, cubes(DARK).addBox(4, 8, -4.5f + i * 2.5f, 0.75f, 4, 1.5f),
                    PartPose.ZERO);
        }
        root.addOrReplaceChild("gearbox", cubes(STEEL).addBox(-3, 7, 5, 6, 4, 2), PartPose.ZERO);
        root.addOrReplaceChild("table", cubes(STEEL).addBox(-5, -3, -5, 10, 2, 10), PartPose.ZERO);
        root.addOrReplaceChild("table_45", cubes(STEEL).addBox(-5, -3, -5, 10, 2, 10),
                PartPose.rotation(0, EIGHTH, 0));
        return bake(mesh);
    }

    /** La baliza va aparte para dibujarla con brillo completo. */
    public static ModelPart beacon() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("lamp", cubes(LAMP).addBox(-1, 13, -1, 2, 2, 2), PartPose.ZERO);
        return bake(mesh);
    }

    /** Un bloque de tubería de perforación (drill pipe) con su tool joint arriba, que es más grueso. */
    public static ModelPart pipeSegment() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        octagon(root, "tube", STEEL, 0, 1.75f, 16);
        octagon(root, "joint", STEEL, 12.5f, 2.5f, 3.5f);
        return bake(mesh);
    }

    /** El kelly: la barra cuadrada que la mesa rotativa hace girar, justo debajo del carro. */
    public static ModelPart kelly() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bar", cubes(DARK).addBox(-2.5f, 0, -2.5f, 5, 16, 5), PartPose.ZERO);
        octagon(root, "bushing", STEEL, 13, 3.25f, 3);
        return bake(mesh);
    }

    /** Altura (px) y separación del eje donde va montado cada cono, en el marco de su pata. */
    public static final float CONE_MOUNT_Y = 5.25f;
    public static final float CONE_MOUNT_Z = 2.5f;
    /** Inclinación del eje del cono hacia el centro de la broca. */
    public static final float CONE_TILT = 0.78f;

    /**
     * Cuerpo de la broca tricónica (roller cone bit), de abajo hacia arriba en un bloque de alto:
     * pin roscado y hombro arriba, cuerpo octogonal, tres patas a 120° que bajan hacia afuera y tres
     * boquillas de lodo entre las patas. Los conos van aparte ({@link #cone()}) porque cada uno gira
     * sobre su propio eje inclinado; el renderer los coloca en la punta de cada pata.
     * El eje de la broca es el eje Y del modelo.
     */
    public static ModelPart triconeBody() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        octagon(root, "pin", STEEL, 13, 1.75f, 3);
        octagon(root, "shoulder", STEEL, 11.5f, 3.25f, 1.5f);
        octagon(root, "body", DARK, 8, 4.25f, 3.5f);
        for (int i = 0; i < 3; i++) {
            float yaw = i * TAU / 3;
            // La pata baja desde el cuerpo hacia afuera, en el lado +Z de su marco
            PartDefinition leg = root.addOrReplaceChild("leg" + i, CubeListBuilder.create(),
                    PartPose.rotation(0, yaw, 0));
            leg.addOrReplaceChild("shank", cubes(DARK).addBox(-1.75f, 5, 2.5f, 3.5f, 4, 2.25f),
                    PartPose.ZERO);
            leg.addOrReplaceChild("shirttail", cubes(DARK).addBox(-1.5f, 4, 3.25f, 3, 1.25f, 1.5f), PartPose.ZERO);
            // Boquilla de lodo, entre esta pata y la siguiente
            PartDefinition nozzle = root.addOrReplaceChild("nozzle" + i, CubeListBuilder.create(),
                    PartPose.rotation(0, yaw + TAU / 6, 0));
            nozzle.addOrReplaceChild("jet", cubes(DARK).addBox(-0.75f, 5.5f, 3.25f, 1.5f, 2.5f, 1.5f),
                    PartPose.ZERO);
        }
        return bake(mesh);
    }

    /**
     * Un cono de la broca, con el eje en -Y: base ancha arriba, punta abajo. Tres escalones que forman
     * el tronco de cono y una nariz de carburo. Los insertos van aparte ({@link #coneTeeth(int)}) para
     * teñirlos según el material de la broca y sacarlos a medida que se gastan.
     */
    public static ModelPart cone() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("c1", cubes(CONE).addBox(-3.0f, -1.75f, -3.0f, 6.0f, 1.75f, 6.0f), PartPose.ZERO);
        root.addOrReplaceChild("c2", cubes(CONE).addBox(-2.3f, -3.25f, -2.3f, 4.6f, 1.5f, 4.6f), PartPose.ZERO);
        root.addOrReplaceChild("c3", cubes(CONE).addBox(-1.5f, -4.5f, -1.5f, 3.0f, 1.25f, 3.0f), PartPose.ZERO);
        root.addOrReplaceChild("nose", cubes(CARBIDE).addBox(-0.6f, -5.25f, -0.6f, 1.2f, 0.75f, 1.2f),
                PartPose.ZERO);
        return bake(mesh);
    }

    /** Filas de insertos del cono, de afuera (0, la que más trabaja y primero se gasta) hacia la punta (2). */
    public static final int TEETH_ROWS = 3;

    public static ModelPart coneTeeth(int row) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        switch (row) {
            case 0 -> addTeeth(root, "row1", 3.25f, -1.4f, 0);
            case 1 -> addTeeth(root, "row2", 2.55f, -2.9f, EIGHTH);
            default -> addTeeth(root, "row3", 1.75f, -4.15f, 0);
        }
        return bake(mesh);
    }

    /**
     * Tolva colgada del costado del carro: ahí caen los drops antes de ir al buffer. Paredes de 1 px y un
     * embudo abajo; el contenido lo dibuja el renderer con el modelo del último bloque minado.
     */
    public static final float HOPPER_INNER_MIN_X = -4;
    public static final float HOPPER_INNER_MAX_X = 4;
    public static final float HOPPER_INNER_MIN_Z = 8;
    public static final float HOPPER_INNER_MAX_Z = 13;
    public static final float HOPPER_INNER_BOTTOM = 1;
    public static final float HOPPER_INNER_TOP = 8;

    public static ModelPart hopper() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bottom", cubes(DARK).addBox(-5, 0, 7, 10, 1, 7), PartPose.ZERO);
        root.addOrReplaceChild("wall_w", cubes(YELLOW).addBox(-5, 1, 7, 1, 8, 7), PartPose.ZERO);
        root.addOrReplaceChild("wall_e", cubes(YELLOW).addBox(4, 1, 7, 1, 8, 7), PartPose.ZERO);
        root.addOrReplaceChild("wall_s", cubes(YELLOW).addBox(-4, 1, 13, 8, 8, 1), PartPose.ZERO);
        root.addOrReplaceChild("lip", cubes(STEEL).addBox(-5.5f, 8.5f, 12.5f, 11, 1, 2), PartPose.ZERO);
        root.addOrReplaceChild("funnel", cubes(DARK).addBox(-2, -3, 8.5f, 4, 3, 4), PartPose.ZERO);
        root.addOrReplaceChild("chute", cubes(STEEL).addBox(-1, -5, 9.5f, 2, 2, 2), PartPose.ZERO);
        return bake(mesh);
    }

    /**
     * Broca PDC (polycrystalline diamond compact) del tier nuclear: cuerpo de acero con cinco aletas
     * radiales; cada aleta lleva una fila de cortadores de diamante en el borde de abajo y uno de calibre en
     * la punta exterior. Entre aletas, las boquillas. No tiene partes móviles: gira entera.
     */
    public static ModelPart pdcBit() {
        return pdcBit(-1);
    }

    /**
     * {@code cutters} = -1: el cuerpo (acero, aletas y boquillas). 0..3: un juego de cortadores, del de
     * calibre (0, el exterior, el primero que se gasta) a los de la nariz (3).
     */
    public static final int PDC_CUTTER_SETS = 4;

    public static ModelPart pdcBit(int cutters) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        if (cutters >= 0) {
            for (int i = 0; i < 5; i++) {
                PartDefinition blade = root.addOrReplaceChild("blade" + i, CubeListBuilder.create(),
                        PartPose.rotation(0, i * TAU / 5, 0));
                if (cutters == 0) {
                    blade.addOrReplaceChild("gauge", cubes(CARBIDE).addBox(-0.6f, 3.5f, 5.0f, 1.2f, 1.6f, 0.6f),
                            PartPose.ZERO);
                } else {
                    int c = 3 - cutters;
                    float r = 1.25f + c * 1.5f;
                    float y = 0.9f + c * 0.45f;
                    blade.addOrReplaceChild("cutter" + c, cubes(CARBIDE).addBox(-0.6f, y, r - 0.6f, 1.2f, 1.2f, 1.2f),
                            PartPose.ZERO);
                }
            }
            return bake(mesh);
        }
        octagon(root, "pin", STEEL, 13, 1.75f, 3);
        octagon(root, "shoulder", STEEL, 11.5f, 3.5f, 1.5f);
        octagon(root, "body", STEEL, 6, 3.25f, 5.5f);
        octagon(root, "nose", DARK, 1.5f, 1.5f, 4.5f);
        for (int i = 0; i < 5; i++) {
            float yaw = i * TAU / 5;
            PartDefinition blade = root.addOrReplaceChild("blade" + i, CubeListBuilder.create(),
                    PartPose.rotation(0, yaw, 0));
            // Aleta: más baja cerca del centro (perfil de "cono" de la cara de la broca)
            blade.addOrReplaceChild("web_inner", cubes(DARK).addBox(-0.9f, 1.5f, 0.75f, 1.8f, 9, 2.25f), PartPose.ZERO);
            blade.addOrReplaceChild("web_outer", cubes(DARK).addBox(-0.9f, 2.25f, 3, 1.8f, 8.25f, 2.25f), PartPose.ZERO);
            PartDefinition nozzle = root.addOrReplaceChild("nozzle" + i, CubeListBuilder.create(),
                    PartPose.rotation(0, yaw + TAU / 10, 0));
            nozzle.addOrReplaceChild("jet", cubes(STEEL).addBox(-0.6f, 4, 2.5f, 1.2f, 1.2f, 1.2f), PartPose.ZERO);
        }
        return bake(mesh);
    }

    /** Cubo apenas más chico que un bloque, para el holograma de las piezas que faltan del multibloque. */
    public static ModelPart ghostBlock() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", cubes(STEEL).addBox(1, 1, 1, 14, 14, 14), PartPose.ZERO);
        return bake(mesh);
    }

    /** Cuatro insertos de carburo alrededor del eje del cono, a la distancia {@code r}. */
    private static void addTeeth(PartDefinition cone, String name, float r, float y, float offset) {
        PartDefinition row = cone.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.rotation(0, offset, 0));
        float s = 1.1f;
        row.addOrReplaceChild("t0", cubes(CARBIDE).addBox(r - s / 2, y, -s / 2, s, s, s), PartPose.ZERO);
        row.addOrReplaceChild("t1", cubes(CARBIDE).addBox(-r - s / 2, y, -s / 2, s, s, s), PartPose.ZERO);
        row.addOrReplaceChild("t2", cubes(CARBIDE).addBox(-s / 2, y, r - s / 2, s, s, s), PartPose.ZERO);
        row.addOrReplaceChild("t3", cubes(CARBIDE).addBox(-s / 2, y, -r - s / 2, s, s, s), PartPose.ZERO);
    }
}
