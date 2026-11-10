package org.example.pz.quarryindustrial.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.example.pz.quarryindustrial.block.drone.DronePadStatus;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.menu.InfoKind;
import org.example.pz.quarryindustrial.menu.InfoMenu;

import java.util.List;

import static org.example.pz.quarryindustrial.client.screen.ModGui.*;

/**
 * Pantalla de solo lectura de las máquinas: título, y una fila por campo (estado, número, barra o posición),
 * dibujada sin textura con el mismo estilo que la de la quarry. El pad suma un botón para mandar a explorar.
 */
public class InfoScreen extends AbstractContainerScreen<InfoMenu> {
    private static final int WIDTH = InfoMenu.WIDTH;
    private static final int MARGIN = InfoMenu.MARGIN;
    private static final int TOP = InfoMenu.TOP;
    private static final int LINE = InfoMenu.LINE;
    private static final int BAR_ROW = InfoMenu.BAR_ROW;
    private static final int BAR_H = 6;
    private static final int BUTTON_H = InfoMenu.BUTTON_H;


    private Button action;
    /** Solo el pad: abre el panel con las mejoras instaladas, como ítems. */
    private Button toggle;
    private boolean upgradesOpen;

    private static final int SLOT = InfoMenu.SLOT;
    private static final int UPGRADE_COLUMNS = 6;
    private static final int UPGRADE_PANEL_W = UPGRADE_COLUMNS * SLOT + 8;
    /** Índices de los valores que manda el pad (ver DronePadBlockEntity.fillInfo). */
    private static final int V_CHUNK_LOADER = 17;
    private static final int V_SURVEY_UPGRADES = 18;
    private static final int V_SURVEY_MAX = 19;

    private Component toggleLabel() {
        return Component.translatable(upgradesOpen ? "gui.quarryindustrial.info.upgrades.close"
                : "gui.quarryindustrial.info.upgrades.open");
    }

