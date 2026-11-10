package org.example.pz.quarryindustrial.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.example.pz.quarryindustrial.filter.PipeFilterData;
import org.example.pz.quarryindustrial.filter.PipeFilterMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del filtro de caño, dibujada sin textura con el mismo estilo que las demás del mod: los casilleros
 * (los que la mejora no activó se ven apagados), el slot de la mejora y dos botones (lista blanca/negra, ítem/tag).
 */
public class PipeFilterScreen extends AbstractContainerScreen<PipeFilterMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int DIM = 0xFF707070;
    private static final int TEXT = 0xFF404040;

    private Button mode;
    private Button match;

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

    private Component modeLabel() {
        return Component.translatable(menu.allowList() ? "gui.quarryindustrial.pipe_filter.allow"
                : "gui.quarryindustrial.pipe_filter.deny");
    }

    private Component matchLabel() {
        return Component.translatable(menu.tagMatch() ? "gui.quarryindustrial.pipe_filter.match_tag"
                : "gui.quarryindustrial.pipe_filter.match_item");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        bevel(graphics, leftPos, topPos, imageWidth, imageHeight, PANEL, LIGHT, SHADOW);
        for (Slot slot : menu.slots) {
            bevel(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0xFF8B8B8B, SHADOW, LIGHT);
        }
        graphics.text(font, Component.translatable("gui.quarryindustrial.pipe_filter.upgrade"),
                leftPos + PipeFilterMenu.UPGRADE_X - 10, topPos + PipeFilterMenu.UPGRADE_Y - 11, DIM, false);
        graphics.text(font, Component.translatable("gui.quarryindustrial.pipe_filter.cells"),
                leftPos + PipeFilterMenu.CELL_X, topPos + PipeFilterMenu.CELL_Y - 11, DIM, false);
        // Los casilleros sin activar se ven apagados hasta instalar la mejora
        for (int i = menu.activeCells(); i < PipeFilterData.CELLS; i++) {
            int x = leftPos + PipeFilterMenu.CELL_X + i * 18;
            int y = topPos + PipeFilterMenu.CELL_Y;
            graphics.fill(x, y, x + 16, y + 16, 0xD08B8B8B);
        }
        // Marca de "tag" en los casilleros que comparan por categoría
        if (menu.tagMatch()) {
            for (int i = 0; i < menu.activeCells(); i++) {
                if (!menu.filter().getItem(i).isEmpty()) {
                    graphics.text(font, "#", leftPos + PipeFilterMenu.CELL_X + i * 18 + 1,
                            topPos + PipeFilterMenu.CELL_Y + 1, 0xFF55FFFF, true);
                }
            }
        }
        Component state = status();
        graphics.text(font, state, leftPos + PipeFilterMenu.WIDTH / 2 - font.width(state) / 2,
                topPos + 78, TEXT, false);
    }

    private Component status() {
        PipeFilterData filter = menu.filter();
        if (!filter.isConfigured()) {
            return Component.translatable(menu.allowList() ? "gui.quarryindustrial.pipe_filter.empty_allow"
                    : "gui.quarryindustrial.pipe_filter.empty_deny");
        }
        return Component.empty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        mode.setMessage(modeLabel());
        match.setMessage(matchLabel());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractCellTooltip(graphics, mouseX, mouseY);
    }

    /** Al pasar por un casillero: qué se compara (el ítem o el tag elegido) y cómo cambiarlo. */
    private void extractCellTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = 0; i < PipeFilterData.CELLS; i++) {
            int x = leftPos + PipeFilterMenu.CELL_X + i * 18;
            int y = topPos + PipeFilterMenu.CELL_Y;
            if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            ItemStack sample = menu.filter().getItem(i);
            if (i >= menu.activeCells()) {
                lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.locked").withColor(DIM));
            } else if (sample.isEmpty()) {
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
                }
            } else {
                lines.add(sample.getHoverName());
                lines.add(Component.translatable("gui.quarryindustrial.pipe_filter.clear").withColor(DIM));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
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
