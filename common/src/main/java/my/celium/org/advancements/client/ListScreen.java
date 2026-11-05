package my.celium.org.advancements.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import my.celium.org.advancements.AdvancementStats;
import my.celium.org.client.MycelScreens;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Every advancement in one list, in the look of vanilla's advancements window: a tab per category
 * on top, closest to done first, filters below. Click one for its details.
 */
public final class ListScreen extends Screen {
    private static final int ROW_H = 30;
    private static final int TAB_W = 28;
    private static final int TAB_H = 32;
    private static final int BUTTON_H = 20;

    private enum Filter { PROGRESS, NOT_STARTED, DONE, ALL }

    private record Row(AdvancementHolder holder, Checklist list, @Nullable AdvancementHolder root, boolean started) {
    }

    private static Filter lastFilter = Filter.PROGRESS;
    private static @Nullable Identifier lastTab;

    private final @Nullable Screen parent;
    private final List<Row> all = new ArrayList<>();
    private final List<Row> shown = new ArrayList<>();
    private final List<AdvancementHolder> tabs = new ArrayList<>();
    /** Selected category root, or null for every category. */
    private @Nullable AdvancementHolder tab;
    private Filter filter = lastFilter;
    private int scroll;
    private EditBox search;
    private final List<Button> filterButtons = new ArrayList<>();
    private int refreshTicks;

    public ListScreen(@Nullable Screen parent) {
        super(Component.translatable("advancementsmod.list.title"));
        this.parent = parent;
        AdvancementStats.count("list");
    }

    public static void open(@Nullable Screen parent) {
        MycelScreens.open(new ListScreen(parent));
    }

    // ------------------------------------------------------------------ layout

    private int winW() {
        return Math.min(width - 16, 420);
    }

    private int winH() {
        return Math.max(80, Math.min(height - TAB_H - BUTTON_H * 2 - 24, 300));
    }

    private int winX() {
        return (width - winW()) / 2;
    }

