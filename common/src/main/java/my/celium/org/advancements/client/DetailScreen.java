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
import my.celium.org.client.MycelTheme;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * One advancement up close: what it asks for, how far along you are, and every part of it, split
 * into missing and done (each biome, food, mob, ...). From here it can be tracked on the HUD.
 */
public final class DetailScreen extends PanelScreen {
    private static final int CELL_H = 18;
    private static final int CELL_MIN_W = 118;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault());

    private enum Filter { MISSING, DONE, ALL }

    private final AdvancementHolder holder;
    private Checklist list;
    private final List<Checklist.Entry> shown = new ArrayList<>();
    private Filter filter;
    private int scroll;
    private EditBox search;
    private FlatButton missingButton;
    private FlatButton doneButton;
    private FlatButton allButton;
    private FlatButton trackButton;
    private int refreshTicks;

    public DetailScreen(AdvancementHolder holder, @Nullable Screen parent) {
        super(Checklist.title(holder), parent);
        this.holder = holder;
        this.list = Tracker.checklist(holder);
        this.filter = list.complete() || list.missing().isEmpty() ? Filter.ALL : Filter.MISSING;
        AdvancementStats.count("details");
    }

    public static void open(AdvancementHolder holder, @Nullable Screen parent) {
        MycelScreens.open(new DetailScreen(holder, parent));
    }

    // ------------------------------------------------------------------ layout

    private int infoY() {
        return py() + TITLE_BAR + PAD;
    }

    private int progressY() {
        return infoY() + 28;
    }

    private int filterY() {
        return progressY() + 16;
    }

    private int gridY() {
        return filterY() + FlatButton.HEIGHT + 6;
    }

    private int gridH() {
        return bottomRowY() - 6 - gridY();
    }

    private int columns() {
        return Math.max(1, (innerW() - 4) / CELL_MIN_W);
    }

    private int cellW() {
        return (innerW() - 4) / columns();
    }

    private int visibleRows() {
        return Math.max(1, (gridH() - 4) / CELL_H);
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
        boolean listed = list.mode() != Checklist.Mode.SINGLE;
        int x = innerX();
        missingButton = new FlatButton(x, filterY(), 0, Component.empty(), false, () -> setFilter(Filter.MISSING));
        doneButton = new FlatButton(x, filterY(), 0, Component.empty(), false, () -> setFilter(Filter.DONE));
        allButton = new FlatButton(x, filterY(), 0, Component.empty(), false, () -> setFilter(Filter.ALL));
        int searchW = Math.max(70, Math.min(140, innerW() / 3));
        search = searchBox(innerX() + innerW() - searchW, filterY() + 2, searchW,
                Component.translatable("advancementsmod.search"), this::refresh);
        if (listed) {
            addRenderableWidget(missingButton);
            addRenderableWidget(doneButton);
            addRenderableWidget(allButton);
            addRenderableWidget(search);
        }

        int buttonW = (innerW() - 8) / 3;
        trackButton = new FlatButton(innerX(), bottomRowY(), buttonW, Component.empty(), true, this::toggleTrack);
        addRenderableWidget(trackButton);
        addRenderableWidget(new FlatButton(innerX() + buttonW + 4, bottomRowY(), buttonW,
                Component.translatable("advancementsmod.all"), false,
                () -> {
                    if (parent instanceof ListScreen) {
                        onClose();
                    } else {
                        MycelScreens.open(new ListScreen(this));
                    }
                }));
        addRenderableWidget(new FlatButton(innerX() + (buttonW + 4) * 2, bottomRowY(), innerW() - (buttonW + 4) * 2,
                Component.translatable(parent != null ? "gui.back" : "gui.done"), false, this::onClose));
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
        layoutFilters();
        trackButton.setMessage(Component.translatable(Tracker.isTracked(holder)
                ? "advancementsmod.untrack" : "advancementsmod.track"));
    }

    private void layoutFilters() {
        int missing = list.missing().size();
        int done = list.entries().size() - missing;
        place(missingButton, innerX(), Component.translatable("advancementsmod.filter.missing", missing), filter == Filter.MISSING);
        place(doneButton, missingButton.getX() + missingButton.getWidth() + 4,
                Component.translatable("advancementsmod.filter.done", done), filter == Filter.DONE);
        place(allButton, doneButton.getX() + doneButton.getWidth() + 4,
                Component.translatable("advancementsmod.filter.all", list.entries().size()), filter == Filter.ALL);
    }

    private void place(FlatButton button, int x, Component label, boolean selected) {
        button.setMessage(label);
        button.setX(x);
        button.setWidth(FlatButton.widthFor(font, label));
        button.setPrimary(selected);
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
        drawFrame(gfx, Component.translatable("advancementsmod.name").getString(),
                Tracker.isTracked(holder) ? Component.translatable("advancementsmod.tracking").getString() : "");
        drawInfo(gfx);
        drawProgress(gfx);
        if (list.mode() == Checklist.Mode.SINGLE) {
            drawSingle(gfx);
        } else {
            drawField(gfx, search);
            drawGrid(gfx, mx, my);
        }
        super.extractRenderState(gfx, mx, my, a);
    }

    private void drawInfo(GuiGraphicsExtractor gfx) {
        int x = innerX();
        int y = infoY();
        drawBox(gfx, x, y, 24, 24);
        gfx.item(Checklist.icon(holder), x + 4, y + 4);
        int tx = x + 30;
        int room = innerW() - 30;
        String type = Checklist.typeName(holder).getString();
        int typeW = font.width(type);
        gfx.text(font, MycelTheme.fit(font, Checklist.title(holder).getString(), room - typeW - 8), tx, y + 2,
                Checklist.typeColor(holder));
        gfx.text(font, type, x + innerW() - typeW, y + 2, MycelTheme.FAINT);
        List<FormattedCharSequence> lines = font.split(Checklist.description(holder), room);
        if (!lines.isEmpty()) {
            gfx.text(font, lines.get(0), tx, y + 14, MycelTheme.DIM);
        }
    }

    private void drawProgress(GuiGraphicsExtractor gfx) {
        int x = innerX();
        int y = progressY();
        String label;
        if (list.complete()) {
            label = Component.translatable("advancementsmod.done").getString();
        } else if (list.mode() == Checklist.Mode.ALL) {
            label = Component.translatable("advancementsmod.progress", list.doneCount(), list.total(),
                    Math.round(list.fraction() * 100)).getString();
        } else {
            label = Component.translatable("advancementsmod.not_done").getString();
        }
        int labelW = font.width(label);
        gfx.text(font, label, x + innerW() - labelW, y, list.complete() ? MycelTheme.GOOD : MycelTheme.TEXT);
        TrackerHud.bar(gfx, x, y + 3, innerW() - labelW - 8, 3, list, holder);
    }

    private void drawSingle(GuiGraphicsExtractor gfx) {
        int x = innerX();
        int y = filterY();
        drawBox(gfx, x, y, innerW(), bottomRowY() - 6 - y);
        List<FormattedCharSequence> lines = font.split(Checklist.description(holder), innerW() - 12);
        int ty = y + 6;
        gfx.text(font, Component.translatable("advancementsmod.single"), x + 6, ty, MycelTheme.FAINT);
        ty += 13;
        for (FormattedCharSequence line : lines) {
            gfx.text(font, line, x + 6, ty, MycelTheme.TEXT);
            ty += 10;
        }
    }

    private void drawGrid(GuiGraphicsExtractor gfx, int mx, int my) {
        int x0 = innerX();
        int y0 = gridY();
        int h = gridH();
        drawBox(gfx, x0, y0, innerW(), h);
        if (list.mode() == Checklist.Mode.ANY) {
            String note = Component.translatable("advancementsmod.any").getString();
            gfx.text(font, note, x0 + innerW() - font.width(note) - 4, filterY() - 11, MycelTheme.FAINT);
        }
        if (shown.isEmpty()) {
            Component empty = Component.translatable(filter == Filter.MISSING && search.getValue().isBlank()
                    ? "advancementsmod.empty.missing" : "advancementsmod.empty");
            gfx.text(font, empty, x0 + 6, y0 + 6, filter == Filter.MISSING ? MycelTheme.GOOD : MycelTheme.DIM);
            return;
        }
        int cols = columns();
        int cw = cellW();
        int first = scroll * cols;
        int last = Math.min(shown.size(), first + visibleRows() * cols);
        Checklist.Entry hovered = null;
        for (int i = first; i < last; i++) {
            Checklist.Entry e = shown.get(i);
            int col = (i - first) % cols;
            int row = (i - first) / cols;
            int cx = x0 + 2 + col * cw;
            int cy = y0 + 2 + row * CELL_H;
            boolean hover = mx >= cx && mx < cx + cw && my >= cy && my < cy + CELL_H;
            if (hover) {
                gfx.fill(cx, cy, cx + cw - 1, cy + CELL_H, MycelTheme.BORDER);
                hovered = e;
            }
            gfx.item(e.icon(), cx + 1, cy + 1);
            String name = MycelTheme.fit(font, e.name().getString(), cw - 24);
            gfx.text(font, name, cx + 20, cy + 5, e.done() ? MycelTheme.GOOD : MycelTheme.TEXT);
        }
        scrollbar(gfx, x0 + innerW(), y0, h, scroll, maxScroll(), visibleRows(), rowsTotal());
        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(hovered.name());
            if (hovered.done()) {
                tip.add(hovered.obtained() != null
                        ? MycelTheme.colored(Component.translatable("advancementsmod.done_on",
                                DATE.format(hovered.obtained())).getString(), MycelTheme.GOOD)
                        : MycelTheme.colored(Component.translatable("advancementsmod.done").getString(), MycelTheme.GOOD));
            } else {
                tip.add(MycelTheme.colored(Component.translatable("advancementsmod.missing").getString(), MycelTheme.WARN));
            }
            tip.add(MycelTheme.colored(hovered.criterion(), MycelTheme.FAINT));
            gfx.setComponentTooltipForNextFrame(font, tip, mx, my);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
