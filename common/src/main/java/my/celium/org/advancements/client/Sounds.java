package my.celium.org.advancements.client;

import my.celium.org.advancements.AdvancementsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** The overlay's sounds, played only for you (like menu sounds). Off with the "sounds" setting. */
final class Sounds {
    private Sounds() {
    }

    /** A part got done: a soft ding that climbs in pitch the closer the advancement is to done. */
    static void partDone(float fraction) {
        float pitch = 0.9f + 0.7f * Math.max(0f, Math.min(1f, fraction));
        play(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, 0.35f);
        play(SoundEvents.AMETHYST_BLOCK_CHIME, pitch, 0.6f);
    }

    /**
     * The tracked advancement got finished. Challenges already get vanilla's fanfare from their
     * toast, so only tasks and goals get the level-up sound.
     */
    static void advancementDone(String frameType) {
        if (!"challenge".equals(frameType)) {
            play(SoundEvents.PLAYER_LEVELUP, 1.0f, 0.6f);
        }
    }

    /** The details window opened. */
    static void pageTurn() {
        play(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        if (AdvancementsMod.sounds == null || !AdvancementsMod.sounds.get()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
