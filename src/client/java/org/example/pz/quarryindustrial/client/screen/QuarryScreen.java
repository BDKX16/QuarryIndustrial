package org.example.pz.quarryindustrial.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.example.pz.quarryindustrial.block.quarry.DrillHead;
import org.example.pz.quarryindustrial.block.quarry.QuarryArea;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryMenu;
import org.example.pz.quarryindustrial.block.quarry.QuarryStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de la quarry, dibujada sin textura (rectángulos con el estilo de los inventarios vanilla):
 * barra de energía, estado, entrada de EU/t, capa y avance, minimapa de la capa actual, upgrades y buffer.
 */
public class QuarryScreen extends AbstractContainerScreen<QuarryMenu> {
    /** Panel principal de 176 px (el ancho de un inventario) más el panel lateral de upgrades. */
    private static final int PANEL_WIDTH = 176;
    private static final int WIDTH = PANEL_WIDTH + 30;
    private static final int HEIGHT = 230;
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 18;
    private static final int ENERGY_W = 10;
    private static final int ENERGY_H = 50;
    /** Barra de agua (refrigerante) al lado de la de energía, y la de calor (solo nuclear) al lado. */
    private static final int COOLANT_X = 21;
    private static final int COOLANT_W = 5;
    private static final int HEAT_X = 28;
    private static final int HEAT_W = 3;
    private static final int TEXT_X = 34;
    private static final int MAP_X = 120;
    private static final int MAP_Y = 18;
    private static final int MAP_SIZE = 48;
    private static final int LINE = 10;
    private static final int PROGRESS_Y = 72;
    private static final int PROGRESS_W = 102;

    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int TEXT = 0xFF404040;

    public QuarryScreen(QuarryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        inventoryLabelY = QuarryMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        bevel(graphics, x, y, PANEL_WIDTH, HEIGHT, PANEL, LIGHT, SHADOW);
        // Panel lateral de upgrades, pegado al principal
        bevel(graphics, x + PANEL_WIDTH + 2, y + 10, WIDTH - PANEL_WIDTH - 2,
                QuarryMenu.BIT_Y + 18 + 4 - 10, PANEL, LIGHT, SHADOW);
        for (int i = 0; i < QuarryBlockEntity.UPGRADE_SLOTS; i++) {
            slot(graphics, x + QuarryMenu.UPGRADE_X, y + QuarryMenu.UPGRADE_Y + i * 18);
        }
        // Separador y slot de la broca (con una broca dibujada de fondo cuando está vacío)
        graphics.fill(x + QuarryMenu.BIT_X - 1, y + QuarryMenu.BIT_Y - 7, x + QuarryMenu.BIT_X + 17,
                y + QuarryMenu.BIT_Y - 6, SHADOW);
        slot(graphics, x + QuarryMenu.BIT_X, y + QuarryMenu.BIT_Y);
        if (menu.getSlot(QuarryBlockEntity.UPGRADE_SLOTS).getItem().isEmpty()) {
            int bx = x + QuarryMenu.BIT_X + 8;
            int by = y + QuarryMenu.BIT_Y + 2;
            graphics.fill(bx - 1, by, bx + 1, by + 6, 0xFF6E6E6E);
            graphics.fill(bx - 4, by + 6, bx + 4, by + 10, 0xFF6E6E6E);
            graphics.fill(bx - 2, by + 10, bx + 2, by + 13, 0xFF6E6E6E);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(graphics, x + QuarryMenu.BUFFER_X + col * 18, y + QuarryMenu.BUFFER_Y + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(graphics, x + 8 + col * 18, y + QuarryMenu.INVENTORY_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(graphics, x + 8 + col * 18, y + QuarryMenu.INVENTORY_Y + 58);
        }

        drawEnergy(graphics, x + ENERGY_X, y + ENERGY_Y);
        drawCoolant(graphics, x + COOLANT_X, y + ENERGY_Y);
        if (menu.ports() > 0 || isNuclear()) {
            drawHeat(graphics, x + HEAT_X, y + ENERGY_Y);
        }
        drawInfo(graphics, x + TEXT_X, y + ENERGY_Y);
        drawProgress(graphics, x + TEXT_X, y + PROGRESS_Y);
        drawMinimap(graphics, x + MAP_X, y + MAP_Y);
    }

    private void drawEnergy(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, ENERGY_W + 2, ENERGY_H + 2, 0xFF1A1A1A, SHADOW, LIGHT);
        long capacity = Math.max(1, menu.capacity());
        int filled = (int) Math.min(ENERGY_H, menu.energy() * ENERGY_H / capacity);
        // Rojo HV abajo, amarillo LV arriba: el mismo código de colores que los tiers de Ala
        graphics.fillGradient(x, y + ENERGY_H - filled, x + ENERGY_W, y + ENERGY_H, 0xFFFFD24A, 0xFFFF3D5A);
    }

    private boolean isNuclear() {
        QuarryBlockEntity quarry = menu.quarry();
        return quarry != null && quarry.tier().multiblock();
    }

    private void drawCoolant(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, COOLANT_W + 2, ENERGY_H + 2, 0xFF1A1A1A, SHADOW, LIGHT);
        int capacity = Math.max(1, menu.coolantCapacity());
        int filled = Math.min(ENERGY_H, menu.coolant() * ENERGY_H / capacity);
        graphics.fillGradient(x, y + ENERGY_H - filled, x + COOLANT_W, y + ENERGY_H, 0xFF6FB8FF, 0xFF2A5BD8);
    }

