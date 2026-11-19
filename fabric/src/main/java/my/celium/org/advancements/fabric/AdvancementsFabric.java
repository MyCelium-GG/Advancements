package my.celium.org.advancements.fabric;

import my.celium.org.advancements.AdvancementsMod;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint. Everything else is client side. */
public final class AdvancementsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("mycel")) {
            my.celium.org.advancements.MissingMycel.logOnce();
            return;
        }
        my.celium.org.advancements.ModGate.open();
        AdvancementsMod.init();
    }
}
