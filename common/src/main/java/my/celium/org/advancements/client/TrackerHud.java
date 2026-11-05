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
import net.minecraft.util.Util;
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
        Checklist list = Tracker.trackedChecklist();
        if (list == null) {
            return;
        }
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
        long now = Util.getMillis();
        float fraction = smoothFraction(holder, list.fraction());

        // Entry: grows in when tracking starts. Exit: shrinks away when a finished one is dropped.
        float scale = 1f;
        long trackAge = now - Tracker.trackStart();
        if (Tracker.trackStart() > 0 && trackAge < ENTER_MS) {
            scale = easeOutBack(trackAge / (float) ENTER_MS);
        }
        long completeAge = Tracker.completeStart() > 0 ? now - Tracker.completeStart() : -1;
        if (completeAge >= 0 && AdvancementsMod.untrackWhenDone.get() && completeAge > Tracker.DONE_MS - EXIT_MS) {
            float p = Math.min(1f, (completeAge - (Tracker.DONE_MS - EXIT_MS)) / (float) EXIT_MS);
            scale = 1f - p * p;
        }
        if (scale <= 0.01f) {
            return;
        }

        int cx = gfx.guiWidth() / 2;
        int frameY = gfx.guiHeight() - statusHeight(mc) - 1 - FRAME;
        int cy = frameY + FRAME / 2;
        var pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(scale, scale);
        pose.translate(-cx, -cy);

        int barY = cy - 1;
        int left = cx - WIDTH / 2;
        int sideW = WIDTH / 2 - FRAME / 2 - 2;
        boolean complete = list.complete();

        // The bar runs from the left end to the right end, passing behind the frame.
        int filled = Math.round(sideW * 2 * fraction);
        Theme.slimBar(gfx, left, barY, sideW, 2, Math.min(1f, filled / (float) sideW), complete);
        Theme.slimBar(gfx, cx + FRAME / 2 + 2, barY, sideW, 2, Math.max(0f, (filled - sideW) / (float) sideW), complete);
        drawGain(gfx, left, cx + FRAME / 2 + 2, sideW, barY, list, now);

        long flashAge = Tracker.flashStart() > 0 ? now - Tracker.flashStart() : -1;
        boolean celebrating = completeAge >= 0 && completeAge < Tracker.DONE_MS;
        long burstAge = celebrating ? completeAge : flashAge;
        boolean big = celebrating;

        // Glow and sparkles behind and around the frame.
        if (burstAge >= 0 && burstAge < BURST_MS) {
            float p = burstAge / (float) BURST_MS;
            glow(gfx, cx, cy, p, big);
            ring(gfx, cx, cy, Math.min(1f, burstAge / (float) RING_MS), big);
            sparkles(gfx, cx, cy, p, big, Tracker.flashStart() + Tracker.completeStart());
        } else if (celebrating) {
            glow(gfx, cx, cy, 0.6f, true);
        }

        // The frame pops when something gets done.
        float pop = 1f;
        if (burstAge >= 0 && burstAge < POP_MS) {
            float p = burstAge / (float) POP_MS;
            pop = 1f + (big ? 0.55f : 0.35f) * (float) Math.sin(Math.PI * p) * (1f - p * 0.3f);
        }
        Component flash = Tracker.flash();
        ItemStack icon = flash != null && Tracker.flashIcon() != null && !celebrating ? Tracker.flashIcon() : Checklist.icon(holder);
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(pop, pop);
        pose.translate(-cx, -cy);
        Theme.frame(gfx, Theme.frameType(holder), complete || flash != null, icon, cx - FRAME / 2, frameY);
        pose.popMatrix();

        // Left: the title, or what just got done. Right: the count.
        int textY = barY - 11;
        String title;
        int titleColor;
        if (celebrating) {
            title = Component.translatable("advancements.toast." + Theme.frameType(holder)).getString();
            titleColor = Checklist.typeColor(holder);
        } else if (flash != null) {
            title = "+ " + flash.getString();
            titleColor = Theme.GOLD;
        } else {
            title = Checklist.title(holder).getString();
            titleColor = Theme.WHITE;
        }
        title = Theme.fit(font, title, 150);
        int textAlpha = 255;
        if (flashAge >= 0 && flashAge < FADE_MS && !celebrating) {
            textAlpha = Math.max(30, Math.round(255 * flashAge / (float) FADE_MS));
        }
        gfx.text(font, title, cx - FRAME / 2 - 3 - font.width(title), textY, (textAlpha << 24) | (titleColor & 0xFFFFFF));

        String count = countText(list);
        int countX = cx + FRAME / 2 + 3;
        float countPop = 1f;
        if (flashAge >= 0 && flashAge < POP_MS) {
            countPop = 1f + 0.3f * (float) Math.sin(Math.PI * flashAge / POP_MS);
        }
        pose.pushMatrix();
        pose.translate(countX, textY + 4);
        pose.scale(countPop, countPop);
        pose.translate(-countX, -(textY + 4));
        int countColor = complete ? Theme.GOLD : flashAge >= 0 && flashAge < POP_MS ? Theme.WHITE : Theme.XP_GREEN;
        Theme.outlined(gfx, font, count, countX, textY, countColor);
        pose.popMatrix();

        pose.popMatrix();
    }

    // ------------------------------------------------------------------ animation pieces

    private static final long ENTER_MS = 320;
    private static final long EXIT_MS = 280;
    private static final long POP_MS = 420;
    private static final long BURST_MS = 900;
    private static final long FADE_MS = 220;
    private static final long GAIN_MS = 900;
    private static final long RING_MS = 500;

    private static float shownFraction = -1f;
    private static net.minecraft.resources.@org.jspecify.annotations.Nullable Identifier shownFor;
    private static long lastFrame;
    private static float gainFrom;
    private static float gainTo;
    private static long gainStart;

    /** The bar slides to its new value instead of jumping; the gained piece is remembered for the flash. */
    private static float smoothFraction(AdvancementHolder holder, float target) {
        long now = Util.getMillis();
        if (!holder.id().equals(shownFor) || shownFraction < 0f) {
            shownFor = holder.id();
            shownFraction = target;
            lastFrame = now;
            gainStart = 0;
            return target;
        }
        if (target > shownFraction + 0.0001f && (gainStart == 0 || now - gainStart > GAIN_MS || target > gainTo)) {
            gainFrom = gainStart != 0 && now - gainStart <= GAIN_MS ? gainFrom : shownFraction;
            gainTo = target;
            gainStart = now;
        }
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        shownFraction += (target - shownFraction) * (1f - (float) Math.exp(-dt * 7f));
        if (Math.abs(target - shownFraction) < 0.001f) {
            shownFraction = target;
        }
        return shownFraction;
    }

    /** A white flash over the piece of bar that was just gained, fading out. */
    private static void drawGain(GuiGraphicsExtractor gfx, int leftX, int rightX, int sideW, int y, Checklist list, long now) {
        if (gainStart == 0 || now - gainStart > GAIN_MS) {
            return;
        }
        float p = (now - gainStart) / (float) GAIN_MS;
        int alpha = Math.round(220 * (1f - p) * (1f - p));
        int from = Math.round(sideW * 2 * gainFrom);
        int to = Math.round(sideW * 2 * Math.min(gainTo, shownFraction));
        for (int px = from; px < to; px++) {
            int x = px < sideW ? leftX + px : rightX + (px - sideW);
            gfx.fill(x, y, x + 1, y + 2, (alpha << 24) | 0xFFFFFF);
        }
    }

    /** A soft gold glow behind the frame. */
    private static void glow(GuiGraphicsExtractor gfx, int cx, int cy, float p, boolean big) {
        float strength = (1f - p);
        int max = big ? 22 : 17;
        for (int r = max; r > 13; r -= 2) {
            int alpha = Math.round(strength * (big ? 110 : 80) * (1f - (r - 13) / (float) (max - 13)));
            if (alpha > 0) {
                gfx.fill(cx - r, cy - r, cx + r, cy + r, (alpha << 24) | 0xFFD84A);
            }
        }
    }

    /** A gold ring that grows out of the frame and fades. */
    private static void ring(GuiGraphicsExtractor gfx, int cx, int cy, float p, boolean big) {
        float ease = 1f - (1f - p) * (1f - p);
        int r = 13 + Math.round((big ? 22 : 14) * ease);
        int alpha = Math.round(230 * (1f - p));
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | 0xFFD84A;
        gfx.fill(cx - r, cy - r, cx + r, cy - r + 1, color);
        gfx.fill(cx - r, cy + r - 1, cx + r, cy + r, color);
        gfx.fill(cx - r, cy - r + 1, cx - r + 1, cy + r - 1, color);
        gfx.fill(cx + r - 1, cy - r + 1, cx + r, cy + r - 1, color);
    }

    /** Little gold and white sparks flying out of the frame. */
    private static void sparkles(GuiGraphicsExtractor gfx, int cx, int cy, float p, boolean big, long seed) {
        int count = big ? 20 : 12;
        java.util.Random random = new java.util.Random(seed);
        float ease = 1f - (1f - p) * (1f - p) * (1f - p);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i / count) + random.nextDouble() * 0.5;
            float distance = (big ? 34 : 24) * (0.6f + random.nextFloat() * 0.5f) * ease + 10;
            int x = cx + (int) Math.round(Math.cos(angle) * distance);
            int y = cy + (int) Math.round(Math.sin(angle) * distance * 0.75);
            int alpha = Math.round(255 * (1f - p));
            int color = (i % 3 == 0) ? 0xFFFFFF : 0xFFD84A;
            int size = p < 0.35f ? 3 : p < 0.7f ? 2 : 1;
            gfx.fill(x, y, x + size, y + size, (alpha << 24) | color);
        }
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float x = Math.max(0f, Math.min(1f, t)) - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
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
