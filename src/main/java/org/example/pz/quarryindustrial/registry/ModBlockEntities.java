package org.example.pz.quarryindustrial.registry;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.example.pz.quarryindustrial.Quarryindustrial;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.block.generator.DieselGeneratorBlockEntity;
import org.example.pz.quarryindustrial.block.generator.SteamTurbineBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreDeckBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
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

    public static final BlockEntityType<DieselGeneratorBlockEntity> DIESEL_GENERATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("diesel_generator"),
            new BlockEntityType<>(DieselGeneratorBlockEntity::new, Set.of(ModBlocks.DIESEL_GENERATOR)));
    public static final BlockEntityType<SteamTurbineBlockEntity> STEAM_TURBINE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("steam_turbine"),
            new BlockEntityType<>(SteamTurbineBlockEntity::new, Set.of(ModBlocks.STEAM_TURBINE)));

    public static final BlockEntityType<OffshoreRigBlockEntity> OFFSHORE_RIG = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("offshore_rig"),
            new BlockEntityType<>(OffshoreRigBlockEntity::new, Set.of(ModBlocks.OFFSHORE_RIG)));
    public static final BlockEntityType<OffshoreDeckBlockEntity> OFFSHORE_DECK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("offshore_deck"),
            new BlockEntityType<>(OffshoreDeckBlockEntity::new, Set.of(ModBlocks.OFFSHORE_DECK)));

    public static final BlockEntityType<DronePadBlockEntity> DRONE_PAD = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Quarryindustrial.id("drone_pad"),
            new BlockEntityType<>(DronePadBlockEntity::new, Set.of(ModBlocks.DRONE_PAD)));

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
        // Agua de refrigeración: entra por cualquier cara (en la nuclear, por el core)
        FluidStorage.SIDED.registerForBlockEntity((quarry, side) -> quarry.coolantStorage(), QUARRY);

        // Cigüeña: cables y caños se pueden enchufar a la cabeza de pozo o a cualquier parte
        EnergyStorage.SIDED.registerForBlockEntity((pumpjack, side) -> pumpjack.energy, PUMPJACK);
        FluidStorage.SIDED.registerForBlockEntity((pumpjack, side) -> pumpjack.exposedTank(), PUMPJACK);
        EnergyStorage.SIDED.registerForBlockEntity((part, side) -> {
            PumpjackBlockEntity pumpjack = part.controllerEntity();
            return pumpjack != null ? pumpjack.energy : null;
        }, PUMPJACK_PART);
        FluidStorage.SIDED.registerForBlockEntity((part, side) -> {
            PumpjackBlockEntity pumpjack = part.controllerEntity();
            return pumpjack != null ? pumpjack.fluidAt(part.getBlockPos()) : null;
        }, PUMPJACK_PART);

        // Generadores: los cables sacan la energía; los caños meten combustible o vapor
        EnergyStorage.SIDED.registerForBlockEntity((generator, side) -> generator.energy, DIESEL_GENERATOR);
        FluidStorage.SIDED.registerForBlockEntity((generator, side) -> generator.fluidInput(), DIESEL_GENERATOR);
        EnergyStorage.SIDED.registerForBlockEntity((turbine, side) -> turbine.energy, STEAM_TURBINE);
        FluidStorage.SIDED.registerForBlockEntity((turbine, side) -> turbine.fluidInput(), STEAM_TURBINE);

        // Offshore: combustible y energía por el controlador; oil (solo salida) y energía por la cubierta
        EnergyStorage.SIDED.registerForBlockEntity((rig, side) -> rig.energy, OFFSHORE_RIG);
        FluidStorage.SIDED.registerForBlockEntity((rig, side) -> rig.fuelInput(), OFFSHORE_RIG);
        EnergyStorage.SIDED.registerForBlockEntity((deck, side) -> {
            OffshoreRigBlockEntity rig = deck.controllerEntity();
            return rig != null ? rig.energy : null;
        }, OFFSHORE_DECK);
        FluidStorage.SIDED.registerForBlockEntity((deck, side) -> {
            OffshoreRigBlockEntity rig = deck.controllerEntity();
            return rig != null ? rig.oilOutput() : null;
        }, OFFSHORE_DECK);

        // Pad de drones: entra al buzón de salida, sale del de llegada (items, fluidos y energía)
        ItemStorage.SIDED.registerForBlockEntity((pad, side) -> pad.itemStorage(), DRONE_PAD);
        FluidStorage.SIDED.registerForBlockEntity((pad, side) -> pad.fluidStorage(), DRONE_PAD);
        EnergyStorage.SIDED.registerForBlockEntity((pad, side) -> pad.energyStorage(), DRONE_PAD);
    }
}
