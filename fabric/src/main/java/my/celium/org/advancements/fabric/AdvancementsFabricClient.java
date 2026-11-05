package my.celium.org.advancements.fabric;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.advancements.client.AdvancementsClient;
import my.celium.org.advancements.client.TrackerHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/** Fabric client entrypoint: the keys, the tick and the tracker on the HUD. */
public final class AdvancementsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(AdvancementsClient.LIST_KEY);
        KeyMappingHelper.registerKeyMapping(AdvancementsClient.TRACKED_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(AdvancementsClient::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(AdvancementsMod.ID, "tracker"),
                (gfx, tracker) -> TrackerHud.render(gfx));
    }
}
