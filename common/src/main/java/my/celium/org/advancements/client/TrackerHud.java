package my.celium.org.advancements.client;

import java.util.List;

import org.joml.Matrix3x2fStack;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.advancements.AdvancementsMod.TrackerPosition;
import my.celium.org.client.MycelTheme;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the tracked advancement: a slim line with a progress bar right above the hearts and food
 * (the width of the hotbar), or a small card in a top corner listing what is still missing.
 */
public final class TrackerHud {
    private static final int BAR_W = 182;
    private static final int CARD_W = 160;
    private static final int CARD_LINES = 4;
    private static final int TRACK = 0x99000000;

    private TrackerHud() {
    }

    public static void render(GuiGraphicsExtractor gfx) {
        Minecraft mc = Minecraft.getInstance();
        if (!AdvancementsMod.isModEnabled() || !AdvancementsMod.showTracker.get() || mc.player == null) {
            return;
        }
        AdvancementHolder holder = Tracker.trackedHolder();
        if (holder == null) {
            return;
        }
        Checklist list = Tracker.checklist(holder);
        if (TrackerPosition.current() == TrackerPosition.ABOVE_HOTBAR) {
            if (!Tracker.itemNameShowing()) {
                drawLine(gfx, mc, holder, list);
            }
        } else {
            drawCard(gfx, mc, holder, list, TrackerPosition.current() == TrackerPosition.TOP_RIGHT);
        }
    }

    // ------------------------------------------------------------------ above the hotbar

    private static void drawLine(GuiGraphicsExtractor gfx, Minecraft mc, AdvancementHolder holder, Checklist list) {
        Font font = mc.font;
        int x0 = gfx.guiWidth() / 2 - BAR_W / 2;
        int bottom = gfx.guiHeight() - statusHeight(mc) - 2;
        int barY = bottom - 2;
        int textY = barY - 10;

        String count = countText(list);
        int countW = font.width(count);
        smallIcon(gfx, Checklist.icon(holder), x0, textY);
        Component flash = Tracker.flash();
        int titleRoom = BAR_W - 11 - countW - 6;
        if (flash != null) {
            String text = MycelTheme.fit(font, "+ " + flash.getString(), titleRoom);
            gfx.text(font, text, x0 + 11, textY, MycelTheme.GOOD);
        } else {
            String title = MycelTheme.fit(font, Checklist.title(holder).getString(), titleRoom);
            gfx.text(font, title, x0 + 11, textY, MycelTheme.HEADING);
        }
        gfx.text(font, count, x0 + BAR_W - countW, textY, list.complete() ? MycelTheme.GOOD : MycelTheme.TEXT);
        bar(gfx, x0, barY, BAR_W, 2, list, holder);
    }

    /**
     * How far the hearts, armor, food and air reach up from the bottom, like vanilla lays them out.
     * In creative and spectator there is only the hotbar (and the level number).
     */
    static int statusHeight(Minecraft mc) {
        Player player = mc.player;
        if (mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return player != null && player.isSpectator() ? 24 : 35;
        }
        float maxHealth = Math.max(player.getMaxHealth(), player.getHealth());
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int rows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        int left = 39 + (rows - 1) * rowHeight + (player.getArmorValue() > 0 ? 10 : 0);

        int right = 39;
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity living && living.showVehicleHealth()) {
            int hearts = Math.min(30, (int) (living.getMaxHealth() + 0.5F) / 2);
            right = 39 + (Mth.ceil(hearts / 10.0F) - 1) * 10;
        } else if (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply()) {
            right += 10;
        }
        return Math.max(left, right);
    }

    // ------------------------------------------------------------------ corner card

    private static void drawCard(GuiGraphicsExtractor gfx, Minecraft mc, AdvancementHolder holder, Checklist list, boolean right) {
        Font font = mc.font;
        List<Checklist.Entry> missing = list.mode() == Checklist.Mode.ALL ? list.missing() : List.of();
        int shown = Math.min(CARD_LINES, missing.size());
        boolean more = missing.size() > shown;
        int h = 6 + 16 + 4 + 2 + 4 + shown * 10 + (more ? 10 : 0) + (shown > 0 ? 2 : 0);
        int x0 = right ? gfx.guiWidth() - CARD_W - 4 : 4;
        int y0 = 4;
        gfx.fill(x0, y0, x0 + CARD_W, y0 + h, 0xB0101010);
        gfx.fill(x0, y0, x0 + 2, y0 + h, Checklist.typeColor(holder));

        int tx = x0 + 6;
        gfx.item(Checklist.icon(holder), tx, y0 + 5);
        String count = countText(list);
        int countW = font.width(count);
        Component flash = Tracker.flash();
        String title = flash != null ? "+ " + flash.getString() : Checklist.title(holder).getString();
        gfx.text(font, MycelTheme.fit(font, title, CARD_W - 30 - countW - 6), tx + 20, y0 + 9,
                flash != null ? MycelTheme.GOOD : MycelTheme.HEADING);
        gfx.text(font, count, x0 + CARD_W - 5 - countW, y0 + 9, list.complete() ? MycelTheme.GOOD : MycelTheme.TEXT);
        int barY = y0 + 6 + 16 + 3;
        bar(gfx, tx, barY, CARD_W - 11, 2, list, holder);

        int y = barY + 6;
        for (int i = 0; i < shown; i++) {
            gfx.text(font, MycelTheme.fit(font, missing.get(i).name().getString(), CARD_W - 14), tx, y, MycelTheme.DIM);
            y += 10;
        }
        if (more) {
            gfx.text(font, Component.translatable("advancementsmod.hud.more", missing.size() - shown), tx, y, MycelTheme.FAINT);
        }
    }

    // ------------------------------------------------------------------ shared

    static String countText(Checklist list) {
        if (list.complete()) {
            return Component.translatable("advancementsmod.done").getString();
        }
        if (list.mode() == Checklist.Mode.ALL) {
            return list.doneCount() + "/" + list.total();
        }
        return Component.translatable("advancementsmod.not_done").getString();
    }

    static void bar(GuiGraphicsExtractor gfx, int x, int y, int w, int h, Checklist list, AdvancementHolder holder) {
        gfx.fill(x, y, x + w, y + h, TRACK);
        int filled = Math.round(w * list.fraction());
        if (filled > 0) {
            gfx.fill(x, y, x + filled, y + h, list.complete() ? MycelTheme.GOOD : Checklist.typeColor(holder));
        }
    }

    /** An item drawn at half size (8x8). */
    static void smallIcon(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y) {
        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(0.5f, 0.5f);
        gfx.item(stack, 0, 0);
        pose.popMatrix();
    }
}
