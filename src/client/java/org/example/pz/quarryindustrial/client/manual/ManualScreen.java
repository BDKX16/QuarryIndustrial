package org.example.pz.quarryindustrial.client.manual;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del manual, con estilo de plano técnico: capítulos a la izquierda (con su ícono) y la página a
 * la derecha. Las páginas largas se parten solas en varias hojas. Flechas o rueda del mouse para pasar hoja.
 */
public class ManualScreen extends Screen {
    private static final int WIDTH = 340;
    private static final int HEIGHT = 210;
    private static final int SIDEBAR = 112;
    private static final int CHAPTER_H = 18;
    private static final int PAD = 10;
    private static final int LINE = 10;

    private static final int PAPER = 0xFF0F2A47;
    private static final int GRID = 0xFF173A5E;
    private static final int GRID_MAJOR = 0xFF1F4A75;
    private static final int INK = 0xFFE6F0FF;
    private static final int DIM = 0xFF8FB4DA;
    private static final int ACCENT = 0xFFFFD24A;
    private static final int SELECTED = 0xFF24527F;

    private final ManualContent.Book book;
    private int chapter;
    private int sheet;
    /** Hojas del capítulo actual: cada una es una lista de (título de página, líneas, items). */
    private List<Sheet> sheets = List.of();

    private record Sheet(String title, List<FormattedCharSequence> lines, List<ItemStack> items) {
    }

    public ManualScreen() {
        super(Component.translatable("item.quarryindustrial.manual"));
        this.book = ManualContent.load();
    }