    private int winY() {
        return (height - winH() - BUTTON_H * 2 - 12 + TAB_H - 4) / 2;
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

    private int visibleRows() {
        return Math.max(1, (inBottom() - inY() - 4) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, shown.size() - visibleRows());
    }

    private int tabX(int index) {
        return winX() + (TAB_W + 4) * index;
    }

    private int tabY() {
        return winY() - TAB_H + 4;
    }

    private int maxTabs() {
        return Math.max(1, (winW() + 4) / (TAB_W + 4));
    }

    // ------------------------------------------------------------------ data

    private void collect() {
        all.clear();
        tabs.clear();
        ClientAdvancements adv = Tracker.advancements();
        if (adv == null) {
            return;
        }
        for (AdvancementNode root : Tracker.tree(adv).roots()) {
            if (root.holder().value().display().isPresent() && tabs.size() < maxTabs() - 1) {
                tabs.add(root.holder());
            }
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
            all.add(new Row(holder, Checklist.of(holder, progress), node.root().holder(),
                    progress != null && progress.hasProgress()));
        }
        if (tab == null && lastTab != null) {
            tab = tabs.stream().filter(h -> h.id().equals(lastTab)).findFirst().orElse(null);
        }
        if (tab != null && !tabs.contains(tab)) {
            tab = null;
        }
    }

    private boolean inTab(Row row) {
        return tab == null || tab.equals(row.root());
    }

    private static boolean passes(Row row, Filter f) {
        boolean done = row.list().complete();
        return switch (f) {
            case PROGRESS -> !done && row.started();
            case NOT_STARTED -> !done && !row.started();
            case DONE -> done;
            case ALL -> true;
        };
    }

    private void refresh() {
        collect();
        shown.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (Row row : all) {
            if (inTab(row) && passes(row, filter) && (query.isEmpty() || matches(row, query))) {
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
                || Checklist.description(row.holder()).getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private int count(Filter f) {
        int n = 0;
        for (Row row : all) {
            if (inTab(row) && passes(row, f)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        filterButtons.clear();
        int y = winY() + winH() + 4;
        for (Filter f : Filter.values()) {
            filterButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> {
                filter = f;
                lastFilter = f;
                scroll = 0;
                refresh();
            }).bounds(winX(), y, 60, BUTTON_H).build()));
        }
        search = new EditBox(font, winX(), y, 80, BUTTON_H, Component.translatable("advancementsmod.search"));
        search.setHint(Component.translatable("advancementsmod.search"));
        search.setResponder(text -> {
            scroll = 0;
            refresh();
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable(parent != null ? "gui.back" : "gui.done"),
                b -> onClose()).bounds(width / 2 - 100, y + BUTTON_H + 4, 200, BUTTON_H).build());
        refresh();
    }

    private void layoutFilters() {
        String[] keys = {"advancementsmod.filter.progress", "advancementsmod.filter.not_started",
                "advancementsmod.filter.done", "advancementsmod.filter.all"};
        int x = winX();
        for (int i = 0; i < filterButtons.size(); i++) {
            Filter f = Filter.values()[i];
            Button b = filterButtons.get(i);
            Component label = Component.translatable(keys[i], count(f));
            b.setMessage(label);
            b.setX(x);
            b.setWidth(font.width(label) + 12);
            b.active = f != filter;
            x += b.getWidth() + 2;
        }
        if (search != null) {
            int w = Math.max(50, winX() + winW() - x - 2);
            search.setX(winX() + winW() - w);
            search.setWidth(w);
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
        Theme.tiles(gfx, Theme.background(tab), inX(), inY(), inX() + inW(), inBottom());
        gfx.fill(inX(), inY(), inX() + inW(), inBottom(), 0x55000000);
        drawRows(gfx, mx, my);
        Theme.window(gfx, winX(), winY(), winW(), winH());
        drawTabs(gfx, mx, my);
        String title = tab == null ? Component.translatable("advancementsmod.list.title").getString()
                : Checklist.title(tab).getString();
        long done = all.stream().filter(r -> inTab(r) && r.list().complete()).count();
        long total = all.stream().filter(this::inTab).count();
        String right = Component.translatable("advancementsmod.list.done", done, total).getString();
        gfx.text(font, Theme.fit(font, title, winW() - 24 - font.width(right)), winX() + 8, winY() + 6, Theme.TITLE_TEXT, false);
        gfx.text(font, right, winX() + winW() - 8 - font.width(right), winY() + 6, Theme.TITLE_TEXT, false);
        super.extractRenderState(gfx, mx, my, a);
        drawTooltips(gfx, mx, my);
    }

    private void drawTabs(GuiGraphicsExtractor gfx, int mx, int my) {
        int count = tabs.size() + 1;
        for (int i = 0; i < count; i++) {
            AdvancementHolder h = i == 0 ? null : tabs.get(i - 1);
            boolean selected = h == null ? tab == null : h.equals(tab);
            String part = i == 0 ? "left" : "middle";
            Identifier sprite = Identifier.withDefaultNamespace("advancements/tab_above_" + part + (selected ? "_selected" : ""));
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tabX(i), tabY(), TAB_W, TAB_H);
            ItemStack icon = h == null ? new ItemStack(Items.KNOWLEDGE_BOOK) : Checklist.icon(h);
            gfx.item(icon, tabX(i) + 6, tabY() + 9);
        }
    }

    private void drawRows(GuiGraphicsExtractor gfx, int mx, int my) {
        int x0 = inX() + 4;
        int w = inW() - 12;
        int y0 = inY() + 3;
        if (shown.isEmpty()) {
            gfx.centeredText(font, Component.translatable("advancementsmod.empty"), inX() + inW() / 2, y0 + 12, Theme.GRAY);
            return;
        }
        int end = Math.min(shown.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            Row row = shown.get(i);
            int y = y0 + (i - scroll) * ROW_H;
            boolean done = row.list().complete();
            boolean hover = mx >= x0 && mx < x0 + w && my >= y && my < y + 26;
            Theme.ribbon(gfx, done, x0 + 13, y + 3, w - 13, 20);
            Theme.frame(gfx, Theme.frameType(row.holder()), done, Checklist.icon(row.holder()), x0, y);

            String count = TrackerHud.countText(row.list());
            int countW = font.width(count);
            gfx.text(font, count, x0 + w - 6 - countW, y + 9, Theme.WHITE);
            String title = Theme.fit(font, Checklist.title(row.holder()).getString(), w - 50 - countW);
            int titleColor = hover ? 0xFFFFFFA0 : Theme.WHITE;
            gfx.text(font, title, x0 + 31, y + 9, titleColor);
            if (Tracker.isTracked(row.holder())) {
                String t = Component.translatable("advancementsmod.tracking").getString();
                int tx = x0 + 31 + font.width(title) + 6;
                if (tx + font.width(t) < x0 + w - 12 - countW) {
                    gfx.text(font, t, tx, y + 9, Theme.GOLD);
                }
            }
            if (!done && row.list().mode() == Checklist.Mode.ALL) {
                Theme.slimBar(gfx, x0 + 31, y + 25, w - 37, 2, row.list().fraction(), false);
            }
        }
        int max = maxScroll();
        if (max > 0) {
            int h = visibleRows() * ROW_H;
            int thumb = Math.max(12, h * visibleRows() / shown.size());
            int top = y0 + (h - thumb) * scroll / max;
            int right = inX() + inW() - 2;
            gfx.fill(right - 3, y0, right, y0 + h, 0x66000000);
            gfx.fill(right - 3, top, right, top + thumb, 0xFFC6C6C6);
        }
    }

    private void drawTooltips(GuiGraphicsExtractor gfx, int mx, int my) {
        for (int i = 0; i <= tabs.size(); i++) {
            if (mx >= tabX(i) && mx < tabX(i) + TAB_W && my >= tabY() && my < tabY() + TAB_H - 4) {
                Component name = i == 0 ? Component.translatable("advancementsmod.list.every")
                        : Checklist.title(tabs.get(i - 1));
                gfx.setTooltipForNextFrame(font, name, mx, my);
                return;
            }
        }
        Row row = rowAt(mx, my);
        if (row != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Checklist.title(row.holder()).copy().withColor(Checklist.typeColor(row.holder())));
            for (var line : font.split(Checklist.description(row.holder()), 200)) {
                tip.add(Component.literal(toString(line)).withColor(0xFFC6C6C6));
            }
            if (row.root() != null && !row.root().equals(row.holder())) {
                tip.add(Checklist.title(row.root()).copy().withColor(0xFF808080));
            }
            gfx.setComponentTooltipForNextFrame(font, tip, mx, my);
        }
    }

    private static String toString(net.minecraft.util.FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private @Nullable Row rowAt(double mx, double my) {
        int x0 = inX() + 4;
        int y0 = inY() + 3;
        if (mx < x0 || mx >= x0 + inW() - 12 || my < y0) {
            return null;
        }
        int index = scroll + (int) ((my - y0) / ROW_H);
        if ((my - y0) % ROW_H >= 26 || index >= Math.min(shown.size(), scroll + visibleRows())) {
            return null;
        }
        return shown.get(index);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        for (int i = 0; i <= tabs.size(); i++) {
            if (event.x() >= tabX(i) && event.x() < tabX(i) + TAB_W && event.y() >= tabY() && event.y() < tabY() + TAB_H - 4) {
                tab = i == 0 ? null : tabs.get(i - 1);
                lastTab = tab == null ? null : tab.id();
                scroll = 0;
                refresh();
                return true;
            }
        }
        Row row = rowAt(event.x(), event.y());
        if (row != null) {
            DetailScreen.open(row.holder(), this);
            return true;
        }
        return false;
    }

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