    public InfoScreen(InfoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, InfoMenu.screenHeight(menu.kind()));
        if (menu.kind().hasInventory()) {
            inventoryLabelX = MARGIN;
            inventoryLabelY = InfoMenu.playerY(menu.kind()) - InfoMenu.LABEL;
        } else {
            inventoryLabelY = -1000;
        }
    }

    @Override
    protected void init() {
        super.init();
        if (menu.kind() == InfoKind.PAD) {
            toggle = addRenderableWidget(Button.builder(toggleLabel(), button -> {
                        upgradesOpen = !upgradesOpen;
                        button.setMessage(toggleLabel());
                    })
                    .bounds(leftPos + WIDTH + 2, topPos, 74, 16)
                    .build());
        }
        if (menu.kind().hasAction()) {
            action = addRenderableWidget(Button.builder(Component.translatable("gui.quarryindustrial.info.survey"),
                            button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                    .bounds(leftPos + MARGIN, topPos + InfoMenu.infoHeight(menu.kind()) - MARGIN - BUTTON_H,
                            WIDTH - MARGIN * 2, BUTTON_H)
                    .build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        panel(graphics, leftPos, topPos, WIDTH, imageHeight);
        if (menu.kind() == InfoKind.PAD && upgradesOpen) {
            drawUpgrades(graphics);
        }
        if (menu.kind().hasInventory()) {
            drawInventory(graphics);
        }
        InfoKind kind = menu.kind();
        int y = topPos + TOP;
        int x = leftPos + MARGIN;
        int right = leftPos + WIDTH - MARGIN;
        List<InfoKind.Field> fields = kind.fields();
        for (int i = 0; i < fields.size(); i++) {
            InfoKind.Field field = fields.get(i);
            if (kind.hidden(field)) {
                continue;
            }
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

    // ── Buzones del pad e inventario ──

    /** Casilleros hundidos debajo de cada slot y los rótulos de los buzones. */
    private void drawInventory(GuiGraphicsExtractor graphics) {
        InfoKind kind = menu.kind();
        graphics.fill(leftPos + MARGIN - 2, topPos + InfoMenu.infoHeight(kind) - 4,
                leftPos + WIDTH - MARGIN + 2, topPos + InfoMenu.infoHeight(kind) - 3, SHADOW);
        graphics.text(font, Component.translatable("gui.quarryindustrial.info.pad_out"), leftPos + MARGIN,
                topPos + InfoMenu.outboxY(kind) - InfoMenu.LABEL + 1, DIM, false);
        graphics.text(font, Component.translatable("gui.quarryindustrial.info.pad_in"), leftPos + MARGIN,
                topPos + InfoMenu.inboxY(kind) - InfoMenu.LABEL + 1, DIM, false);
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
    }

    // ── Panel desplegable de mejoras ──

    private int panelX() {
        return leftPos + WIDTH + 2;
    }

    private int panelY() {
        return topPos + 18;
    }

    private int surveyMax() {
        return (int) Math.max(1, menu.value(V_SURVEY_MAX));
    }

    private int surveyRows() {
        return (surveyMax() + UPGRADE_COLUMNS - 1) / UPGRADE_COLUMNS;
    }

    /** Alto del panel: la fila del cargador, la grilla de mejoras de alcance y el contador. */
    private int panelHeight() {
        return 4 + SLOT + 6 + surveyRows() * SLOT + 14;
    }

    private int chunkSlotX() {
        return panelX() + 4;
    }

    private int chunkSlotY() {
        return panelY() + 4;
    }

    private int surveySlotX(int index) {
        return panelX() + 4 + (index % UPGRADE_COLUMNS) * SLOT;
    }

    private int surveySlotY(int index) {
        return panelY() + 4 + SLOT + 6 + (index / UPGRADE_COLUMNS) * SLOT;
    }

    private void drawUpgrades(GuiGraphicsExtractor graphics) {
        sidePanel(graphics, leftPos, WIDTH, panelY(), UPGRADE_PANEL_W, panelHeight());
        drawSlot(graphics, chunkSlotX(), chunkSlotY(), new ItemStack(ModItems.PAD_CHUNK_LOADER),
                menu.value(V_CHUNK_LOADER) != 0);
        int installed = (int) menu.value(V_SURVEY_UPGRADES);
        for (int i = 0; i < surveyMax(); i++) {
            drawSlot(graphics, surveySlotX(i), surveySlotY(i), new ItemStack(ModItems.SURVEY_RANGE_UPGRADE), i < installed);
        }
        String count = installed + " / " + surveyMax();
        graphics.text(font, count, panelX() + 4, surveySlotY(surveyMax() - 1) + SLOT + 3, DIM, false);
    }

    /** Un casillero hundido; vacío muestra el ítem apagado, para ver qué falta instalar. */
    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack, boolean installed) {
        bevel(graphics, x, y, SLOT_SIZE, SLOT_SIZE, SLOT_FILL, SHADOW, LIGHT);
        graphics.item(stack, x + 1, y + 1);
        if (!installed) {
            graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xC08B8B8B);
        }
    }

    private void upgradeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack stack = null;
        boolean installed = false;
        if (isInside(mouseX, mouseY, chunkSlotX(), chunkSlotY())) {
            stack = new ItemStack(ModItems.PAD_CHUNK_LOADER);
            installed = menu.value(V_CHUNK_LOADER) != 0;
        } else {
            for (int i = 0; i < surveyMax(); i++) {
                if (isInside(mouseX, mouseY, surveySlotX(i), surveySlotY(i))) {
                    stack = new ItemStack(ModItems.SURVEY_RANGE_UPGRADE);
                    installed = i < menu.value(V_SURVEY_UPGRADES);
                    break;
                }
            }
        }
        if (stack != null) {
            graphics.setComponentTooltipForNextFrame(font, List.of(stack.getHoverName(),
                    Component.translatable(installed ? "gui.quarryindustrial.info.upgrade.installed"
                            : "gui.quarryindustrial.info.upgrade.free").withColor(installed ? 0xFF55FF55 : DIM)),
                    mouseX, mouseY);
        }
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
    }

    private void line(GuiGraphicsExtractor graphics, int left, int right, int y, Component label, Component value,
                      int valueColor) {
        graphics.text(font, label, left, y, DIM, false);
        graphics.text(font, value, right - font.width(value), y, valueColor, false);
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, long amount, long capacity,
                            int top, int bottom) {
        bevel(graphics, x - 1, y - 1, width + 2, BAR_H + 2, INSET, SHADOW, LIGHT);
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
        if (menu.kind() == InfoKind.PAD && upgradesOpen) {
            upgradeTooltip(graphics, mouseX, mouseY);
        }
    }

    /** Valor exacto al pasar el mouse por una barra o un número (los de la pantalla van abreviados). */
    private void extractValueTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        InfoKind kind = menu.kind();
        int y = TOP;
        for (int i = 0; i < kind.fields().size(); i++) {
            InfoKind.Field field = kind.fields().get(i);
            if (kind.hidden(field)) {
                continue;
            }
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

}
