package my.celium.org.advancements.neoforge;

import my.celium.org.advancements.client.AdvancementsClient;
import my.celium.org.advancements.client.TrackerHud;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge client side: the keys, the tick and the tracker on the HUD. Only loaded on clients. */
final class NeoForgeClient {
    private NeoForgeClient() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(AdvancementsClient.LIST_KEY);
            event.register(AdvancementsClient.TRACKED_KEY);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AdvancementsClient.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> TrackerHud.render(event.getGuiGraphics()));
    }
}
