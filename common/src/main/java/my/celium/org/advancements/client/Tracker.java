package my.celium.org.advancements.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import my.celium.org.advancements.AdvancementStats;
import my.celium.org.advancements.AdvancementsMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import my.celium.org.advancements.mixin.client.ClientAdvancementsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The tracked advancement, one per world or server, saved in
 * {@code config/advancements-tracked.json}. Also notices when one of its entries gets done, so the
 * HUD can show it for a moment.
 */
public final class Tracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FLASH_TICKS = 70;
    private static final int DONE_TICKS = 100;
    /** How long a just-finished advancement stays on screen. */
    public static final long DONE_MS = 6000;
    private static final int ITEM_NAME_TICKS = 50;

    private static final Map<String, String> SAVED = new LinkedHashMap<>();
    private static boolean loaded;

    private static @Nullable Identifier tracked;
    private static @Nullable Checklist trackedList;
    private static String language = "";
    private static String worldKey = "";
    private static Set<String> lastDone = Set.of();
    private static @Nullable Identifier lastDoneFor;
    private static @Nullable Component flash;
    private static @Nullable ItemStack flashIcon;
    private static int actionBarTicks;
    private static int flashTicks;
    private static long flashStart;
    private static long completeStart;
    private static long trackStart;
    private static boolean wasComplete;
    private static int doneTicks;
    private static ItemStack lastHeld = ItemStack.EMPTY;
    private static int itemNameTicks;

    private Tracker() {
    }

    // ------------------------------------------------------------------ queries

    public static @Nullable ClientAdvancements advancements() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getAdvancements();
    }

    public static @Nullable AdvancementHolder holder(@Nullable Identifier id) {
        ClientAdvancements adv = advancements();
        return adv == null || id == null ? null : adv.get(id);
    }

    public static @Nullable AdvancementProgress progress(AdvancementHolder holder) {
        ClientAdvancements adv = advancements();
        return adv == null ? null : progressMap(adv).get(holder);
    }

    static Map<AdvancementHolder, AdvancementProgress> progressMap(ClientAdvancements adv) {
        return ((ClientAdvancementsAccessor) adv).advancements$progress();
    }

    public static net.minecraft.advancements.@Nullable AdvancementNode node(AdvancementHolder holder) {
        ClientAdvancements adv = advancements();
        return adv == null ? null : tree(adv).get(holder);
    }

    static AdvancementTree tree(ClientAdvancements adv) {
        return ((ClientAdvancementsAccessor) adv).advancements$tree();
    }

    public static Checklist checklist(AdvancementHolder holder) {
        return Checklist.of(holder, progress(holder));
    }

    public static boolean isTracked(AdvancementHolder holder) {
        return holder.id().equals(tracked);
    }

    /** The tracked advancement, if this world has one and the server sent it. */
    public static @Nullable AdvancementHolder trackedHolder() {
        return holder(tracked);
    }

    /** The tracked advancement's checklist, worked out once per tick for the HUD. */
    public static @Nullable Checklist trackedChecklist() {
        AdvancementHolder holder = trackedHolder();
        if (holder == null) {
            return null;
        }
        Checklist list = trackedList;
        return list != null && list.holder().equals(holder) ? list : checklist(holder);
    }

    public static @Nullable Component flash() {
        return flashTicks > 0 ? flash : null;
    }

    /** Ticks left of the flash, for fading. */
    public static int flashTicks() {
        return flashTicks;
    }

    public static @Nullable ItemStack flashIcon() {
        return flashTicks > 0 ? flashIcon : null;
    }

    /** When the latest part got done (real time), 0 when none. */
    public static long flashStart() {
        return flashTicks > 0 ? flashStart : 0;
    }

    /** When the tracked advancement got finished while tracked (real time), 0 when not. */
    public static long completeStart() {
        return completeStart;
    }

    /** When tracking started (real time), for the entry animation. */
    public static long trackStart() {
        return trackStart;
    }

    /** Vanilla just showed an action bar message (it stays about three seconds). */
    public static void actionBarShown() {
        actionBarTicks = 70;
    }

    public static boolean actionBarShowing() {
        return actionBarTicks > 0;
    }

    /** Whether vanilla is showing the held item's name above the hotbar right now. */
    public static boolean itemNameShowing() {
        return itemNameTicks > 0;
    }

    // ------------------------------------------------------------------ changes

    public static void track(AdvancementHolder holder) {
        tracked = holder.id();
        trackStart = net.minecraft.util.Util.getMillis();
        completeStart = 0;
        wasComplete = checklist(holder).complete();
        doneTicks = 0;
        flashTicks = 0;
        rememberDone(holder);
        save();
        AdvancementStats.count("track");
        AdvancementStats.count("tracked", holder.id().toString());
    }

    public static void untrack() {
        tracked = null;
        completeStart = 0;
        flashTicks = 0;
        doneTicks = 0;
        save();
    }

    public static void toggle(AdvancementHolder holder) {
        if (isTracked(holder)) {
            untrack();
        } else {
            track(holder);
        }
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            worldKey = "";
            tracked = null;
            lastDone = Set.of();
            lastDoneFor = null;
            return;
        }
        String key = worldKey(mc);
        if (!key.equals(worldKey)) {
            worldKey = key;
            load();
            String saved = SAVED.get(key);
            tracked = saved == null ? null : Identifier.tryParse(saved);
            lastDone = Set.of();
            lastDoneFor = null;
            flashTicks = 0;
            doneTicks = 0;
        }
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (actionBarTicks > 0) {
            actionBarTicks--;
        }
        tickItemName(mc);

        String lang = mc.options.languageCode;
        if (!lang.equals(language)) {
            language = lang;
            Checklist.clearCache();
        }
        AdvancementHolder holder = trackedHolder();
        if (holder == null) {
            trackedList = null;
            return;
        }
        Checklist list = checklist(holder);
        trackedList = list;
        Set<String> done = doneSet(list);
        if (holder.id().equals(lastDoneFor)) {
            for (Checklist.Entry e : list.entries()) {
                if (e.done() && !lastDone.contains(e.criterion())) {
                    flash = e.name();
                    flashIcon = e.icon();
                    flashTicks = FLASH_TICKS;
                    flashStart = net.minecraft.util.Util.getMillis();
                }
            }
        }
        boolean sameAdvancement = holder.id().equals(lastDoneFor);
        lastDone = done;
        lastDoneFor = holder.id();
        if (list.complete() && !wasComplete && sameAdvancement) {
            completeStart = net.minecraft.util.Util.getMillis();
        }
        wasComplete = list.complete();
        if (list.complete() && AdvancementsMod.untrackWhenDone.get()) {
            // Stays for the celebration, then shrinks away (see TrackerHud.EXIT_MS).
            if (completeStart > 0 && net.minecraft.util.Util.getMillis() - completeStart > DONE_MS) {
                untrack();
            } else if (completeStart == 0 && ++doneTicks >= DONE_TICKS) {
                untrack();
            }
        } else {
            doneTicks = 0;
        }
    }

    private static void rememberDone(AdvancementHolder holder) {
        lastDone = doneSet(checklist(holder));
        lastDoneFor = holder.id();
    }

    private static Set<String> doneSet(Checklist list) {
        Set<String> done = new HashSet<>();
        for (Checklist.Entry e : list.entries()) {
            if (e.done()) {
                done.add(e.criterion());
            }
        }
        return done;
    }

    /** Mirrors vanilla: the held item's name shows for a moment after switching items. */
    private static void tickItemName(Minecraft mc) {
        ItemStack held = mc.player.getMainHandItem();
        boolean changed = !held.isEmpty() && (lastHeld.isEmpty() || !held.is(lastHeld.getItem())
                || !held.getHoverName().equals(lastHeld.getHoverName()));
        if (changed) {
            itemNameTicks = ITEM_NAME_TICKS;
        } else if (itemNameTicks > 0) {
            itemNameTicks--;
        }
        if (held.isEmpty()) {
            itemNameTicks = 0;
        }
        lastHeld = held.copy();
    }

    // ------------------------------------------------------------------ storage

    private static String worldKey(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            return "world:" + server.getWorldData().getLevelName();
        }
        ServerData data = mc.getCurrentServer();
        return data != null ? "server:" + data.ip.toLowerCase(java.util.Locale.ROOT) : "server:unknown";
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("advancements-tracked.json");
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>() { }.getType());
            if (map != null) {
                SAVED.putAll(map);
            }
        } catch (IOException | RuntimeException e) {
            // A broken file only loses the tracked advancements.
        }
    }

    private static void save() {
        if (worldKey.isEmpty()) {
            return;
        }
        if (tracked == null) {
            SAVED.remove(worldKey);
        } else {
            SAVED.put(worldKey, tracked.toString());
        }
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(SAVED, writer);
            }
        } catch (IOException e) {
            // Tracking still works for this session.
        }
    }
}
