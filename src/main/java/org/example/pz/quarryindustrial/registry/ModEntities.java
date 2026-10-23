package org.example.pz.quarryindustrial.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.drone.DroneEntity;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> DRONE_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
            Quarryindustrial.id("drone"));
    /** Rastreado de lejos y con la posición cada tick: se ve volar suave aunque vaya rápido. */
    public static final EntityType<DroneEntity> DRONE = Registry.register(BuiltInRegistries.ENTITY_TYPE, DRONE_KEY,
            EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                    .sized(0.9f, 0.45f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build(DRONE_KEY));

    private ModEntities() {
    }

    public static void init() {
        // Los campos estáticos se registran al cargar la clase
    }
}
