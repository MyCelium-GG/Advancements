package my.celium.org.advancements.client;

import java.util.List;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.advancements.AdvancementsMod.TrackerPosition;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the tracked advancement. Above the hotbar: the advancement's frame in the middle with the
 * progress bar running out to both sides, the title on the left and the count on the right. In a
 * top corner: an advancement toast with what is still missing below it.
 */
public final class TrackerHud {
    private static final int WIDTH = 182;
    private static final int FRAME = 26;
    private static final int CARD_LINES = 4;

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
            // Vanilla's held item name and action bar sit in the same spot; give them the room.
            if (!Tracker.itemNameShowing() && !Tracker.actionBarShowing()) {
                drawCenter(gfx, mc, holder, list);
            }
        } else {
            drawToast(gfx, mc, holder, list, TrackerPosition.current() == TrackerPosition.TOP_RIGHT);
        }
    }

    // ------------------------------------------------------------------ above the hotbar

    private static void drawCenter(GuiGraphicsExtractor gfx, Minecraft mc, AdvancementHolder holder, Checklist list) {
        Font font = mc.font;
        int cx = gfx.guiWidth() / 2;
        int frameX = cx - FRAME / 2;
        int frameY = gfx.guiHeight() - statusHeight(mc) - 1 - FRAME;
        int barY = frameY + FRAME / 2 - 1;
        int left = cx - WIDTH / 2;
        int sideW = WIDTH / 2 - FRAME / 2 - 2;

        // The bar runs from the left end to the right end, passing behind the frame.
        float fraction = list.fraction();
        int filled = Math.round(sideW * 2 * fraction);
        Theme.slimBar(gfx, left, barY, sideW, 2, Math.min(1f, filled / (float) sideW), list.complete());
        Theme.slimBar(gfx, cx + FRAME / 2 + 2, barY, sideW, 2, Math.max(0f, (filled - sideW) / (float) sideW), list.complete());

        Component flash = Tracker.flash();
        ItemStack icon = flash != null && Tracker.flashIcon() != null ? Tracker.flashIcon() : Checklist.icon(holder);
        Theme.frame(gfx, Theme.frameType(holder), list.complete() || flash != null, icon, frameX, frameY);

        int textY = barY - 11;
        String title = flash != null ? "+ " + flash.getString() : Checklist.title(holder).getString();
        title = Theme.fit(font, title, 150);
        gfx.text(font, title, frameX - 3 - font.width(title), textY, flash != null ? Theme.GOLD : Theme.WHITE);
        String count = countText(list);
        Theme.outlined(gfx, font, count, frameX + FRAME + 3, textY, list.complete() ? Theme.GOLD : Theme.XP_GREEN);
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

    // ------------------------------------------------------------------ corner toast

    private static void drawToast(GuiGraphicsExtractor gfx, Minecraft mc, AdvancementHolder holder, Checklist list, boolean right) {
        Font font = mc.font;
        int x0 = right ? gfx.guiWidth() - 160 - 4 : 4;
        int y0 = 4;
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, Theme.TOAST, x0, y0, 160, 32);
        Component flash = Tracker.flash();
        ItemStack icon = flash != null && Tracker.flashIcon() != null ? Tracker.flashIcon() : Checklist.icon(holder);
        gfx.item(icon, x0 + 8, y0 + 8);
        String title = flash != null ? "+ " + flash.getString() : Checklist.title(holder).getString();
        gfx.text(font, Theme.fit(font, title, 124), x0 + 30, y0 + 7,
                flash != null ? Theme.GOLD : Checklist.typeColor(holder), false);
        String count = countText(list);
        gfx.text(font, count, x0 + 30, y0 + 18, Theme.WHITE, false);
        int barX = x0 + 34 + font.width(count);
        Theme.slimBar(gfx, barX, y0 + 21, x0 + 150 - barX, 2, list.fraction(), list.complete());

        List<Checklist.Entry> missing = list.mode() == Checklist.Mode.ALL ? list.missing() : List.of();
        int shown = Math.min(CARD_LINES, missing.size());
        int y = y0 + 36;
        for (int i = 0; i < shown; i++) {
            Checklist.Entry e = missing.get(i);
            Theme.smallIcon(gfx, e.icon(), x0 + 4, y);
            gfx.text(font, Theme.fit(font, e.name().getString(), 140), x0 + 15, y, Theme.WHITE);
            y += 10;
        }
        if (missing.size() > shown) {
            gfx.text(font, Component.translatable("advancementsmod.hud.more", missing.size() - shown), x0 + 15, y, Theme.GRAY);
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
}
