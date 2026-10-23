package org.example.pz.quarryindustrial.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.block.generator.DieselGeneratorBlockEntity;
import org.example.pz.quarryindustrial.block.generator.GeneratorBlock;
import org.example.pz.quarryindustrial.block.generator.SteamTurbineBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreStatus;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlock;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackLayout;
import org.example.pz.quarryindustrial.block.quarry.QuarryArea;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryStatus;
import org.example.pz.quarryindustrial.drone.DroneEntity;
import org.example.pz.quarryindustrial.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Todos los sonidos en loop del mod: el motor y la broca de la quarry (sigue al carro), los generadores, el
 * motor diésel de la cigüeña y de la plataforma, y las hélices de cada dron. Más la alarma cuando una quarry
 * que estaba minando se para (sin energía, sin broca o con la salida llena).
 */
public final class MachineSounds {
    private static final double HEARING_DISTANCE = 32;
    private static final Map<Object, LoopSound> PLAYING = new WeakHashMap<>();
    private static final Map<QuarryBlockEntity, QuarryStatus> LAST_STATUS = new WeakHashMap<>();

    private MachineSounds() {
    }

    // ── Quarry ──

    public static void tickQuarry(QuarryBlockEntity quarry) {
        QuarryStatus previous = LAST_STATUS.put(quarry, quarry.status());
        if (previous != null && previous.drilling() && stalled(quarry.status())) {
            play(ModSounds.ALARM, Vec3.atCenterOf(quarry.getBlockPos()), 1.0f);
        }
        if (!quarry.status().drilling() || quarry.visualHeadCount() == 0) {
            return;
        }
        // Cuanto más alto el tier, más grave: motores más grandes
        float pitch = switch (quarry.tier()) {
            case LV -> 1.15f;
            case MV -> 1.0f;
            case HV -> 0.85f;
            case NUCLEAR -> 0.7f;
        };
        start(quarry, ModSounds.DRILL, 0.5f, pitch, () -> {
            QuarryArea area = quarry.area();
            return new Vec3(quarry.visualX(0, 1), area != null ? area.topY() + 1 : quarry.getBlockPos().getY(),
                    quarry.visualZ(0, 1));
        }, () -> !quarry.isRemoved() && quarry.status().drilling());
    }

    private static boolean stalled(QuarryStatus status) {
        return status == QuarryStatus.NO_POWER || status == QuarryStatus.NO_BIT || status == QuarryStatus.OUTPUT_FULL;
    }

    // ── Generadores, cigüeña, plataforma ──

    public static void tickMachine(BlockEntity machine) {
        BlockPos pos = machine.getBlockPos();
        if (machine instanceof DieselGeneratorBlockEntity generator) {
            if (generator.getBlockState().getValue(GeneratorBlock.LIT)) {
                start(generator, ModSounds.DIESEL, 0.6f, 1.1f, () -> Vec3.atCenterOf(pos),
                        () -> !generator.isRemoved() && generator.getBlockState().getValue(GeneratorBlock.LIT));
            }
        } else if (machine instanceof SteamTurbineBlockEntity turbine) {
            if (turbine.getBlockState().getValue(GeneratorBlock.LIT)) {
                start(turbine, ModSounds.TURBINE, 0.5f, 1.0f, () -> Vec3.atCenterOf(pos),
                        () -> !turbine.isRemoved() && turbine.getBlockState().getValue(GeneratorBlock.LIT));
            }
        } else if (machine instanceof PumpjackBlockEntity pumpjack) {
            if (pumpjack.onDiesel() && pumpjack.status().running()) {
                BlockPos motor = PumpjackLayout.toWorld(pos, pumpjack.getBlockState().getValue(PumpjackBlock.FACING), 0, 1,
                        PumpjackLayout.LENGTH - 1);
                start(pumpjack, ModSounds.DIESEL, 0.6f, 0.8f, () -> Vec3.atCenterOf(motor),
                        () -> !pumpjack.isRemoved() && pumpjack.onDiesel() && pumpjack.status().running());
            }
        } else if (machine instanceof OffshoreRigBlockEntity rig) {
            if (rig.status() == OffshoreStatus.DRILLING) {
                start(rig, ModSounds.DRILL, 0.8f, 0.6f, () -> Vec3.atCenterOf(pos).add(0, 1, 0),
                        () -> !rig.isRemoved() && rig.status() == OffshoreStatus.DRILLING);
            } else if (rig.status() == OffshoreStatus.PRODUCING) {
                start(rig, ModSounds.DIESEL, 0.7f, 0.7f, () -> Vec3.atCenterOf(pos).add(-2, 2, 2),
                        () -> !rig.isRemoved() && rig.status() == OffshoreStatus.PRODUCING);
            }
        }
    }

    public static void pumpjackStroke(BlockEntity pumpjack) {
        play(ModSounds.PUMPJACK_STROKE, Vec3.atCenterOf(pumpjack.getBlockPos()), 0.9f);
    }

    // ── Drones ──

    /** Cada tick de cliente: hélices para los drones que estén cerca y todavía no suenen. */
    public static void tickDrones(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof DroneEntity drone) {
                start(drone, ModSounds.DRONE, 0.45f, 1.0f, drone::position, () -> !drone.isRemoved());
            }
        }
    }

    // ── Comunes ──

    private static void start(Object owner, SoundEvent sound, float volume, float pitch, Supplier<Vec3> position,
                              BooleanSupplier active) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        LoopSound current = PLAYING.get(owner);
        if (current != null && !current.isStopped() && current.sound == sound) {
            return;
        }
        if (minecraft.player.position().distanceToSqr(position.get()) > HEARING_DISTANCE * HEARING_DISTANCE) {
            return;
        }
        LoopSound loop = new LoopSound(sound, volume, pitch, position, active);
        PLAYING.put(owner, loop);
        minecraft.getSoundManager().play(loop);
    }

    private static void play(SoundEvent sound, Vec3 at, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.position().distanceToSqr(at) > HEARING_DISTANCE * HEARING_DISTANCE) {
            return;
        }
        minecraft.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.BLOCKS, 1.0f, pitch,
                RandomSource.create(), at.x, at.y, at.z));
    }

    /** Loop que sigue una posición y se corta solo cuando la máquina deja de andar. */
    private static final class LoopSound extends AbstractTickableSoundInstance {
        private final SoundEvent sound;
        private final Supplier<Vec3> position;
        private final BooleanSupplier active;

        LoopSound(SoundEvent sound, float volume, float pitch, Supplier<Vec3> position, @Nullable BooleanSupplier active) {
            super(sound, SoundSource.BLOCKS, RandomSource.create());
            this.sound = sound;
            this.position = position;
            this.active = active;
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.pitch = pitch;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            follow();
        }

        private void follow() {
            Vec3 at = position.get();
            x = at.x;
            y = at.y;
            z = at.z;
        }

        @Override
        public void tick() {
            if (active != null && !active.getAsBoolean()) {
                stop();
                return;
            }
            follow();
        }
    }
}
