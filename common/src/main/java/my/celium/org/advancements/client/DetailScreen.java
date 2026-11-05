package my.celium.org.advancements.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import my.celium.org.advancements.AdvancementStats;
import my.celium.org.client.MycelScreens;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A small window over vanilla's advancements screen: what the advancement is, what is still
 * needed (each biome, food, mob, ...), and a switch for showing it on screen while you play.
 */
public final class DetailScreen extends Screen {
    private static final int ROW_H = 18;
    private static final int MAX_ROWS = 7;
    private static final int PAD = 6;
    private static final int BUTTON_H = 20;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault());

    private final AdvancementHolder holder;
    private final @Nullable Screen parent;
    private Checklist list;
    /** Missing first, then done. */
    private final List<Checklist.Entry> rows = new ArrayList<>();
    private int scroll;
    private Button overlayButton;
    private int refreshTicks;

    public DetailScreen(AdvancementHolder holder, @Nullable Screen parent) {
        super(Checklist.title(holder));
        this.holder = holder;
        this.parent = parent;
        this.list = Tracker.checklist(holder);
        AdvancementStats.count("details");
    }

    public static void open(AdvancementHolder holder, @Nullable Screen parent) {
        MycelScreens.open(new DetailScreen(holder, parent));
    }

    // ------------------------------------------------------------------ layout

    private boolean hasList() {
        return list.mode() != Checklist.Mode.SINGLE;
    }

    private int winW() {
        return Math.min(width - 20, 250);
    }

    private int inX() {
        return winX() + Theme.BORDER;
    }

    private int inW() {
        return winW() - Theme.BORDER * 2;
    }

    private List<FormattedCharSequence> descriptionLines() {
        List<FormattedCharSequence> lines = font.split(Checklist.description(holder), inW() - PAD * 2 - 32);
        return lines.size() > 4 ? lines.subList(0, 4) : lines;
    }

    /** Height of the frame and description block. */
    private int headH() {
        return Math.max(26, descriptionLines().size() * 10 + 2);
    }

    private int visibleRows() {
        return hasList() ? Math.min(MAX_ROWS, Math.max(1, rows.size())) : 0;
    }

    private int winH() {
        int h = Theme.TITLE + PAD + headH() + PAD + 12;
        if (hasList()) {
            h += 12 + visibleRows() * ROW_H;
        }
        h += PAD + BUTTON_H + PAD + Theme.BORDER - 4;
        return Math.min(height - 20, h);
    }

    private int winX() {
        return (width - winW()) / 2;
    }

    private int winY() {
        return (height - winH()) / 2;
    }

    private int headY() {
        return winY() + Theme.TITLE + PAD;
    }

    private int progressY() {
        return headY() + headH() + PAD;
    }

    private int listY() {
        return progressY() + 12 + 12;
    }

    private int buttonY() {
        return winY() + winH() - Theme.BORDER - BUTTON_H - 2;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        if (parent != null && (parent.width != width || parent.height != height)) {
            parent.init(width, height);
        }
        refresh();
        int w = (inW() - PAD * 2 - 4) / 2;
        overlayButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            Tracker.toggle(holder);
            refresh();
        }).bounds(inX() + PAD, buttonY(), w, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(inX() + PAD + w + 4, buttonY(), w, BUTTON_H).build());
        refresh();
    }

    private void refresh() {
        list = Tracker.checklist(holder);
        rows.clear();
        rows.addAll(list.missing());
        for (Checklist.Entry e : list.entries()) {
            if (e.done()) {
                rows.add(e);
            }
        }
        scroll = Math.min(scroll, maxScroll());
        if (overlayButton != null) {
            overlayButton.setMessage(Component.translatable(Tracker.isTracked(holder)
                    ? "advancementsmod.overlay.on" : "advancementsmod.overlay.off"));
        }
    }

    @Override
    public void tick() {
        // Progress can arrive while the window is open.
        if (++refreshTicks % 20 == 0) {
            refresh();
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float a) {
        if (parent != null) {
            // Vanilla's advancements screen stays visible behind the window, dimmed.
            parent.extractBackground(gfx, -1, -1, a);
            parent.extractRenderState(gfx, -1, -1, a);
            gfx.nextStratum();
            gfx.fill(0, 0, width, height, 0x99000000);
        } else {
            super.extractBackground(gfx, mouseX, mouseY, a);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        int x0 = winX();
        int y0 = winY();
        Theme.tiles(gfx, Theme.background(root()), inX(), y0 + Theme.TITLE, inX() + inW(), y0 + winH() - Theme.BORDER);
        gfx.fill(inX(), y0 + Theme.TITLE, inX() + inW(), y0 + winH() - Theme.BORDER, 0x99000000);
        Theme.window(gfx, x0, y0, winW(), winH());
        gfx.text(font, Theme.fit(font, Checklist.title(holder).getString(), winW() - 16), x0 + 8, y0 + 6, Theme.TITLE_TEXT, false);

        // What it is.
        int x = inX() + PAD;
        Theme.frame(gfx, Theme.frameType(holder), list.complete(), Checklist.icon(holder), x, headY());
        int ty = headY() + Math.max(0, (headH() - descriptionLines().size() * 10) / 2) + 1;
        for (FormattedCharSequence line : descriptionLines()) {
            gfx.text(font, line, x + 32, ty, Theme.WHITE);
            ty += 10;
        }

        // How far along.
        int py = progressY();
        String status;
        int color;
        if (list.complete()) {
            status = Component.translatable("advancementsmod.done").getString();
            color = Theme.GOLD;
        } else if (list.mode() == Checklist.Mode.ALL) {
            status = list.doneCount() + " / " + list.total();
            color = Theme.XP_GREEN;
        } else {
            status = Component.translatable("advancementsmod.not_done").getString();
            color = Theme.GRAY;
        }
        Theme.outlined(gfx, font, status, x, py, color);
        int barX = x + font.width(status) + 6;
        Theme.slimBar(gfx, barX, py + 3, inX() + inW() - PAD - barX, 2, list.fraction(), list.complete());

        if (hasList()) {
            drawList(gfx, mx, my);
        }
        super.extractRenderState(gfx, mx, my, a);
        drawTooltip(gfx, mx, my);
    }

    private @Nullable AdvancementHolder root() {
        var node = Tracker.node(holder);
        return node == null ? null : node.root().holder();
    }

    private void drawList(GuiGraphicsExtractor gfx, int mx, int my) {
        int x = inX() + PAD;
        int w = inW() - PAD * 2;
        String heading = Component.translatable(list.mode() == Checklist.Mode.ANY ? "advancementsmod.any"
                : list.complete() ? "advancementsmod.got" : "advancementsmod.needed").getString();
        gfx.text(font, heading, x, listY() - 11, Theme.GRAY);
        int end = Math.min(rows.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            Checklist.Entry e = rows.get(i);
            int y = listY() + (i - scroll) * ROW_H;
            if (mx >= x && mx < x + w && my >= y && my < y + ROW_H) {
                gfx.fill(x - 2, y, x + w, y + ROW_H, 0x40FFFFFF);
            }
            gfx.item(e.icon(), x, y + 1);
            gfx.text(font, Theme.fit(font, e.name().getString(), w - 26), x + 20, y + 5, e.done() ? Theme.GOLD : Theme.WHITE);
        }
        int max = maxScroll();
        if (max > 0) {
            int h = visibleRows() * ROW_H;
            int thumb = Math.max(10, h * visibleRows() / rows.size());
            int top = listY() + (h - thumb) * scroll / max;
            int right = x + w + 2;
            gfx.fill(right - 2, listY(), right, listY() + h, 0x66000000);
            gfx.fill(right - 2, top, right, top + thumb, 0xFFC6C6C6);
        }
    }

    private void drawTooltip(GuiGraphicsExtractor gfx, int mx, int my) {
        if (!hasList()) {
            return;
        }
        int x = inX() + PAD;
        int w = inW() - PAD * 2;
        if (mx < x || mx >= x + w || my < listY() || my >= listY() + visibleRows() * ROW_H) {
            return;
        }
        int index = scroll + (my - listY()) / ROW_H;
        if (index >= rows.size()) {
            return;
        }
        Checklist.Entry e = rows.get(index);
        List<Component> tip = new ArrayList<>();
        tip.add(e.name());
        if (e.done()) {
            tip.add(e.obtained() != null
                    ? Component.translatable("advancementsmod.done_on", DATE.format(e.obtained())).withColor(Theme.GOLD)
                    : Component.translatable("advancementsmod.done").withColor(Theme.GOLD));
        } else {
            tip.add(Component.translatable("advancementsmod.missing").withColor(0xFF55C8FF));
        }
        gfx.setComponentTooltipForNextFrame(font, tip, mx, my);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0 && maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        MycelScreens.open(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
