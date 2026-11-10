package org.example.pz.quarryindustrial.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.filter.PipeFilterData;
import org.example.pz.quarryindustrial.filter.PipeFilterMenu;
import org.example.pz.quarryindustrial.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

import static org.example.pz.quarryindustrial.client.screen.ModGui.*;

/**
 * Pantalla del filtro de caño, dibujada sin textura con el mismo estilo que las demás del mod: los casilleros
 * (uno solo hasta instalar la expansión), dos botones (lista blanca/negra, ítem/tag) y, en un panel a la derecha,
 * el slot de la expansión.
 */
public class PipeFilterScreen extends AbstractContainerScreen<PipeFilterMenu> {

    private static final int SIDE_Y = PipeFilterMenu.UPGRADE_Y - 18;
    private static final int SIDE_W = 62;
    private static final int SIDE_H = 46;

    // Vista previa de la categoría: grilla de íconos de los ítems que entran, bajo el panel de la expansión
    private static final int PREVIEW_Y = SIDE_Y + SIDE_H + 4;
    private static final int PREVIEW_COLS = 3;
    private static final int PREVIEW_ROWS = 5;
    private static final int PREVIEW_HEADER = 14;

    private Button mode;
    private TagKey<Item> previewTag;
    private List<ItemStack> previewItems = List.of();
    private int previewScroll;
    private int previewCell = -1;
    private Button match;
    private String tooltipState;

