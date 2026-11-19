package my.celium.org.advancements.neoforge;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.client.ClientHooks;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** NeoForge entrypoint. Everything else is client side, in {@link NeoForgeClient}. */
@Mod(AdvancementsMod.ID)
public final class AdvancementsNeoForge {
    public AdvancementsNeoForge(IEventBus modBus, @SuppressWarnings("unused") ModContainer container) {
        if (!net.neoforged.fml.ModList.get().isLoaded("mycel")) {
            my.celium.org.advancements.MissingMycel.logOnce();
            if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                        (net.neoforged.neoforge.client.event.ClientTickEvent.Post event) ->
                                my.celium.org.advancements.MissingMycel.tick(net.minecraft.client.Minecraft.getInstance()));
            }
            return;
        }
        my.celium.org.advancements.ModGate.open();
        AdvancementsMod.init();
        if (FMLEnvironment.getDist().isClient()) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    () -> (ModContainer ignored, Screen parent) -> ClientHooks.createConfigScreen(parent, AdvancementsMod.ID));
            NeoForgeClient.register(modBus);
        }
    }
}
