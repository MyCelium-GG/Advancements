package my.celium.org.advancements.client;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

/**
 * The checklist of one advancement, built from what the server sends every client: the
 * requirement groups (criterion names) and which criteria are done.
 *
 * <p>Vanilla names criteria after what they ask for ({@code minecraft:plains} for Adventuring
 * Time, {@code apple} for A Balanced Diet, {@code minecraft:zombie} for Monsters Hunted), so a name
 * is looked up as a biome, mob, effect, item or block, in that order, and otherwise shown tidied up.
 */
public final class Checklist {
    /** How the entries combine. */
    public enum Mode {
        /** Every entry is needed. */
        ALL,
        /** Any one entry completes the advancement. */
        ANY,
        /** A single goal: nothing to list. */
        SINGLE
    }

    /** One line of the checklist: a requirement group, or one option of a single "any of" group. */
    public record Entry(String criterion, Component name, ItemStack icon, boolean done, @Nullable Instant obtained) {
        String sortName() {
            return name.getString().toLowerCase(Locale.ROOT);
        }
    }

    private final AdvancementHolder holder;
    private final List<Entry> entries;
    private final Mode mode;
    private final int doneCount;
    private final int total;
    private final boolean complete;

    private Checklist(AdvancementHolder holder, List<Entry> entries, Mode mode, int doneCount, int total, boolean complete) {
        this.holder = holder;
        this.entries = entries;
        this.mode = mode;
        this.doneCount = doneCount;
        this.total = total;
        this.complete = complete;
    }

    public static Checklist of(AdvancementHolder holder, @Nullable AdvancementProgress progress) {
        AdvancementRequirements requirements = holder.value().requirements();
        List<List<String>> groups = requirements.requirements();
        ItemStack fallback = holder.value().display().map(Display::icon).orElse(new ItemStack(Items.BOOK));
        List<Entry> entries = new ArrayList<>();
        Mode mode;
        if (groups.size() > 1) {
            mode = Mode.ALL;
            for (List<String> group : groups) {
                String shown = group.get(0);
                Instant obtained = null;
                for (String criterion : group) {
                    Instant at = obtained(progress, criterion);
                    if (at != null && (obtained == null || at.isBefore(obtained))) {
                        obtained = at;
                        shown = criterion;
                    }
                }
                if (obtained == null && progress != null && progress.isDone()) {
                    obtained = Instant.EPOCH;
                }
                entries.add(entry(shown, fallback, obtained));
            }
        } else if (groups.size() == 1 && groups.get(0).size() > 1) {
            mode = Mode.ANY;
            for (String criterion : groups.get(0)) {
                entries.add(entry(criterion, fallback, obtained(progress, criterion)));
            }
        } else {
            mode = Mode.SINGLE;
        }
        entries.sort(Comparator.comparing(Entry::sortName));
        boolean complete = progress != null && progress.isDone();
        int done = (int) entries.stream().filter(Entry::done).count();
        int total = entries.size();
        if (mode != Mode.ALL) {
            done = complete ? 1 : 0;
            total = 1;
        }
        return new Checklist(holder, List.copyOf(entries), mode, done, total, complete);
    }

    private record Look(Component name, ItemStack icon) {
    }

    /** Names and icons per criterion: looking them up every frame for the HUD is wasteful. */
    private static final java.util.Map<String, Look> LOOKS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Entry entry(String criterion, ItemStack fallback, @Nullable Instant obtained) {
        Look look = LOOKS.computeIfAbsent(criterion + "|" + fallback.getItem(),
                k -> new Look(name(criterion), icon(criterion, fallback)));
        return new Entry(criterion, look.name(), look.icon(), obtained != null,
                obtained == null || Instant.EPOCH.equals(obtained) ? null : obtained);
    }

    /** Forgets cached names, for example after the language changed. */
    public static void clearCache() {
        LOOKS.clear();
    }

    private static @Nullable Instant obtained(@Nullable AdvancementProgress progress, String criterion) {
        if (progress == null) {
            return null;
        }
        CriterionProgress c = progress.getCriterion(criterion);
        return c != null && c.isDone() ? (c.getObtained() != null ? c.getObtained() : Instant.EPOCH) : null;
    }

    public AdvancementHolder holder() {
        return holder;
    }

    public List<Entry> entries() {
        return entries;
    }

    public Mode mode() {
        return mode;
    }

    public int doneCount() {
        return doneCount;
    }

    public int total() {
        return total;
    }

    public boolean complete() {
        return complete;
    }

    public float fraction() {
        if (complete) {
            return 1f;
        }
        return total == 0 ? 0f : Math.min(1f, doneCount / (float) total);
    }

    /** Entries still missing, in name order. */
    public List<Entry> missing() {
        return entries.stream().filter(e -> !e.done()).toList();
    }

    // ------------------------------------------------------------------ names and icons

    private static final String[] TRANSLATION_PREFIXES = {"biome", "entity", "effect"};

