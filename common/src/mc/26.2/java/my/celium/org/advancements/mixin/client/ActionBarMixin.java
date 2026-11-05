package my.celium.org.advancements.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import my.celium.org.advancements.client.Tracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;

/** Notices action bar messages, so the tracker above the hotbar makes room for them. */
@Mixin(Hud.class)
public abstract class ActionBarMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void advancements$actionBar(Component message, boolean animate, CallbackInfo ci) {
        Tracker.actionBarShown();
    }
}