    /** Calor de la broca: verde → naranja → rojo; la marca blanca es donde empieza a frenar. */
    private void drawHeat(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, HEAT_W + 2, ENERGY_H + 2, 0xFF1A1A1A, SHADOW, LIGHT);
        int heat = Math.clamp(menu.heat(), 0, 1000);
        int filled = heat * ENERGY_H / 1000;
        int color = heat > 700 ? 0xFFFF3A2A : heat > 400 ? 0xFFFF9A2A : 0xFF4ACF5A;
        graphics.fill(x, y + ENERGY_H - filled, x + HEAT_W, y + ENERGY_H, color);
        graphics.fill(x, y + ENERGY_H * 3 / 10, x + HEAT_W, y + ENERGY_H * 3 / 10 + 1, 0xAAFFFFFF);
    }

    private void drawInfo(GuiGraphicsExtractor graphics, int x, int y) {
        QuarryStatus status = menu.status();
        graphics.text(font, status.label(), x, y, statusColor(status), false);
        graphics.text(font, Component.translatable("gui.quarryindustrial.input", compact(menu.input())),
                x, y + LINE, TEXT, false);
        graphics.text(font, Component.translatable("gui.quarryindustrial.energy", compact(menu.energy()),
                compact(menu.capacity())), x, y + LINE * 2, TEXT, false);
        Component layer = status == QuarryStatus.FINISHED
                ? Component.translatable("gui.quarryindustrial.done")
                : Component.translatable("gui.quarryindustrial.layer", menu.layer());
        graphics.text(font, layer, x, y + LINE * 3, TEXT, false);
        Component second = menu.ports() > 0
                ? Component.translatable("gui.quarryindustrial.ports", menu.ports())
                : Component.translatable("gui.quarryindustrial.area", menu.side(), menu.side());
        graphics.text(font, second, x, y + LINE * 4, TEXT, false);
    }

    /** Barra de avance a lo ancho, con el porcentaje a la derecha. */
    private void drawProgress(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, PROGRESS_W + 2, 7, 0xFF1A1A1A, SHADOW, LIGHT);
        graphics.fill(x, y, x + menu.progressPermille() * PROGRESS_W / 1000, y + 5, 0xFF3CE87A);
        graphics.text(font, (menu.progressPermille() / 10) + "%", x + PROGRESS_W + 4, y - 1, TEXT, false);
    }

    /**
     * Minimapa de la capa en curso, reducido al cuadro: gris = por minar, oscuro = ya pasado por el cabezal,
     * amarillo = cabezales. Se arma con el área y los cursores que ya tiene el cliente.
     */
    private void drawMinimap(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, MAP_SIZE + 2, MAP_SIZE + 2, 0xFF1A1A1A, SHADOW, LIGHT);
        QuarryBlockEntity quarry = menu.quarry();
        QuarryArea area = quarry != null ? quarry.area() : null;
        if (area == null || quarry.heads().isEmpty()) {
            graphics.fill(x, y, x + MAP_SIZE, y + MAP_SIZE, 0xFF2A2A2A);
            return;
        }
        int sizeX = area.maxX() - area.minX() + 1;
        int sizeZ = area.maxZ() - area.minZ() + 1;
        List<DrillHead> heads = quarry.heads();
        for (int py = 0; py < MAP_SIZE; py++) {
            for (int px = 0; px < MAP_SIZE; px++) {
                int cellX = area.minX() + px * sizeX / MAP_SIZE;
                int cellZ = area.minZ() + py * sizeZ / MAP_SIZE;
                graphics.fill(x + px, y + py, x + px + 1, y + py + 1, mined(heads, cellX, cellZ) ? 0xFF3B3328 : 0xFF8A8A8A);
            }
        }
        for (DrillHead head : heads) {
            if (head.isFinished()) {
                continue;
            }
            int hx = x + (head.cursor().getX() - area.minX()) * MAP_SIZE / sizeX;
            int hz = y + (head.cursor().getZ() - area.minZ()) * MAP_SIZE / sizeZ;
            graphics.fill(hx - 1, hz - 1, hx + 2, hz + 2, 0xFFFFD24A);
        }
    }

    /** Si el cabezal dueño de esa columna ya pasó por ahí en la capa actual. */
    private static boolean mined(List<DrillHead> heads, int x, int z) {
        for (DrillHead head : heads) {
            if (x < head.minX() || x > head.maxX()) {
                continue;
            }
            if (head.isFinished()) {
                return true;
            }
            int cursorX = head.cursor().getX();
            int cursorZ = head.cursor().getZ();
            return z < cursorZ || (z == cursorZ && x < cursorX);
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.quarryindustrial.energy_tooltip",
                    menu.energy(), menu.capacity()), mouseX, mouseY);
        } else if (isHovering(COOLANT_X, ENERGY_Y, COOLANT_W, ENERGY_H, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.quarryindustrial.coolant", menu.coolant(), menu.coolantCapacity()),
                    Component.translatable("gui.quarryindustrial.coolant_hint")), mouseX, mouseY);
        } else if (isNuclear() && isHovering(HEAT_X, ENERGY_Y, HEAT_W, ENERGY_H, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.quarryindustrial.heat", menu.heat() / 10),
                    Component.translatable("gui.quarryindustrial.heat_hint")), mouseX, mouseY);
        } else if (menu.ports() > 0 && isHovering(TEXT_X, ENERGY_Y + LINE, MAP_X - TEXT_X, LINE, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.quarryindustrial.ports_tooltip"));
            for (int i = 0; i < Math.min(menu.ports(), QuarryMenu.MAX_PORTS_SHOWN); i++) {
                lines.add(Component.translatable("gui.quarryindustrial.port_line", i + 1, menu.portInput(i)));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private static int statusColor(QuarryStatus status) {
        return switch (status) {
            case MINING -> 0xFF1E8A3C;
            case BUILDING_FRAME, OVERHEATING -> 0xFFB07A00;
            case NO_POWER, STRUCTURE_INCOMPLETE, NO_BIT -> 0xFFB02020;
            case OUTPUT_FULL -> 0xFFC05A00;
            case FINISHED -> 0xFF2050B0;
            case IDLE -> TEXT;
        };
    }

    /** 1234 → "1.2k", 2000000 → "2.0M". */
    private static String compact(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 10_000) {
            return String.format("%.1fk", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, 18, 18, SLOT, SHADOW, LIGHT);
    }

    /** Rectángulo con borde claro arriba-izquierda y oscuro abajo-derecha (o al revés, para huecos). */
    private static void bevel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int topLeft,
                              int bottomRight) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w - 1, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h - 1, topLeft);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, bottomRight);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, bottomRight);
    }
}
