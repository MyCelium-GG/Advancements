package my.celium.org.advancements.forge;

import my.celium.org.advancements.AdvancementsMod;
import my.celium.org.client.ClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge entrypoint. Everything else is client side, in {@link ForgeClient}. */
@Mod(AdvancementsMod.ID)
public final class AdvancementsForge {
    public AdvancementsForge(@SuppressWarnings("unused") FMLJavaModLoadingContext context) {
        AdvancementsMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (Minecraft minecraft, Screen parent) -> ClientHooks.createConfigScreen(parent, AdvancementsMod.ID)));
            ForgeClient.register();
        }
    }
}
