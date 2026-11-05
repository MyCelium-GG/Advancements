package my.celium.org.advancements.fabric;

import my.celium.org.advancements.AdvancementsMod;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint. Everything else is client side. */
public final class AdvancementsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AdvancementsMod.init();
    }
}
