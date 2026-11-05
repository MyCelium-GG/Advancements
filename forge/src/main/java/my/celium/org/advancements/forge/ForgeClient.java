package my.celium.org.advancements.forge;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.advancements.client.AdvancementsClient;
import my.celium.org.advancements.client.TrackerHud;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.event.TickEvent;

/** Forge client side: the tick and the tracker on the HUD. Only loaded on clients. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> AdvancementsClient.tick(Minecraft.getInstance()));
        AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw().add(
                Identifier.fromNamespaceAndPath(AdvancementsMod.ID, "tracker"), (gfx, tracker) -> TrackerHud.render(gfx)));
    }
}