    @Override
    protected void init() {
        layoutChapter();
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    private int textWidth() {
        return WIDTH - SIDEBAR - PAD * 2;
    }

    /** Arma las hojas del capítulo: cada página se parte en tantas hojas como haga falta. */
    private void layoutChapter() {
        List<Sheet> result = new ArrayList<>();
        if (!book.chapters().isEmpty()) {
            ManualContent.Chapter current = book.chapters().get(chapter);
            int linesPerSheet = (HEIGHT - PAD * 2 - 40) / LINE;
            for (ManualContent.Page page : current.pages()) {
                List<ItemStack> items = new ArrayList<>();
                if (page.items() != null) {
                    for (String id : page.items()) {
                        items.add(stack(id));
                    }
                }
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (String paragraph : page.text().split("\n", -1)) {
                    if (paragraph.isEmpty()) {
                        lines.add(FormattedCharSequence.EMPTY);
                    } else {
                        lines.addAll(font.split(FormattedText.of(paragraph), textWidth()));
                    }
                }
                for (int from = 0; from < Math.max(1, lines.size()); from += linesPerSheet) {
                    List<FormattedCharSequence> chunk = lines.subList(from, Math.min(lines.size(), from + linesPerSheet));
                    result.add(new Sheet(page.title(), List.copyOf(chunk), from == 0 ? items : List.of()));
                }
            }
        }
        sheets = result;
        sheet = Math.clamp(sheet, 0, Math.max(0, sheets.size() - 1));
    }

    private static ItemStack stack(String id) {
        Identifier identifier = Identifier.tryParse(id);
        Item item = identifier == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(identifier);
        return item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        drawPaper(graphics, x, y);
        drawChapters(graphics, x, y, mouseX, mouseY);
        drawSheet(graphics, x + SIDEBAR + PAD, y + PAD);
    }

    /** Papel azul de plano con cuadrícula fina y gruesa, y un marco doble. */
    private static void drawPaper(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 3, y - 3, x + WIDTH + 3, y + HEIGHT + 3, 0xFF06111F);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, PAPER);
        for (int gx = 8; gx < WIDTH; gx += 8) {
            graphics.fill(x + gx, y, x + gx + 1, y + HEIGHT, gx % 40 == 0 ? GRID_MAJOR : GRID);
        }
        for (int gy = 8; gy < HEIGHT; gy += 8) {
            graphics.fill(x, y + gy, x + WIDTH, y + gy + 1, gy % 40 == 0 ? GRID_MAJOR : GRID);
        }
        graphics.outline(x + 2, y + 2, WIDTH - 4, HEIGHT - 4, DIM);
        graphics.fill(x + SIDEBAR, y + 6, x + SIDEBAR + 1, y + HEIGHT - 6, DIM);
    }

    private void drawChapters(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        graphics.text(font, title, x + 8, y + 8, ACCENT, false);
        List<ManualContent.Chapter> chapters = book.chapters();
        for (int i = 0; i < chapters.size(); i++) {
            int cy = y + 22 + i * CHAPTER_H;
            boolean hovered = mouseX >= x + 4 && mouseX < x + SIDEBAR - 4 && mouseY >= cy && mouseY < cy + CHAPTER_H;
            if (i == chapter || hovered) {
                graphics.fill(x + 4, cy, x + SIDEBAR - 4, cy + CHAPTER_H - 1, i == chapter ? SELECTED : GRID_MAJOR);
            }
            graphics.item(stack(chapters.get(i).icon()), x + 6, cy + 1);
            String name = font.plainSubstrByWidth(chapters.get(i).title(), SIDEBAR - 32);
            graphics.text(font, name, x + 25, cy + 5, i == chapter ? ACCENT : INK, false);
        }
    }

    private void drawSheet(GuiGraphicsExtractor graphics, int x, int y) {
        if (sheets.isEmpty()) {
            return;
        }
        Sheet current = sheets.get(sheet);
        int itemsWidth = current.items().size() * 18;
        graphics.text(font, font.plainSubstrByWidth(current.title(), textWidth() - itemsWidth), x, y + 2, ACCENT, false);
        for (int i = 0; i < current.items().size(); i++) {
            graphics.item(current.items().get(i), x + textWidth() - itemsWidth + i * 18, y - 2);
        }
        graphics.fill(x, y + 16, x + textWidth(), y + 17, DIM);
        int ly = y + 24;
        for (FormattedCharSequence line : current.lines()) {
            graphics.text(font, line, x, ly, INK, false);
            ly += LINE;
        }
        // Pie: hoja actual y flechas
        int footer = top() + HEIGHT - PAD - 8;
        String counter = (sheet + 1) + " / " + sheets.size();
        graphics.text(font, counter, x + (textWidth() - font.width(counter)) / 2, footer, DIM, false);
        if (sheet > 0) {
            graphics.text(font, "<", x, footer, ACCENT, false);
        }
        if (sheet < sheets.size() - 1) {
            graphics.text(font, ">", x + textWidth() - 6, footer, ACCENT, false);
        }
    }

    private void turn(int delta) {
        int next = sheet + delta;
        if (next >= 0 && next < sheets.size()) {
            sheet = next;
        } else if (next < 0 && chapter > 0) {
            // Al pasar del principio, la última hoja del capítulo anterior
            chapter--;
            sheet = Integer.MAX_VALUE;
            layoutChapter();
        } else if (next >= sheets.size() && chapter < book.chapters().size() - 1) {
            chapter++;
            sheet = 0;
            layoutChapter();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = left();
        int y = top();
        double mx = event.x();
        double my = event.y();
        for (int i = 0; i < book.chapters().size(); i++) {
            int cy = y + 22 + i * CHAPTER_H;
            if (mx >= x + 4 && mx < x + SIDEBAR - 4 && my >= cy && my < cy + CHAPTER_H) {
                chapter = i;
                sheet = 0;
                layoutChapter();
                return true;
            }
        }
        int footer = y + HEIGHT - PAD - 8;
        if (my >= footer - 4 && my < footer + 12) {
            int textX = x + SIDEBAR + PAD;
            if (mx >= textX - 4 && mx < textX + 20) {
                turn(-1);
                return true;
            }
            if (mx >= textX + textWidth() - 20 && mx < textX + textWidth() + 4) {
                turn(1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        turn(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_A) {
            turn(-1);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_D) {
            turn(1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
