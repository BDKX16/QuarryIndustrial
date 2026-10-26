package org.example.pz.quarryindustrial.client.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.item.BitTier;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Las páginas de información de JEI y REI: un texto corto por máquina (cómo se arma, qué necesita), con la
 * clave {@code info.quarryindustrial.<id>} en los archivos de idioma.
 */
public final class MachineGuide {
    public record Entry(Item item, Component text) {
    }

    private MachineGuide() {
    }

    public static List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        add(entries, ModBlocks.QUARRY_LV.asItem(), "quarry");
        add(entries, ModBlocks.QUARRY_MV.asItem(), "quarry");
        add(entries, ModBlocks.QUARRY_HV.asItem(), "quarry");
        add(entries, ModBlocks.QUARRY_CORE.asItem(), "quarry_core");
        add(entries, ModBlocks.QUARRY_CASING.asItem(), "quarry_core");
        add(entries, ModBlocks.ENERGY_PORT.asItem(), "energy_port");
        add(entries, ModBlocks.PUMPJACK.asItem(), "pumpjack");
        add(entries, ModBlocks.OFFSHORE_RIG.asItem(), "offshore_rig");
        add(entries, ModBlocks.DIESEL_GENERATOR.asItem(), "diesel_generator");
        add(entries, ModBlocks.STEAM_TURBINE.asItem(), "steam_turbine");
        add(entries, ModBlocks.DRONE_PAD.asItem(), "drone_pad");
        add(entries, ModBlocks.DRONE_CONSOLE.asItem(), "drone_console");
        add(entries, ModBlocks.RADAR.asItem(), "drone_radar");
        add(entries, ModItems.DRONE_LINKER, "drone_linker");
        add(entries, ModItems.PAD_CHUNK_LOADER, "pad_chunk_loader");
        add(entries, ModItems.SURVEY_RANGE_UPGRADE, "survey_range_upgrade");
        add(entries, ModItems.MANUAL, "manual");
        for (BitTier tier : BitTier.values()) {
            add(entries, ModItems.bit(tier), "drill_bit");
        }
        for (DroneKind kind : DroneKind.values()) {
            add(entries, ModItems.drone(kind), kind.id() + "_drone");
        }
        return entries;
    }

    private static void add(List<Entry> entries, Item item, String key) {
        entries.add(new Entry(item, Component.translatable("info.quarryindustrial." + key)));
    }
}
