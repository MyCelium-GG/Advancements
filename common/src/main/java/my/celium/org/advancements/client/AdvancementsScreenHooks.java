package my.celium.org.advancements.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** What the advancements screen mixin adds, kept out of the mixin. */
public final class AdvancementsScreenHooks {
    private static final int WINDOW_W = 252;
    private static final int WINDOW_H = 140;

    private AdvancementsScreenHooks() {
    }

    public static void openDetails(AdvancementHolder holder, Screen screen) {
        DetailScreen.open(holder, screen);
    }

    /** "Click an advancement for details", below the window. */
    public static void drawHint(GuiGraphicsExtractor gfx, Font font, int width, int height) {
        int y = (height + WINDOW_H) / 2 + 3 + 6;
        String hint = Theme.fit(font, Component.translatable("advancementsmod.click_hint").getString(),
                WINDOW_W);
        gfx.centeredText(font, hint, width / 2, y, 0xFFFFFFFF);
    }
}
