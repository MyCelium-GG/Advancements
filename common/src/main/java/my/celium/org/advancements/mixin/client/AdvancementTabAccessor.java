package my.celium.org.advancements.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;

/** The tab's widgets and scroll, to find the advancement under the mouse on every Minecraft line. */
@Mixin(AdvancementTab.class)
public interface AdvancementTabAccessor {
    @Accessor("widgets")
    Map<AdvancementHolder, AdvancementWidget> advancements$widgets();

    @Accessor("scrollX")
    double advancements$scrollX();

    @Accessor("scrollY")
    double advancements$scrollY();
}
