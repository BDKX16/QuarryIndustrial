package org.example.pz.quarryindustrial.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Lo que comparten las pantallas del mod que se dibujan sin textura (la quarry, las máquinas de información y el
 * filtro de caño): la paleta gris de los inventarios, los biseles, los slots hundidos y el panel lateral pegado al
 * principal.
 */
public final class ModGui {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int LIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF555555;
    /** Fondo de un slot hundido. */
    public static final int SLOT_FILL = 0xFF8B8B8B;
    /** Fondo de las barras y mapas hundidos. */
    public static final int INSET = 0xFF1A1A1A;
    public static final int TEXT = 0xFF404040;
    public static final int DIM = 0xFF707070;

    /** Lado de un slot con su borde. */
    public static final int SLOT_SIZE = 18;
    /** Separación entre el panel principal y el lateral. */
    public static final int SIDE_GAP = 2;

    private ModGui() {
    }

    /** Rectángulo con borde claro arriba-izquierda y oscuro abajo-derecha (o al revés, para huecos). */
    public static void bevel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int topLeft,
                             int bottomRight) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w - 1, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h - 1, topLeft);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, bottomRight);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, bottomRight);
    }

    /** Panel levantado, como el fondo de un inventario. */
    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        bevel(graphics, x, y, w, h, PANEL, LIGHT, SHADOW);
    }

    /** Panel lateral pegado a la derecha del principal (que empieza en {@code mainLeft} y mide {@code mainWidth}). */
    public static void sidePanel(GuiGraphicsExtractor graphics, int mainLeft, int mainWidth, int y, int w, int h) {
        panel(graphics, mainLeft + mainWidth + SIDE_GAP, y, w, h);
    }

    /** Casillero hundido de un slot cuyo ítem se dibuja en ({@code x}, {@code y}). */
    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE, SLOT_FILL, SHADOW, LIGHT);
    }

    /** Hueco hundido oscuro, para barras de progreso y mapas. */
    public static void inset(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        bevel(graphics, x, y, w, h, INSET, SHADOW, LIGHT);
    }
}
