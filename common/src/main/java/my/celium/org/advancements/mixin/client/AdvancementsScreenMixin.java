package my.celium.org.advancements.mixin.client;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.InputConstants;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.advancements.client.AdvancementsScreenHooks;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Vanilla's advancements screen: a click on an advancement opens a small window explaining it.
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {
    @Shadow
    private @Nullable AdvancementTab selectedTab;

    @Shadow
    private boolean isScrolling;

    protected AdvancementsScreenMixin(Component title) {
        super(title);
    }

    /** A left click on an advancement opens its window right away; everything else is vanilla's. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void advancements$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !AdvancementsMod.isModEnabled()
                || !AdvancementsMod.clickForDetails.get() || selectedTab == null) {
            return;
        }
        AdvancementHolder hit = advancements$hit(event.x(), event.y());
        if (hit != null) {
            isScrolling = false;
            AdvancementsScreenHooks.openDetails(hit, this);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void advancements$hint(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (AdvancementsMod.isModEnabled() && AdvancementsMod.clickForDetails.get()) {
            AdvancementsScreenHooks.drawHint(gfx, font, width, height);
        }
    }

    @Unique
    private @Nullable AdvancementHolder advancements$hit(double mouseX, double mouseY) {
        int left = (width - 252) / 2;
        int top = (height - 140) / 2;
        int relX = (int) mouseX - left - 9;
        int relY = (int) mouseY - top - 18;
        if (relX <= 0 || relX >= 234 || relY <= 0 || relY >= 113) {
            return null;
        }
        AdvancementTabAccessor tab = (AdvancementTabAccessor) selectedTab;
        int scrollX = Mth.floor(tab.advancements$scrollX());
        int scrollY = Mth.floor(tab.advancements$scrollY());
        for (Map.Entry<AdvancementHolder, AdvancementWidget> e : tab.advancements$widgets().entrySet()) {
            if (e.getValue().isMouseOver(scrollX, scrollY, relX, relY)) {
                return e.getKey();
            }
        }
        return null;
    }
}
