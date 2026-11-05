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
 * Vanilla's advancements screen: a click on an advancement (not a drag) opens its details, and a
 * button below the window opens the list of all advancements.
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {
    @Shadow
    private @Nullable AdvancementTab selectedTab;

    @Shadow
    private boolean isScrolling;

    @Unique
    private double advancements$pressX = Double.NaN;

    @Unique
    private double advancements$pressY;

    protected AdvancementsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void advancements$addListButton(CallbackInfo ci) {
        if (AdvancementsMod.isModEnabled()) {
            addRenderableWidget(AdvancementsScreenHooks.listButton(this, width, height));
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void advancements$press(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        advancements$pressX = event.button() == 0 ? event.x() : Double.NaN;
        advancements$pressY = event.y();
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void advancements$release(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        double pressX = advancements$pressX;
        advancements$pressX = Double.NaN;
        if (event.button() != 0 || Double.isNaN(pressX) || !AdvancementsMod.isModEnabled()
                || !AdvancementsMod.clickForDetails.get() || selectedTab == null) {
            return;
        }
        if (Math.abs(event.x() - pressX) > 3 || Math.abs(event.y() - advancements$pressY) > 3) {
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
