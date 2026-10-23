package org.example.pz.quarryindustrial.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.example.pz.quarryindustrial.block.quarry.QuarryArea;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Zumbido grave del motor mientras la quarry mina. El sonido sigue al carro del primer cabezal y se
 * corta solo cuando la quarry deja de minar, se rompe o el jugador se aleja.
 */
public final class QuarrySounds {
    private static final double HEARING_DISTANCE = 32;
    private static final Map<QuarryBlockEntity, MotorSound> PLAYING = new WeakHashMap<>();

    private QuarrySounds() {
    }

    /** Se llama en cada tick de cliente de cada quarry. */
    public static void tick(QuarryBlockEntity quarry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !quarry.status().drilling() || quarry.visualHeadCount() == 0) {
            return;
        }
        MotorSound current = PLAYING.get(quarry);
        if (current != null && !current.isStopped()) {
            return;
        }
        if (minecraft.player.position().distanceToSqr(quarry.visualX(0, 1), quarry.visualY(0, 1),
                quarry.visualZ(0, 1)) > HEARING_DISTANCE * HEARING_DISTANCE) {
            return;
        }
        MotorSound sound = new MotorSound(quarry);
        PLAYING.put(quarry, sound);
        minecraft.getSoundManager().play(sound);
    }

    private static final class MotorSound extends AbstractTickableSoundInstance {
        private final QuarryBlockEntity quarry;

        MotorSound(QuarryBlockEntity quarry) {
            super(SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, RandomSource.create());
            this.quarry = quarry;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.35f;
            // Cuanto más alto el tier, más grave: motores más grandes
            this.pitch = switch (quarry.tier()) {
                case LV -> 0.7f;
                case MV -> 0.6f;
                case HV -> 0.5f;
                case NUCLEAR -> 0.42f;
            };
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            follow();
        }

        private void follow() {
            QuarryArea area = quarry.area();
            x = quarry.visualX(0, 1);
            y = area != null ? area.topY() + 1 : quarry.getBlockPos().getY();
            z = quarry.visualZ(0, 1);
        }

        @Override
        public void tick() {
            if (quarry.isRemoved() || !quarry.status().drilling()) {
                stop();
                return;
            }
            follow();
        }
    }
}