    /** A readable name for a criterion. */
    public static Component name(String criterion) {
        String trim = trimTemplate(criterion);
        if (trim != null) {
            Identifier template = Identifier.tryParse(trim);
            String path = template == null ? trim : template.getPath();
            return Component.literal(tidy(path.replace("_smithing_template", "")));
        }
        Identifier id = parse(criterion);
        if (id != null) {
            Language language = Language.getInstance();
            String dotted = id.getNamespace() + "." + id.getPath().replace('/', '.');
            for (String prefix : TRANSLATION_PREFIXES) {
                String key = prefix + "." + dotted;
                if (language.has(key)) {
                    return Component.translatable(key);
                }
            }
            Item item = item(id);
            if (item != null) {
                return new ItemStack(item).getHoverName();
            }
            Block block = block(id);
            if (block != null) {
                return block.getName();
            }
        }
        return Component.literal(tidy(criterion));
    }

    /** An icon for a criterion: the item, the mob's spawn egg, or {@code fallback}. */
    public static ItemStack icon(String criterion, ItemStack fallback) {
        String trim = trimTemplate(criterion);
        Identifier id = parse(trim != null ? trim : criterion);
        if (id != null) {
            if (Language.getInstance().has("biome." + id.getNamespace() + "." + id.getPath().replace('/', '.'))) {
                return new ItemStack(Items.FILLED_MAP);
            }
            EntityType<?> type = entity(id);
            if (type != null) {
                Optional<Holder<Item>> egg = SpawnEggItem.byId(type);
                if (egg.isPresent()) {
                    return new ItemStack(egg.get().value());
                }
            }
            if (Language.getInstance().has("effect." + id.getNamespace() + "." + id.getPath())) {
                return new ItemStack(Items.POTION);
            }
            Item item = item(id);
            if (item != null) {
                return new ItemStack(item);
            }
            Block block = block(id);
            if (block != null && block.asItem() != Items.AIR) {
                return new ItemStack(block.asItem());
            }
        }
        return fallback.copy();
    }

    /**
     * Armor trim criteria are named {@code armor_trimmed_<template item>_smithing_trim}; returns the
     * template item id, or null for other criteria.
     */
    static @Nullable String trimTemplate(String criterion) {
        String prefix = "armor_trimmed_";
        String suffix = "_smithing_trim";
        if (criterion.startsWith(prefix) && criterion.endsWith(suffix)
                && criterion.length() > prefix.length() + suffix.length()) {
            return criterion.substring(prefix.length(), criterion.length() - suffix.length());
        }
        return null;
    }

    private static @Nullable Identifier parse(String criterion) {
        String s = criterion.trim().toLowerCase(Locale.ROOT);
        return s.isEmpty() ? null : Identifier.tryParse(s);
    }

    private static @Nullable Item item(Identifier id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == Items.AIR ? null : item;
    }

    private static @Nullable Block block(Identifier id) {
        return BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.getValue(id) : null;
    }

    private static @Nullable EntityType<?> entity(Identifier id) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.getValue(id) : null;
    }

    /** {@code minecraft:all_black} becomes {@code All Black}. */
    static String tidy(String criterion) {
        String path = criterion.contains(":") ? criterion.substring(criterion.indexOf(':') + 1) : criterion;
        path = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_\\-.\\s]+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.isEmpty() ? criterion : out.toString();
    }

    // ------------------------------------------------------------------ display helpers

    public static @Nullable DisplayInfo display(AdvancementHolder holder) {
        return holder.value().display().orElse(null);
    }

    public static Component title(AdvancementHolder holder) {
        DisplayInfo d = display(holder);
        return d != null ? Display.title(d) : Component.literal(tidy(holder.id().getPath()));
    }

    public static Component description(AdvancementHolder holder) {
        DisplayInfo d = display(holder);
        return d != null ? Display.description(d) : Component.empty();
    }

    /** How to get a vanilla advancement, from this mod's lang files; null when there is none. */
    public static @Nullable Component explanation(AdvancementHolder holder) {
        Identifier id = holder.id();
        String key = "advancementsmod.explain." + id.getPath().replace('/', '.');
        if (!"minecraft".equals(id.getNamespace()) || !Language.getInstance().has(key)) {
            return null;
        }
        return Component.translatable(key);
    }

    public static ItemStack icon(AdvancementHolder holder) {
        DisplayInfo d = display(holder);
        return d != null ? Display.icon(d) : new ItemStack(Items.BOOK);
    }

    private static final int TASK_COLOR = 0xFF4ADE80;
    private static final int CHALLENGE_COLOR = 0xFFC084FC;

    /** Frame color: green for tasks and goals, purple for challenges, like vanilla's chat. */
    public static int typeColor(AdvancementHolder holder) {
        DisplayInfo d = display(holder);
        if (d == null) {
            return 0xFFF4F4F5;
        }
        return "challenge".equals(Display.type(d).getSerializedName()) ? CHALLENGE_COLOR : TASK_COLOR;
    }

    public static boolean hidden(AdvancementHolder holder) {
        DisplayInfo d = display(holder);
        return d == null || Display.hidden(d);
    }
}
