package my.celium.org.advancements.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import my.celium.org.advancements.AdvancementStats;
import my.celium.org.client.MycelScreens;
import my.celium.org.client.MycelTheme;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;

/**
 * Every advancement in one list, closest to done first: filter by in progress, not started or
 * done, search by name, click one for its details.
 */
public final class ListScreen extends PanelScreen {
    private static final int ROW_H = 24;
    private static final int BAR_W = 56;

    private enum Filter { PROGRESS, NOT_STARTED, DONE, ALL }

    private record Row(AdvancementHolder holder, Checklist list, String category, boolean started) {
    }

    private static Filter lastFilter = Filter.PROGRESS;

    private final List<Row> all = new ArrayList<>();
    private final List<Row> shown = new ArrayList<>();
    private Filter filter = lastFilter;
    private int scroll;
    private EditBox search;
    private final List<FlatButton> filterButtons = new ArrayList<>();
    private int doneCount;
    private int refreshTicks;

    public ListScreen(@Nullable Screen parent) {
        super(Component.translatable("advancementsmod.list.title"), parent);
        AdvancementStats.count("list");
    }

    public static void open(@Nullable Screen parent) {
        MycelScreens.open(new ListScreen(parent));
    }

    // ------------------------------------------------------------------ layout

    private int filterY() {
        return py() + TITLE_BAR + PAD;
    }

    private int listY() {
        return filterY() + FlatButton.HEIGHT + 6;
    }

    private int listH() {
        return bottomRowY() - 6 - listY();
    }

    private int visibleRows() {
        return Math.max(1, (listH() - 2) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, shown.size() - visibleRows());
    }

    // ------------------------------------------------------------------ data

    private void collect() {
        all.clear();
        doneCount = 0;
        ClientAdvancements adv = Tracker.advancements();
        if (adv == null) {
            return;
        }
        for (AdvancementNode node : Tracker.tree(adv).nodes()) {
            AdvancementHolder holder = node.holder();
            if (holder.value().display().isEmpty()) {
                continue;
            }
            AdvancementProgress progress = Tracker.progressMap(adv).get(holder);
            boolean done = progress != null && progress.isDone();
            if (Checklist.hidden(holder) && !done) {
                continue;
            }
            Checklist list = Checklist.of(holder, progress);
            String category = node.parent() == null ? "" : Checklist.title(node.root().holder()).getString();
            all.add(new Row(holder, list, category, progress != null && progress.hasProgress()));
            if (done) {
                doneCount++;
            }
        }
    }

    private void refresh() {
        collect();
        shown.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (Row row : all) {
            boolean done = row.list().complete();
            boolean keep = switch (filter) {
                case PROGRESS -> !done && row.started();
                case NOT_STARTED -> !done && !row.started();
                case DONE -> done;
                case ALL -> true;
            };
            if (keep && (query.isEmpty() || matches(row, query))) {
                shown.add(row);
            }
        }
        shown.sort(Comparator.<Row>comparingInt(r -> Tracker.isTracked(r.holder()) ? 0 : 1)
                .thenComparingInt(r -> r.list().complete() ? 2 : r.started() ? 0 : 1)
                .thenComparing(r -> -r.list().fraction())
                .thenComparing(r -> Checklist.title(r.holder()).getString()));
        scroll = Math.min(scroll, maxScroll());
        layoutFilters();
    }

    private static boolean matches(Row row, String query) {
        return Checklist.title(row.holder()).getString().toLowerCase(Locale.ROOT).contains(query)
                || Checklist.description(row.holder()).getString().toLowerCase(Locale.ROOT).contains(query)
                || row.category().toLowerCase(Locale.ROOT).contains(query);
    }