    public PipeFilterScreen(PipeFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PipeFilterMenu.WIDTH, PipeFilterMenu.HEIGHT);
        inventoryLabelY = PipeFilterMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(modeLabel(), button ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PipeFilterMenu.BUTTON_MODE))
                .bounds(leftPos + 8, topPos + 54, 78, 20)
                .build());
        match = addRenderableWidget(Button.builder(matchLabel(), button ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PipeFilterMenu.BUTTON_MATCH))
                .bounds(leftPos + 90, topPos + 54, 78, 20)
                .build());
    }

    /** Con un solo casillero no es una lista: se dice «Permitir» o «Denegar», y «lista» recién con la expansión. */
    private String modeKey() {
        String base = menu.allowList() ? "allow" : "deny";
        return "gui.quarryindustrial.pipe_filter." + base + (menu.activeCells() > 1 ? "_list" : "_one");
    }

    private Component modeLabel() {
        return Component.translatable(modeKey());
    }

    private Component matchLabel() {
        return Component.translatable(menu.tagMatch() ? "gui.quarryindustrial.pipe_filter.match_tag"
                : "gui.quarryindustrial.pipe_filter.match_item");
    }

    /** Qué hace cada botón en su estado actual, con un ejemplo en el caso de la categoría. */
    private void updateTooltips() {
        // Solo al cambiar de estado: recrear el tooltip en cada cuadro le reiniciaría la espera al pasar el mouse
        String key = modeKey() + menu.tagMatch();
        if (key.equals(tooltipState)) {
            return;
        }
        tooltipState = key;
        mode.setTooltip(Tooltip.create(Component.translatable(modeKey() + ".tooltip")));
        match.setTooltip(Tooltip.create(Component.translatable(menu.tagMatch()
                ? "gui.quarryindustrial.pipe_filter.match_tag.tooltip"
                : "gui.quarryindustrial.pipe_filter.match_item.tooltip")));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        // Panel lateral de la expansión
        sidePanel(graphics, leftPos, PipeFilterMenu.WIDTH, topPos + SIDE_Y, SIDE_W, SIDE_H);
        graphics.text(font, Component.translatable("gui.quarryindustrial.pipe_filter.upgrade_short"),
                leftPos + PipeFilterMenu.WIDTH + SIDE_GAP + 4, topPos + SIDE_Y + 4, DIM, false);
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                slot(graphics, leftPos + slot.x, topPos + slot.y);
            }
        }
        // Slot de la expansión vacío: se ve el ítem apagado, para saber qué va
        Slot upgrade = menu.slots.get(PipeFilterData.UPGRADE_SLOT);
        if (!upgrade.hasItem()) {
            graphics.item(new ItemStack(ModItems.PIPE_FILTER_UPGRADE), leftPos + upgrade.x, topPos + upgrade.y);
            graphics.fill(leftPos + upgrade.x, topPos + upgrade.y, leftPos + upgrade.x + 16, topPos + upgrade.y + 16,
                    0xC08B8B8B);
        }
        updatePreview(mouseX, mouseY);
        if (previewCell >= 0) {
            drawPreview(graphics);
        }
        // Marca de "tag" en los casilleros que comparan por categoría
        if (menu.tagMatch()) {
            for (int i = 0; i < menu.activeCells(); i++) {
                if (!menu.filter().getItem(i).isEmpty()) {
                    graphics.text(font, "#", leftPos + PipeFilterMenu.cellX(i) + 1,
                            topPos + PipeFilterMenu.CELL_Y + 1, 0xFF55FFFF, true);
                }
            }
        }
        Component state = status();
        graphics.text(font, state, leftPos + PipeFilterMenu.WIDTH / 2 - font.width(state) / 2,
                topPos + 78, TEXT, false);
    }

    private Component status() {
        if (!menu.filter().isConfigured()) {
            return Component.translatable(menu.allowList() ? "gui.quarryindustrial.pipe_filter.empty_allow"
                    : "gui.quarryindustrial.pipe_filter.empty_deny");
        }
        return Component.empty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        mode.setMessage(modeLabel());
        match.setMessage(matchLabel());
        updateTooltips();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractCellTooltip(graphics, mouseX, mouseY);
        extractPreviewTooltip(graphics, mouseX, mouseY);
        extractUpgradeTooltip(graphics, mouseX, mouseY);
    }

    private void extractUpgradeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot upgrade = menu.slots.get(PipeFilterData.UPGRADE_SLOT);
        if (!upgrade.hasItem() && menu.getCarried().isEmpty() && isHovering(upgrade.x, upgrade.y, 16, 16, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("item.quarryindustrial.pipe_filter_upgrade"),
                    Component.translatable("gui.quarryindustrial.pipe_filter.upgrade_hint").withColor(DIM)),
                    mouseX, mouseY);
        }
    }

    // ── Vista previa de la categoría ──

    /**
     * Qué casillero se previsualiza: el que está bajo el mouse si está marcado, y si no el primero marcado. Solo en
     * modo categoría; en modo ítem no hay nada que mostrar.
     */
    private void updatePreview(int mouseX, int mouseY) {
        previewCell = -1;
        if (!menu.tagMatch()) {
            return;
        }
        int first = -1;
        for (int i = 0; i < menu.activeCells(); i++) {
            if (menu.filter().getItem(i).isEmpty()) {
                continue;
            }
            first = first < 0 ? i : first;
            if (isHovering(PipeFilterMenu.cellX(i), PipeFilterMenu.CELL_Y, 16, 16, mouseX, mouseY)) {
                previewCell = i;
                break;
            }
        }
        if (previewCell < 0) {
            previewCell = first;
        }
        TagKey<Item> tag = null;
        if (previewCell >= 0) {
            List<TagKey<Item>> tags = PipeFilterData.tagsOf(menu.filter().getItem(previewCell).getItem());
            tag = tags.isEmpty() ? null : tags.get(Math.floorMod(menu.tagIndex(previewCell), tags.size()));
        }
        if (tag == null) {
            previewCell = -1;
            previewTag = null;
            return;
        }
        if (!tag.equals(previewTag)) {
            previewTag = tag;
            previewScroll = 0;
            List<ItemStack> items = new ArrayList<>();
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                items.add(new ItemStack(holder.value()));
            }
            previewItems = items;
        }
    }

    private int previewPanelHeight() {
        int rows = Math.min(PREVIEW_ROWS, (previewItems.size() + PREVIEW_COLS - 1) / PREVIEW_COLS);
        return PREVIEW_HEADER + rows * SLOT_SIZE + (previewItems.size() > PREVIEW_COLS * PREVIEW_ROWS ? 12 : 4);
    }

    private int previewX() {
        return leftPos + PipeFilterMenu.WIDTH + SIDE_GAP;
    }

    private int previewIconX(int col) {
        return previewX() + (SIDE_W - PREVIEW_COLS * SLOT_SIZE) / 2 + 1 + col * SLOT_SIZE;
    }

    private int previewIconY(int row) {
        return topPos + PREVIEW_Y + PREVIEW_HEADER + 1 + row * SLOT_SIZE;
    }

    private int maxPreviewScroll() {
        int rows = (previewItems.size() + PREVIEW_COLS - 1) / PREVIEW_COLS;
        return Math.max(0, rows - PREVIEW_ROWS);
    }

    private int previewShown() {
        return Math.min(PREVIEW_COLS * PREVIEW_ROWS, previewItems.size() - previewScroll * PREVIEW_COLS);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        sidePanel(graphics, leftPos, PipeFilterMenu.WIDTH, topPos + PREVIEW_Y, SIDE_W, previewPanelHeight());
        graphics.text(font, Component.translatable("gui.quarryindustrial.pipe_filter.preview", previewItems.size()),
                previewX() + 4, topPos + PREVIEW_Y + 4, DIM, false);
        int shown = previewShown();
        for (int i = 0; i < shown; i++) {
            int x = previewIconX(i % PREVIEW_COLS);
            int y = previewIconY(i / PREVIEW_COLS);
            slot(graphics, x, y);
            graphics.item(previewItems.get(previewScroll * PREVIEW_COLS + i), x, y);
        }
        if (previewItems.size() > PREVIEW_COLS * PREVIEW_ROWS) {
            int from = previewScroll * PREVIEW_COLS + 1;
            graphics.text(font, Component.translatable("gui.quarryindustrial.pipe_filter.preview_range", from,
                            from + shown - 1, previewItems.size()),
                    previewX() + 4, topPos + PREVIEW_Y + previewPanelHeight() - 11, DIM, false);
        }
    }

    /** Nombre del ítem de la vista previa que está bajo el mouse. */
    private void extractPreviewTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (previewCell < 0) {
            return;
        }
        for (int i = 0; i < previewShown(); i++) {
            int x = previewIconX(i % PREVIEW_COLS);
            int y = previewIconY(i / PREVIEW_COLS);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.setTooltipForNextFrame(font, previewItems.get(previewScroll * PREVIEW_COLS + i).getHoverName(),
                        mouseX, mouseY);
                return;
            }
        }
    }

    /** La rueda sobre la vista previa pasa de a una fila de íconos. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (previewCell >= 0 && mouseX >= previewX() && mouseX < previewX() + SIDE_W && mouseY >= topPos + PREVIEW_Y
                && mouseY < topPos + PREVIEW_Y + previewPanelHeight()) {
            previewScroll = Math.max(0, Math.min(maxPreviewScroll(), previewScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Al pasar por un casillero: qué se compara (el ítem o el tag elegido) y cómo cambiarlo. */
    private void extractCellTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = 0; i < menu.activeCells(); i++) {
            if (!isHovering(PipeFilterMenu.cellX(i), PipeFilterMenu.CELL_Y, 16, 16, mouseX, mouseY)) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            ItemStack sample = menu.filter().getItem(i);
            if (sample.isEmpty()) {
                lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.cell_empty"));
            } else if (menu.tagMatch()) {
                List<TagKey<Item>> tags = PipeFilterData.tagsOf(sample.getItem());
                if (tags.isEmpty()) {
                    lines.add(sample.getHoverName());
                    lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.no_tags").withColor(DIM));
                } else {
                    int index = Math.floorMod(menu.tagIndex(i), tags.size());
                    lines.add(Component.literal("#" + tags.get(index).location()).withColor(0xFF55FFFF));
                    lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.tag_count", index + 1, tags.size())
                            .withColor(DIM));
                    lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.cycle").withColor(DIM));
                    lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.see_matches").withColor(DIM));
                }
            } else {
                lines.add(sample.getHoverName());
                lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.clear").withColor(DIM));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
    }

}
