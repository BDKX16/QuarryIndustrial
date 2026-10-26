package org.example.pz.quarryindustrial.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.example.pz.quarryindustrial.block.drone.DronePadStatus;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.menu.InfoKind;
import org.example.pz.quarryindustrial.menu.InfoMenu;

import java.util.List;

/**
 * Pantalla de solo lectura de las máquinas: título, y una fila por campo (estado, número, barra o posición),
 * dibujada sin textura con el mismo estilo que la de la quarry. El pad suma un botón para mandar a explorar.
 */
public class InfoScreen extends AbstractContainerScreen<InfoMenu> {
    private static final int WIDTH = 176;
    private static final int MARGIN = 8;
    private static final int TOP = 20;
    private static final int LINE = 11;
    private static final int BAR_ROW = 21;
    private static final int BAR_H = 6;
    private static final int BUTTON_H = 20;

    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int TEXT = 0xFF404040;
    private static final int DIM = 0xFF707070;

    private Button action;

    public InfoScreen(InfoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, height(menu.kind()));
        inventoryLabelY = -1000;
    }

    private static int height(InfoKind kind) {
        int height = TOP;
        for (InfoKind.Field field : kind.fields()) {
            height += field.type() == InfoKind.Type.BAR ? BAR_ROW : LINE;
        }
        return height + (kind.hasAction() ? BUTTON_H + 8 : 0) + MARGIN;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.kind().hasAction()) {
            action = addRenderableWidget(Button.builder(Component.translatable("gui.quarryindustrial.info.survey"),
                            button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                    .bounds(leftPos + MARGIN, topPos + imageHeight - MARGIN - BUTTON_H, WIDTH - MARGIN * 2, BUTTON_H)
                    .build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        bevel(graphics, leftPos, topPos, WIDTH, imageHeight, PANEL, LIGHT, SHADOW);
        InfoKind kind = menu.kind();
        int y = topPos + TOP;
        int x = leftPos + MARGIN;
        int right = leftPos + WIDTH - MARGIN;
        List<InfoKind.Field> fields = kind.fields();
        for (int i = 0; i < fields.size(); i++) {
            InfoKind.Field field = fields.get(i);
            int offset = kind.offset(i);
            switch (field.type()) {
                case CHOICE -> {
                    int index = (int) menu.value(offset);
                    boolean valid = index >= 0 && index < field.options().size();
                    int color = valid && field.colors() != null ? field.colors()[index] : TEXT;
                    line(graphics, x, right, y, field.label(), valid ? field.options().get(index) : Component.literal("?"),
                            color);
                    y += LINE;
                }
                case NUMBER -> {
                    String value = compact(menu.value(offset)) + (field.unit().isEmpty() ? "" : " " + field.unit());
                    line(graphics, x, right, y, field.label(), Component.literal(value), TEXT);
                    y += LINE;
                }
                case COORDS -> {
                    Component value = menu.value(offset) == 0 ? Component.literal("-")
                            : Component.literal((int) menu.value(offset + 1) + " " + (int) menu.value(offset + 2)
                            + " " + (int) menu.value(offset + 3));
                    line(graphics, x, right, y, field.label(), value, TEXT);
                    y += LINE;
                }
                case BAR -> {
                    long amount = menu.value(offset);
                    long capacity = menu.value(offset + 1);
                    String unit = field.unit().isEmpty() ? "" : " " + field.unit();
                    line(graphics, x, right, y, field.label(),
                            Component.literal(compact(amount) + " / " + compact(capacity) + unit), TEXT);
                    bar(graphics, x, y + LINE, right - x, amount, capacity, field.top(), field.bottom());
                    y += BAR_ROW;
                }
            }
        }
    }

    private void line(GuiGraphicsExtractor graphics, int left, int right, int y, Component label, Component value,
                      int valueColor) {
        graphics.text(font, label, left, y, DIM, false);
        graphics.text(font, value, right - font.width(value), y, valueColor, false);
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, long amount, long capacity,
                            int top, int bottom) {
        bevel(graphics, x - 1, y - 1, width + 2, BAR_H + 2, 0xFF1A1A1A, SHADOW, LIGHT);
        int filled = capacity <= 0 ? 0 : (int) Math.min(width, amount * width / capacity);
        if (filled > 0) {
            graphics.fillGradient(x, y, x + filled, y + BAR_H, top, bottom);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (action != null) {
            // Solo sirve con el explorador adentro y en tierra
            boolean survey = menu.value(1) == DroneKind.SURVEY.ordinal() + 1;
            action.visible = survey;
            action.active = survey && menu.value(0) != DronePadStatus.IN_FLIGHT.ordinal();
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractValueTooltip(graphics, mouseX, mouseY);
    }

    /** Valor exacto al pasar el mouse por una barra o un número (los de la pantalla van abreviados). */
    private void extractValueTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        InfoKind kind = menu.kind();
        int y = TOP;
        for (int i = 0; i < kind.fields().size(); i++) {
            InfoKind.Field field = kind.fields().get(i);
            int rowH = field.type() == InfoKind.Type.BAR ? BAR_ROW : LINE;
            if (isHovering(MARGIN, y, WIDTH - MARGIN * 2, rowH, mouseX, mouseY)) {
                int offset = kind.offset(i);
                if (field.type() == InfoKind.Type.BAR) {
                    graphics.setTooltipForNextFrame(font, Component.literal(menu.value(offset) + " / "
                            + menu.value(offset + 1) + (field.unit().isEmpty() ? "" : " " + field.unit())), mouseX, mouseY);
                } else if (field.type() == InfoKind.Type.NUMBER) {
                    graphics.setTooltipForNextFrame(font, Component.literal(Long.toString(menu.value(offset))),
                            mouseX, mouseY);
                }
                return;
            }
            y += rowH;
        }
    }

    /** 1234 → "1234", 12345 → "12.3k", 2000000 → "2.0M". */
    private static String compact(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 10_000) {
            return String.format("%.1fk", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private static void bevel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int topLeft,
                              int bottomRight) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w - 1, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h - 1, topLeft);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, bottomRight);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, bottomRight);
    }
}
