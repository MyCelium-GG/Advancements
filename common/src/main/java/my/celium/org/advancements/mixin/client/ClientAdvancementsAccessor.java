package my.celium.org.advancements.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.multiplayer.ClientAdvancements;

/** The advancement tree and progress, which only 26.3 exposes with public getters. */
@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccessor {
    @Accessor("tree")
    AdvancementTree advancements$tree();

    @Accessor("progress")
    Map<AdvancementHolder, AdvancementProgress> advancements$progress();
}
