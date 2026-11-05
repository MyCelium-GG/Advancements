package my.celium.org.advancements;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigBuilder;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.metadata.ModMetadata;

/**
 * Advancements: a better look at the advancements you already have. Click an advancement to see
 * which parts are done and which are missing (every biome for Adventuring Time, every food for A
 * Balanced Diet, ...), browse all of them in one sorted list, and track one above the hotbar.
 *
 * <p>Client side only. The server already sends every criterion and whether it is done, so this
 * works on any server, vanilla ones included. The client part lives in {@code client/}.
 */
public final class AdvancementsMod {
    public static final String ID = "advancements";

    public static final ModMetadata META = ModMetadata.builder(ID, "Advancements", "1.0.0")
            .author("MyCelium")
            .website("https://www.curseforge.com/minecraft/mc-mods/advancements")
            .updateUrl("https://wiki.mycelium-gg.org/updates/advancements.json")
            .build();

    public static ConfigValue<Boolean> enabled;
    public static ConfigValue<Boolean> showTracker;
    public static ConfigValue<String> trackerPosition;
    public static ConfigValue<Boolean> untrackWhenDone;
    public static ConfigValue<Boolean> clickForDetails;
    public static ConfigValue<Boolean> sounds;
    public static MycelConfig config;

    private AdvancementsMod() {
    }

    /** Called once from each loader's entrypoint. */
    public static void init() {
        Mycel.initialize();
        Mycel.registerMod(META);

        ConfigBuilder builder = Mycel.config(META);
        enabled = builder.booleanValue("enabled", true,
                "Master switch for Advancements.");
        showTracker = builder.booleanValue("showTracker", true,
                "Show the tracked advancement on screen.");
        trackerPosition = builder.stringValue("trackerPosition", TrackerPosition.ABOVE_HOTBAR.id, 16,
                "Where the tracker sits: above_hotbar, top_left or top_right.");
        untrackWhenDone = builder.booleanValue("untrackWhenDone", true,
                "Stop tracking an advancement a few seconds after it is done.");
        clickForDetails = builder.booleanValue("clickForDetails", true,
                "Click an advancement in the advancements screen to see what is done and what is missing.");
        sounds = builder.booleanValue("sounds", true,
                "Play a sound when a tracked advancement makes progress or gets done.");
        config = builder.build();
        AdvancementStats.start();
    }

    public static boolean isModEnabled() {
        return Mycel.isEnabled(ID) && enabled != null && enabled.get();
    }

    /** Where the tracker is drawn. Stored as a string, see {@link #trackerPosition}. */
    public enum TrackerPosition {
        ABOVE_HOTBAR("above_hotbar"),
        TOP_LEFT("top_left"),
        TOP_RIGHT("top_right");

        public final String id;

        TrackerPosition(String id) {
            this.id = id;
        }

        public static TrackerPosition current() {
            String value = trackerPosition == null ? "" : trackerPosition.get().trim();
            for (TrackerPosition p : values()) {
                if (p.id.equalsIgnoreCase(value) || p.name().equalsIgnoreCase(value)) {
                    return p;
                }
            }
            return ABOVE_HOTBAR;
        }
    }
}
