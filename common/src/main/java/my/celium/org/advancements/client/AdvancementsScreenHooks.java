package my.celium.org.advancements.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** What the advancements screen mixin adds, kept out of the mixin. */
public final class AdvancementsScreenHooks {
    private static final int WINDOW_W = 252;
    private static final int WINDOW_H = 140;
    private static final int BUTTON_W = 100;

    private AdvancementsScreenHooks() {
    }

    /** "All advancements", right below the window's right edge. */
    public static AbstractButton listButton(Screen screen, int width, int height) {
        int x = (width + WINDOW_W) / 2 - BUTTON_W;
        int y = (height + WINDOW_H) / 2 + 3;
        return Button.builder(Component.translatable("advancementsmod.all"), b -> ListScreen.open(screen))
                .bounds(x, y, BUTTON_W, 20).build();
    }

    public static void openDetails(AdvancementHolder holder, Screen screen) {
        DetailScreen.open(holder, screen);
    }

    /** "Click one for details", left below the window. */
    public static void drawHint(GuiGraphicsExtractor gfx, Font font, int width, int height) {
        int x = (width - WINDOW_W) / 2;
        int y = (height + WINDOW_H) / 2 + 3 + 6;
        String hint = Theme.fit(font, Component.translatable("advancementsmod.click_hint").getString(),
                WINDOW_W - BUTTON_W - 6);
        gfx.text(font, hint, x, y, 0xFFFFFFFF);
    }
}
