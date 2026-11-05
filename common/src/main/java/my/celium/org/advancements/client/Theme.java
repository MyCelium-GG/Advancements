package my.celium.org.advancements.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Drawing in the look of vanilla's advancements screen, with vanilla's own textures: the window,
 * the tab backgrounds, the task/goal/challenge frames, the gold and blue title ribbons and an
 * experience-bar style progress bar.
 */
final class Theme {
    static final Identifier WINDOW = Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
    static final Identifier STONE = Identifier.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png");
    private static final Identifier BOX_OBTAINED = Identifier.withDefaultNamespace("advancements/box_obtained");
    private static final Identifier BOX_UNOBTAINED = Identifier.withDefaultNamespace("advancements/box_unobtained");
    private static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
    private static final Identifier XP_BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier XP_PROGRESS = Identifier.withDefaultNamespace("hud/experience_bar_progress");
    static final Identifier TOAST = Identifier.withDefaultNamespace("toast/advancement");

    /** Window border sizes in window.png (252x140 at 0,0). */
    static final int BORDER = 9;
    static final int TITLE = 18;
    private static final int WIN_W = 252;
    private static final int WIN_H = 140;

    /** Title text in the window's gray title row, as vanilla draws it. */
    static final int TITLE_TEXT = 0xFF404040;
    static final int WHITE = 0xFFFFFFFF;
    static final int GRAY = 0xFFA0A0A0;
    static final int GOLD = 0xFFFFD84A;
    static final int XP_GREEN = 0xFF80FF20;
    static final int CHALLENGE = 0xFFC86BFF;

    private Theme() {
    }

    // ------------------------------------------------------------------ window

    /** Vanilla's advancements window stretched to any size; the inside is left for the caller. */
    static void window(GuiGraphicsExtractor gfx, int x, int y, int w, int h) {
        int midW = WIN_W - BORDER * 2;
        int midH = WIN_H - TITLE - BORDER;
        // Corners.
        blit(gfx, x, y, 0, 0, BORDER, TITLE);
        blit(gfx, x + w - BORDER, y, WIN_W - BORDER, 0, BORDER, TITLE);
        blit(gfx, x, y + h - BORDER, 0, WIN_H - BORDER, BORDER, BORDER);
        blit(gfx, x + w - BORDER, y + h - BORDER, WIN_W - BORDER, WIN_H - BORDER, BORDER, BORDER);
        // Top and bottom edges, tiled.
        for (int dx = BORDER; dx < w - BORDER; dx += midW) {
            int seg = Math.min(midW, w - BORDER - dx);
            blit(gfx, x + dx, y, BORDER, 0, seg, TITLE);
            blit(gfx, x + dx, y + h - BORDER, BORDER, WIN_H - BORDER, seg, BORDER);
        }
        // Left and right edges, tiled.
        for (int dy = TITLE; dy < h - BORDER; dy += midH) {
            int seg = Math.min(midH, h - BORDER - dy);
            blit(gfx, x, y + dy, 0, TITLE, BORDER, seg);
            blit(gfx, x + w - BORDER, y + dy, WIN_W - BORDER, TITLE, BORDER, seg);
        }
    }

    private static void blit(GuiGraphicsExtractor gfx, int x, int y, int u, int v, int w, int h) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, WINDOW, x, y, u, v, w, h, 256, 256);
    }

    /** The tab background tiled over an area, like the inside of vanilla's window. */
    static void tiles(GuiGraphicsExtractor gfx, Identifier texture, int x0, int y0, int x1, int y1) {
        gfx.enableScissor(x0, y0, x1, y1);
        for (int y = y0; y < y1; y += 16) {
            for (int x = x0; x < x1; x += 16) {
                gfx.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 16, 16, 16, 16);
            }
        }
        gfx.disableScissor();
    }

    /** The background of the tab an advancement belongs to (stone when unknown). */
    static Identifier background(@Nullable AdvancementHolder root) {
        if (root == null) {
            return STONE;
        }
        DisplayInfo d = Checklist.display(root);
        return d == null ? STONE : Display.background(d).orElse(STONE);
    }

    // ------------------------------------------------------------------ frames and ribbons

    /** "task", "goal" or "challenge". */
    static String frameType(AdvancementHolder holder) {
        DisplayInfo d = Checklist.display(holder);
        return d == null ? "task" : Display.type(d).getSerializedName();
    }

    /** A 26x26 advancement frame with the icon inside, gold when obtained. */
    static void frame(GuiGraphicsExtractor gfx, String type, boolean obtained, ItemStack icon, int x, int y) {
        Identifier sprite = Identifier.withDefaultNamespace("advancements/" + type + "_frame_"
                + (obtained ? "obtained" : "unobtained"));
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 26, 26);
        gfx.item(icon, x + 5, y + 5);
    }

    /** The frame at twice the size, for the big icon at the top of the details. */
    static void bigFrame(GuiGraphicsExtractor gfx, String type, boolean obtained, ItemStack icon, int x, int y) {
        var pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(2f, 2f);
        frame(gfx, type, obtained, icon, 0, 0);
        pose.popMatrix();
    }

    /** The ribbon vanilla shows behind a hovered advancement's title: gold when done, blue when not. */
    static void ribbon(GuiGraphicsExtractor gfx, boolean obtained, int x, int y, int w, int h) {
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, obtained ? BOX_OBTAINED : BOX_UNOBTAINED, x, y, w, h);
    }

    /** The dark box vanilla shows under a hovered advancement's description. */
    static void darkBox(GuiGraphicsExtractor gfx, int x, int y, int w, int h) {
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX, x, y, w, h);
    }

    // ------------------------------------------------------------------ progress

    /** The experience bar at its real size (182x5), filled to {@code fraction}. */
    static void xpBar(GuiGraphicsExtractor gfx, int x, int y, float fraction) {
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, XP_BACKGROUND, x, y, 182, 5);
        int filled = Math.round(182 * Math.max(0f, Math.min(1f, fraction)));
        if (filled > 0) {
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, XP_PROGRESS, 182, 5, 0, 0, x, y, filled, 5);
        }
    }

    /** A slim bar in the experience bar's colors, for any width. */
    static void slimBar(GuiGraphicsExtractor gfx, int x, int y, int w, int h, float fraction, boolean done) {
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        gfx.fill(x, y, x + w, y + h, 0xFF2B2B2B);
        int filled = Math.round(w * Math.max(0f, Math.min(1f, fraction)));
        if (filled > 0) {
            gfx.fill(x, y, x + filled, y + h, done ? GOLD : XP_GREEN);
        }
    }

    /** Text with a black outline, like the experience level number. */
    static void outlined(GuiGraphicsExtractor gfx, Font font, String text, int x, int y, int color) {
        gfx.text(font, text, x + 1, y, 0xFF000000, false);
        gfx.text(font, text, x - 1, y, 0xFF000000, false);
        gfx.text(font, text, x, y + 1, 0xFF000000, false);
        gfx.text(font, text, x, y - 1, 0xFF000000, false);
        gfx.text(font, text, x, y, color, false);
    }

    /** An item drawn at half size (8x8). */
    static void smallIcon(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y) {
        var pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(0.5f, 0.5f);
        gfx.item(stack, 0, 0);
        pose.popMatrix();
    }

    /** Cuts {@code text} with an ellipsis to fit. */
    static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }
}
