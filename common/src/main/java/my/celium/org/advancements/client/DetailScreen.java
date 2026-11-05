package my.celium.org.advancements.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import my.celium.org.advancements.AdvancementStats;
import my.celium.org.client.MycelScreens;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * One advancement up close, in the look of vanilla's advancements window: the advancement's frame
 * big at the top, its progress as an experience bar, and every part of it as a small advancement of
 * its own, gold when done and blue when missing.
 */
public final class DetailScreen extends Screen {
    private static final int CELL_H = 28;
    private static final int CELL_MIN_W = 148;
    private static final int BUTTON_H = 20;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault());

    private enum Filter { MISSING, DONE, ALL }

    private final AdvancementHolder holder;
    private final @Nullable Screen parent;
    private Checklist list;
    private final List<Checklist.Entry> shown = new ArrayList<>();
    private Filter filter;
    private int scroll;
    private @Nullable EditBox search;
    private Button missingButton;
    private Button doneButton;
    private Button allButton;
    private Button trackButton;
    private int refreshTicks;

    public DetailScreen(AdvancementHolder holder, @Nullable Screen parent) {
        super(Checklist.title(holder));
        this.holder = holder;
        this.parent = parent;
        this.list = Tracker.checklist(holder);
        this.filter = list.complete() || list.missing().isEmpty() ? Filter.ALL : Filter.MISSING;
        AdvancementStats.count("details");
    }

    public static void open(AdvancementHolder holder, @Nullable Screen parent) {
        MycelScreens.open(new DetailScreen(holder, parent));
    }

    // ------------------------------------------------------------------ layout

    private int winW() {
        return Math.min(width - 16, 420);
    }

    private int winH() {
        int full = Math.min(height - 16 - BUTTON_H - 6, 300);
        if (list.mode() == Checklist.Mode.SINGLE) {
            // Frame, description, bar and one line of text: no list to make room for.
            int lines = Math.min(2, font.split(Checklist.description(holder), winW() - Theme.BORDER * 2 - 40).size());
            return Math.min(full, Theme.TITLE + 6 + 56 + lines * 10 + 14 + 5 + 6 + 18 + Theme.BORDER);
        }
        return full;
    }

    private int winX() {
        return (width - winW()) / 2;
    }

    private int winY() {
        return (height - winH() - BUTTON_H - 6) / 2;
    }

    private int inX() {
        return winX() + Theme.BORDER;
    }

    private int inY() {
        return winY() + Theme.TITLE;
    }

    private int inW() {
        return winW() - Theme.BORDER * 2;
    }

    private int inBottom() {
        return winY() + winH() - Theme.BORDER;
    }

    private int heroY() {
        return inY() + 6;
    }

    private List<FormattedCharSequence> descriptionLines() {
        List<FormattedCharSequence> lines = font.split(Checklist.description(holder), inW() - 40);
        return lines.size() > 2 ? lines.subList(0, 2) : lines;
    }

    private int descriptionY() {
        return heroY() + 52 + 4;
    }

    private int barY() {
        return descriptionY() + descriptionLines().size() * 10 + 14;
    }

    private int filterY() {
        return barY() + 5 + 6;
    }

    private int gridY() {
        return filterY() + BUTTON_H + 4;
    }

    private int gridH() {
        return inBottom() - 2 - gridY();
    }

    private int columns() {
        return Math.max(1, (inW() - 8) / CELL_MIN_W);
    }

    private int cellW() {
        return (inW() - 8) / columns();
    }

    private int visibleRows() {
        return Math.max(1, gridH() / CELL_H);
    }

    private int rowsTotal() {
        return (shown.size() + columns() - 1) / columns();
    }

    private int maxScroll() {
        return Math.max(0, rowsTotal() - visibleRows());
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        if (list.mode() != Checklist.Mode.SINGLE) {
            missingButton = addRenderableWidget(Button.builder(Component.empty(), b -> setFilter(Filter.MISSING))
                    .bounds(inX() + 4, filterY(), 60, BUTTON_H).build());
            doneButton = addRenderableWidget(Button.builder(Component.empty(), b -> setFilter(Filter.DONE))
                    .bounds(inX() + 4, filterY(), 60, BUTTON_H).build());
            allButton = addRenderableWidget(Button.builder(Component.empty(), b -> setFilter(Filter.ALL))
                    .bounds(inX() + 4, filterY(), 60, BUTTON_H).build());
            int searchW = Math.max(70, Math.min(130, inW() / 3));
            search = new EditBox(font, inX() + inW() - 4 - searchW, filterY(), searchW, BUTTON_H,
                    Component.translatable("advancementsmod.search"));
            search.setHint(Component.translatable("advancementsmod.search"));
            search.setResponder(text -> {
                scroll = 0;
                refresh();
            });
            addRenderableWidget(search);
        }

        int y = winY() + winH() + 6;
        int buttonW = Math.min(120, (winW() - 8) / 3);
        int x = (width - buttonW * 3 - 8) / 2;
        trackButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggleTrack())
                .bounds(x, y, buttonW, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("advancementsmod.all"), b -> {
            if (parent instanceof ListScreen) {
                onClose();
            } else {
                MycelScreens.open(new ListScreen(this));
            }
        }).bounds(x + buttonW + 4, y, buttonW, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable(parent != null ? "gui.back" : "gui.done"),
                b -> onClose()).bounds(x + (buttonW + 4) * 2, y, buttonW, BUTTON_H).build());
        refresh();
    }

    private void setFilter(Filter f) {
        filter = f;
        scroll = 0;
        refresh();
    }

    private void toggleTrack() {
        Tracker.toggle(holder);
        refresh();
    }

    private void refresh() {
        list = Tracker.checklist(holder);
        shown.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (Checklist.Entry e : list.entries()) {
            boolean keep = switch (filter) {
                case MISSING -> !e.done();
                case DONE -> e.done();
                case ALL -> true;
            };
            if (keep && (query.isEmpty() || e.name().getString().toLowerCase(Locale.ROOT).contains(query)
                    || e.criterion().toLowerCase(Locale.ROOT).contains(query))) {
                shown.add(e);
            }
        }
        if (filter == Filter.ALL) {
            shown.sort((a, b) -> Boolean.compare(a.done(), b.done()));
        }
        scroll = Math.min(scroll, maxScroll());
        if (missingButton != null) {
            int missing = list.missing().size();
            int x = inX() + 4;
            x = place(missingButton, x, Component.translatable("advancementsmod.filter.missing", missing), Filter.MISSING);
            x = place(doneButton, x, Component.translatable("advancementsmod.filter.done", list.entries().size() - missing), Filter.DONE);
            place(allButton, x, Component.translatable("advancementsmod.filter.all", list.entries().size()), Filter.ALL);
        }
        trackButton.setMessage(Component.translatable(Tracker.isTracked(holder)
                ? "advancementsmod.untrack" : "advancementsmod.track"));
    }

    private int place(Button button, int x, Component label, Filter f) {
        button.setMessage(label);
        button.setX(x);
        button.setWidth(font.width(label) + 14);
        // The selected filter looks pressed, like a selected tab.
        button.active = filter != f;
        return x + button.getWidth() + 2;
    }

    @Override
    public void tick() {
        // Progress can arrive while the screen is open.
        if (++refreshTicks % 20 == 0) {
            refresh();
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        drawWindow(gfx);
        drawHero(gfx);
        if (list.mode() == Checklist.Mode.SINGLE) {
            drawSingle(gfx);
        } else {
            drawGrid(gfx, mx, my);
        }
        super.extractRenderState(gfx, mx, my, a);
        drawTooltip(gfx, mx, my);
    }

    private @Nullable AdvancementHolder root() {
        AdvancementNode node = Tracker.node(holder);
        return node == null ? null : node.root().holder();
    }

    private void drawWindow(GuiGraphicsExtractor gfx) {
        Theme.tiles(gfx, Theme.background(root()), inX(), inY(), inX() + inW(), inBottom());
        gfx.fill(inX(), inY(), inX() + inW(), inBottom(), 0x55000000);
        Theme.window(gfx, winX(), winY(), winW(), winH());
        String type = Checklist.typeName(holder).getString();
        int typeW = font.width(type);
        String title = Theme.fit(font, Checklist.title(holder).getString(), winW() - 24 - typeW);
        gfx.text(font, title, winX() + 8, winY() + 6, Theme.TITLE_TEXT, false);
        gfx.text(font, type, winX() + winW() - 8 - typeW, winY() + 6, Theme.TITLE_TEXT, false);
    }

    private void drawHero(GuiGraphicsExtractor gfx) {
        int cx = inX() + inW() / 2;
        Theme.bigFrame(gfx, Theme.frameType(holder), list.complete(), Checklist.icon(holder), cx - 26, heroY());
        int y = descriptionY();
        for (FormattedCharSequence line : descriptionLines()) {
            gfx.centeredText(font, line, cx, y, Theme.WHITE);
            y += 10;
        }
        String count;
        if (list.complete()) {
            count = Component.translatable("advancementsmod.done").getString();
        } else if (list.mode() == Checklist.Mode.ALL) {
            count = list.doneCount() + " / " + list.total();
        } else {
            count = Component.translatable("advancementsmod.not_done").getString();
        }
        Theme.outlined(gfx, font, count, cx - font.width(count) / 2, barY() - 10,
                list.complete() ? Theme.GOLD : Theme.XP_GREEN);
        Theme.xpBar(gfx, cx - 91, barY(), list.fraction());
    }

    private void drawSingle(GuiGraphicsExtractor gfx) {
        int cx = inX() + inW() / 2;
        gfx.centeredText(font, Component.translatable("advancementsmod.single.hint"), cx, filterY() + 6, Theme.GRAY);
    }

    private void drawGrid(GuiGraphicsExtractor gfx, int mx, int my) {
        int x0 = inX() + 4;
        int y0 = gridY();
        if (list.mode() == Checklist.Mode.ANY) {
            String note = Component.translatable("advancementsmod.any").getString();
            gfx.text(font, note, inX() + inW() - 6 - font.width(note), barY() - 10, Theme.GRAY);
        }
        if (shown.isEmpty()) {
            Component empty = Component.translatable(filter == Filter.MISSING && (search == null || search.getValue().isBlank())
                    ? "advancementsmod.empty.missing" : "advancementsmod.empty");
            gfx.centeredText(font, empty, inX() + inW() / 2, y0 + 10, filter == Filter.MISSING ? Theme.GOLD : Theme.GRAY);
            return;
        }
        int cols = columns();
        int cw = cellW();
        int first = scroll * cols;
        int last = Math.min(shown.size(), first + visibleRows() * cols);
        for (int i = first; i < last; i++) {
            Checklist.Entry e = shown.get(i);
            int cx = x0 + ((i - first) % cols) * cw;
            int cy = y0 + ((i - first) / cols) * CELL_H;
            boolean hover = mx >= cx && mx < cx + cw - 4 && my >= cy && my < cy + 26;
            Theme.ribbon(gfx, e.done(), cx + 13, cy + 3, cw - 17, 20);
            Theme.frame(gfx, "task", e.done(), e.icon(), cx, cy);
            String name = Theme.fit(font, e.name().getString(), cw - 40);
            gfx.text(font, name, cx + 31, cy + 9, hover ? 0xFFFFFFA0 : Theme.WHITE);
        }
        drawScrollbar(gfx, inX() + inW() - 4, y0, visibleRows() * CELL_H);
    }

    private void drawScrollbar(GuiGraphicsExtractor gfx, int right, int top, int h) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int thumb = Math.max(12, h * visibleRows() / Math.max(1, rowsTotal()));
        int y = top + (h - thumb) * scroll / max;
        gfx.fill(right - 3, top, right, top + h, 0x66000000);
        gfx.fill(right - 3, y, right, y + thumb, 0xFFC6C6C6);
    }

    private void drawTooltip(GuiGraphicsExtractor gfx, int mx, int my) {
        if (list.mode() == Checklist.Mode.SINGLE) {
            return;
        }
        int cols = columns();
        int cw = cellW();
        int x0 = inX() + 4;
        int y0 = gridY();
        if (mx < x0 || my < y0 || mx >= x0 + cols * cw || my >= y0 + visibleRows() * CELL_H) {
            return;
        }
        int index = (scroll + (my - y0) / CELL_H) * cols + (mx - x0) / cw;
        if (index >= shown.size() || (my - y0) % CELL_H >= 26) {
            return;
        }
        Checklist.Entry e = shown.get(index);
        List<Component> tip = new ArrayList<>();
        tip.add(e.name());
        if (e.done()) {
            tip.add(e.obtained() != null
                    ? Component.translatable("advancementsmod.done_on", DATE.format(e.obtained())).withColor(Theme.GOLD)
                    : Component.translatable("advancementsmod.done").withColor(Theme.GOLD));
        } else {
            tip.add(Component.translatable("advancementsmod.missing").withColor(0xFF55C8FF));
        }
        tip.add(Component.literal(e.criterion()).withColor(0xFF808080));
        gfx.setComponentTooltipForNextFrame(font, tip, mx, my);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
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
