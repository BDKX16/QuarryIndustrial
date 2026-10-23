package org.example.pz.quarryindustrial.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.example.pz.quarryindustrial.Quarryindustrial;

/** Sonidos propios (sintetizados con {@code tools/gen_sounds.py}). */
public final class ModSounds {
    public static final SoundEvent DRILL = register("drill");
    public static final SoundEvent DIESEL = register("diesel");
    public static final SoundEvent DRONE = register("drone");
    public static final SoundEvent TURBINE = register("turbine");
    public static final SoundEvent PUMPJACK_STROKE = register("pumpjack_stroke");
    public static final SoundEvent ALARM = register("alarm");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = Quarryindustrial.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
