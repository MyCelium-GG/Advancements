package my.celium.org.advancements.client;

import org.jspecify.annotations.Nullable;

import my.celium.org.client.MycelScreens;
import my.celium.org.client.MycelTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The shared frame of this mod's screens: a wiki-style card with a red title bar. */
abstract class PanelScreen extends Screen {
    static final int TITLE_BAR = 20;
    static final int PAD = 8;

    protected final @Nullable Screen parent;

    PanelScreen(Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    int panelW() {
        return Math.min(width - 12, 400);
    }

    int panelH() {
        return Math.min(height - 12, 260);
    }

    int px() {
        return (width - panelW()) / 2;
    }

    int py() {
        return (height - panelH()) / 2;
    }

    int innerX() {
        return px() + PAD;
    }

    int innerW() {
        return panelW() - PAD * 2;
    }

    int bottomRowY() {
        return py() + panelH() - PAD - FlatButton.HEIGHT;
    }

    void drawFrame(GuiGraphicsExtractor gfx, String heading, String right) {
        int x0 = px();
        int y0 = py();
        int x1 = x0 + panelW();
        MycelTheme.panel(gfx, x0, y0, x1, y0 + panelH());
        gfx.fill(x0 + 2, y0 + 2, x1 - 2, y0 + TITLE_BAR, MycelTheme.ACCENT_DARK);
        gfx.fill(x0 + 2, y0 + TITLE_BAR, x1 - 2, y0 + TITLE_BAR + 1, MycelTheme.HEADER_EDGE);
        int rightW = font.width(right);
        gfx.text(font, MycelTheme.fit(font, heading, panelW() - PAD * 3 - rightW), x0 + PAD, y0 + 7, MycelTheme.HEADING);
        gfx.text(font, right, x1 - PAD - rightW, y0 + 7, MycelTheme.HEADING);
    }

    EditBox searchBox(int x, int y, int w, Component hint, Runnable onChange) {
        EditBox box = new EditBox(font, x + 4, y + 3, w - 8, 9, hint);
        box.setBordered(false);
        box.setMaxLength(64);
        box.setHint(MycelTheme.colored(hint.getString(), MycelTheme.FAINT));
        box.setResponder(text -> onChange.run());
        return box;
    }

    void drawField(GuiGraphicsExtractor gfx, EditBox box) {
        int x = box.getX() - 4;
        int y = box.getY() - 3;
        int w = box.getWidth() + 8;
        gfx.fill(x, y, x + w, y + 14, box.isFocused() ? MycelTheme.ACCENT : MycelTheme.BORDER);
        gfx.fill(x + 1, y + 1, x + w - 1, y + 13, MycelTheme.SURFACE);
    }

    void drawBox(GuiGraphicsExtractor gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, MycelTheme.BORDER);
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, MycelTheme.SURFACE);
    }

    static void scrollbar(GuiGraphicsExtractor gfx, int right, int top, int height, int scroll, int maxScroll, int visible, int total) {
        if (maxScroll <= 0) {
            return;
        }
        int track = height - 2;
        int thumb = Math.max(10, track * visible / Math.max(1, total));
        int y = top + 1 + (track - thumb) * scroll / maxScroll;
        gfx.fill(right - 3, y, right - 1, y + thumb, MycelTheme.DIM);
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
