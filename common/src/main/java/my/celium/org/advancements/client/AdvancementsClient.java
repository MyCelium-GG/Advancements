package my.celium.org.advancements.client;

import my.celium.org.advancements.AdvancementsMod;
import net.minecraft.client.Minecraft;

/** Client entry: loaders call {@link #tick} every client tick. */
public final class AdvancementsClient {
    private AdvancementsClient() {
    }

    public static void tick(Minecraft mc) {
        if (AdvancementsMod.isModEnabled()) {
            Tracker.tick(mc);
        }
    }
}
