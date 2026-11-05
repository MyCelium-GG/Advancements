package my.celium.org.advancements;

import my.celium.org.stats.ModStats;

/**
 * Anonymous stats for the FastStats project {@code advancements}, sent through Mycel under its
 * consent: which views get opened, which advancements people track, and the settings.
 */
public final class AdvancementStats {
    /** Project token from faststats.dev (public, ships in the jar). */
    private static final String TOKEN = "a151ead18abcdb157eed7cf35480a95a";

    private static volatile ModStats stats;

    private AdvancementStats() {
    }

    static void start() {
        if (TOKEN.isEmpty()) {
            return;
        }
        stats = ModStats.builder(AdvancementsMod.META, TOKEN)
                .bool("enabled", () -> AdvancementsMod.enabled.get())
                .bool("show_tracker", () -> AdvancementsMod.showTracker.get())
                .string("tracker_position", () -> AdvancementsMod.TrackerPosition.current().id)
                .bool("untrack_when_done", () -> AdvancementsMod.untrackWhenDone.get())
                .bool("click_for_details", () -> AdvancementsMod.clickForDetails.get())
                .bool("sounds", () -> AdvancementsMod.sounds.get())
                .counters("tracked")
                .errors("my.celium.org.advancements.")
                .start();
    }

    /** Counts a feature use. */
    public static void count(String what) {
        ModStats current = stats;
        if (current != null) {
            current.count(what);
        }
    }

    /** Counts {@code key} in the counter map {@code id}. */
    public static void count(String id, String key) {
        ModStats current = stats;
        if (current != null) {
            current.count(id, key);
        }
    }
}
