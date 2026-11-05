package my.celium.org.advancements.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ChecklistTest {
    @Test
    void tidiesCriterionNames() {
        assertEquals("All Black", Checklist.tidy("minecraft:all_black"));
        assertEquals("Temperate", Checklist.tidy("minecraft:temperate"));
        assertEquals("Loot Bastion Bridge", Checklist.tidy("loot_bastion_bridge"));
        assertEquals("Root", Checklist.tidy("story/root"));
    }

    @Test
    void findsArmorTrimTemplates() {
        assertEquals("minecraft:rib_armor_trim_smithing_template",
                Checklist.trimTemplate("armor_trimmed_minecraft:rib_armor_trim_smithing_template_smithing_trim"));
        assertNull(Checklist.trimTemplate("minecraft:plains"));
    }
}
