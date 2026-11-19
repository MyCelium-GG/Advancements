package my.celium.org.advancements.fabric;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.advancements.client.AdvancementsClient;
import my.celium.org.advancements.client.TrackerHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/** Fabric client entrypoint: the tick and the tracker on the HUD. */
public final class AdvancementsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("mycel")) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                    my.celium.org.advancements.MissingMycel::tick);
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(AdvancementsClient::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(AdvancementsMod.ID, "tracker"),
                (gfx, tracker) -> TrackerHud.render(gfx));
    }
}
