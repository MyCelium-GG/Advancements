package my.celium.org.advancements.client;

import java.util.Optional;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** DisplayInfo access for Minecraft 26.3, where it became a record. */
final class Display {
    private Display() {
    }

    static ItemStack icon(DisplayInfo d) {
        return d.icon().create();
    }

    static Component title(DisplayInfo d) {
        return d.title();
    }

    static Component description(DisplayInfo d) {
        return d.description();
    }

    static AdvancementType type(DisplayInfo d) {
        return d.type();
    }

    static boolean hidden(DisplayInfo d) {
        return d.hidden();
    }

    static Optional<Identifier> background(DisplayInfo d) {
        return d.background().map(ClientAsset.ResourceTexture::texturePath);
    }
}
