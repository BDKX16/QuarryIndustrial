package org.example.pz.quarryindustrial.registry;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackPartBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import team.reborn.energy.api.EnergyStorage;

import java.util.Set;

public final class ModBlockEntities {
    /** Un solo tipo para los tres tiers: el tier se lee del bloque al crear el block entity. */
    public static final BlockEntityType<QuarryBlockEntity> QUARRY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("quarry"),
            new BlockEntityType<>(QuarryBlockEntity::new,
                    Set.of(ModBlocks.QUARRY_LV, ModBlocks.QUARRY_MV, ModBlocks.QUARRY_HV, ModBlocks.QUARRY_CORE)));
    public static final BlockEntityType<EnergyPortBlockEntity> ENERGY_PORT = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("energy_port"),
            new BlockEntityType<>(EnergyPortBlockEntity::new, Set.of(ModBlocks.ENERGY_PORT)));
    public static final BlockEntityType<PumpjackBlockEntity> PUMPJACK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("pumpjack"),
            new BlockEntityType<>(PumpjackBlockEntity::new, Set.of(ModBlocks.PUMPJACK)));
    public static final BlockEntityType<PumpjackPartBlockEntity> PUMPJACK_PART = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("pumpjack_part"),
            new BlockEntityType<>(PumpjackPartBlockEntity::new, Set.of(ModBlocks.PUMPJACK_PART)));

    private ModBlockEntities() {
    }

    public static void init() {
        // Publica el buffer de la quarry en la API de Team Reborn Energy: los cables de
        // Ala Industrial (y Tech Reborn) la encuentran con EnergyStorage.SIDED.find(...) y le empujan EU.
        // side puede ser null (Jade y otros preguntan sin cara), así que aceptamos por todas.
        // El core de la quarry nuclear no: a ese solo le entra energía por sus puertos
        EnergyStorage.SIDED.registerForBlockEntity(
                (quarry, side) -> quarry.tier().multiblock() ? null : quarry.energy, QUARRY);
        EnergyStorage.SIDED.registerForBlockEntity((port, side) -> port.energy, ENERGY_PORT);
        // Tuberías y tolvas pueden sacar los drops del buffer por cualquier cara
        ItemStorage.SIDED.registerForBlockEntity((quarry, side) -> quarry.itemStorage(), QUARRY);

        // Cigüeña: cables y caños se pueden enchufar a la cabeza de pozo o a cualquier parte
        EnergyStorage.SIDED.registerForBlockEntity((pumpjack, side) -> pumpjack.energy, PUMPJACK);
        FluidStorage.SIDED.registerForBlockEntity((pumpjack, side) -> pumpjack.exposedTank(), PUMPJACK);
        EnergyStorage.SIDED.registerForBlockEntity((part, side) -> {
            PumpjackBlockEntity pumpjack = part.controllerEntity();
            return pumpjack != null ? pumpjack.energy : null;
        }, PUMPJACK_PART);
        FluidStorage.SIDED.registerForBlockEntity((part, side) -> {
            PumpjackBlockEntity pumpjack = part.controllerEntity();
            return pumpjack != null ? pumpjack.exposedTank() : null;
        }, PUMPJACK_PART);
    }
}
