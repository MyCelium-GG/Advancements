package my.celium.org.advancements.client;

import com.mojang.blaze3d.platform.InputConstants;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.client.MycelScreens;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Client entry: the keys and the tick. Loaders call {@link #tick} every client tick. */
public final class AdvancementsClient {
    /** Opens the list of every advancement. Unbound by default; the advancements screen has a button. */
    public static final KeyMapping LIST_KEY = new KeyMapping("key.advancements.list",
            InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);
    /** Opens the tracked advancement's details. Unbound by default. */
    public static final KeyMapping TRACKED_KEY = new KeyMapping("key.advancements.tracked",
            InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);

    private AdvancementsClient() {
    }

    public static void tick(Minecraft mc) {
        if (!AdvancementsMod.isModEnabled()) {
            return;
        }
        Tracker.tick(mc);
        while (LIST_KEY.consumeClick()) {
            if (mc.player != null && MycelScreens.current() == null) {
                ListScreen.open(null);
            }
        }
        while (TRACKED_KEY.consumeClick()) {
            AdvancementHolder holder = Tracker.trackedHolder();
            if (mc.player != null && MycelScreens.current() == null) {
                if (holder != null) {
                    DetailScreen.open(holder, null);
                } else {
                    ListScreen.open(null);
                }
            }
        }
    }
}