    private int count(Filter f) {
        int n = 0;
        for (Row row : all) {
            boolean done = row.list().complete();
            if (switch (f) {
                case PROGRESS -> !done && row.started();
                case NOT_STARTED -> !done && !row.started();
                case DONE -> done;
                case ALL -> true;
            }) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        filterButtons.clear();
        for (Filter f : Filter.values()) {
            FlatButton b = new FlatButton(innerX(), filterY(), 0, Component.empty(), false, () -> {
                filter = f;
                lastFilter = f;
                scroll = 0;
                refresh();
            });
            filterButtons.add(b);
            addRenderableWidget(b);
        }
        search = searchBox(innerX() + innerW() - 60, filterY() + 2, 60,
                Component.translatable("advancementsmod.search"), () -> {
                    scroll = 0;
                    refresh();
                });
        addRenderableWidget(search);
        addRenderableWidget(new FlatButton(innerX() + innerW() - 90, bottomRowY(), 90,
                Component.translatable(parent != null ? "gui.back" : "gui.done"), true, this::onClose));
        refresh();
    }

    private void layoutFilters() {
        String[] keys = {"advancementsmod.filter.progress", "advancementsmod.filter.not_started",
                "advancementsmod.filter.done", "advancementsmod.filter.all"};
        int x = innerX();
        for (int i = 0; i < filterButtons.size(); i++) {
            Filter f = Filter.values()[i];
            FlatButton b = filterButtons.get(i);
            Component label = Component.translatable(keys[i], count(f));
            b.setMessage(label);
            b.setX(x);
            b.setWidth(FlatButton.widthFor(font, label) - 4);
            b.setPrimary(f == filter);
            x += b.getWidth() + 3;
        }
        // The search box takes whatever room the filters leave.
        if (search != null) {
            int left = x + 5;
            int w = Math.max(50, innerX() + innerW() - left);
            search.setX(innerX() + innerW() - w + 4);
            search.setWidth(w - 8);
        }
    }

    @Override
    public void tick() {
        if (++refreshTicks % 20 == 0) {
            refresh();
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        drawFrame(gfx, Component.translatable("advancementsmod.list.title").getString(),
                Component.translatable("advancementsmod.list.done", doneCount, all.size()).getString());
        drawField(gfx, search);
        drawRows(gfx, mx, my);
        AdvancementHolder tracked = Tracker.trackedHolder();
        if (tracked != null) {
            String t = Component.translatable("advancementsmod.list.tracking", Checklist.title(tracked)).getString();
            gfx.text(font, MycelTheme.fit(font, t, innerW() - 100), innerX(), bottomRowY() + 5, MycelTheme.DIM);
        } else {
            gfx.text(font, Component.translatable("advancementsmod.list.hint"), innerX(), bottomRowY() + 5, MycelTheme.FAINT);
        }
        super.extractRenderState(gfx, mx, my, a);
    }

    private void drawRows(GuiGraphicsExtractor gfx, int mx, int my) {
        int x0 = innerX();
        int y0 = listY();
        int w = innerW();
        drawBox(gfx, x0, y0, w, listH());
        if (shown.isEmpty()) {
            gfx.text(font, Component.translatable("advancementsmod.empty"), x0 + 6, y0 + 6, MycelTheme.DIM);
            return;
        }
        int end = Math.min(shown.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            Row row = shown.get(i);
            int y = y0 + 1 + (i - scroll) * ROW_H;
            boolean hover = mx >= x0 && mx < x0 + w && my >= y && my < y + ROW_H;
            boolean tracked = Tracker.isTracked(row.holder());
            if (hover) {
                gfx.fill(x0 + 1, y, x0 + w - 1, y + ROW_H, MycelTheme.BORDER);
            } else if ((i & 1) == 1) {
                gfx.fill(x0 + 1, y, x0 + w - 1, y + ROW_H, MycelTheme.ZEBRA);
            }
            if (tracked) {
                gfx.fill(x0 + 1, y, x0 + 3, y + ROW_H, MycelTheme.ACCENT);
            }
            gfx.item(Checklist.icon(row.holder()), x0 + 5, y + 4);

            int right = x0 + w - 6;
            String count = TrackerHud.countText(row.list());
            int countW = font.width(count);
            boolean done = row.list().complete();
            gfx.text(font, count, right - countW, y + 4, done ? MycelTheme.GOOD : MycelTheme.TEXT);
            if (!done && row.list().mode() == Checklist.Mode.ALL) {
                TrackerHud.bar(gfx, right - BAR_W, y + 15, BAR_W, 2, row.list(), row.holder());
            }

            int tx = x0 + 26;
            int room = right - Math.max(countW, BAR_W) - 8 - tx;
            String title = Checklist.title(row.holder()).getString();
            gfx.text(font, MycelTheme.fit(font, title, room), tx, y + 3, Checklist.typeColor(row.holder()));
            String sub = row.category().isEmpty() ? Checklist.description(row.holder()).getString()
                    : row.category() + "  " + Checklist.description(row.holder()).getString();
            gfx.text(font, MycelTheme.fit(font, sub, room), tx, y + 13, MycelTheme.FAINT);
        }
        scrollbar(gfx, x0 + w, y0, listH(), scroll, maxScroll(), visibleRows(), shown.size());
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int x0 = innerX();
        int y0 = listY() + 1;
        if (event.button() == 0 && event.x() >= x0 && event.x() < x0 + innerW() && event.y() >= y0) {
            int index = scroll + (int) ((event.y() - y0) / ROW_H);
            if (index < Math.min(shown.size(), scroll + visibleRows())) {
                DetailScreen.open(shown.get(index).holder(), this);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY) * 2));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
