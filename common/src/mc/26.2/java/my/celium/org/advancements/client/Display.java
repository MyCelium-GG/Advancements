package my.celium.org.advancements.client;

import java.util.Optional;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** DisplayInfo access for Minecraft 26.1 and 26.2, where it is a class with getters. */
final class Display {
    private Display() {
    }

    static ItemStack icon(DisplayInfo d) {
        return d.getIcon().create();
    }

    static Component title(DisplayInfo d) {
        return d.getTitle();
    }

    static Component description(DisplayInfo d) {
        return d.getDescription();
    }

    static AdvancementType type(DisplayInfo d) {
        return d.getType();
    }

    static boolean hidden(DisplayInfo d) {
        return d.isHidden();
    }

    static Optional<Identifier> background(DisplayInfo d) {
        return d.getBackground().map(ClientAsset.ResourceTexture::texturePath);
    }
}
