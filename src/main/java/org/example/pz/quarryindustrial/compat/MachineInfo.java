package org.example.pz.quarryindustrial.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.example.pz.quarryindustrial.block.drone.DronePadBlockEntity;
import org.example.pz.quarryindustrial.block.generator.DieselGeneratorBlockEntity;
import org.example.pz.quarryindustrial.block.generator.GeneratorBlockEntity;
import org.example.pz.quarryindustrial.block.generator.SteamTurbineBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreDeckBlockEntity;
import org.example.pz.quarryindustrial.block.offshore.OffshoreRigBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackBlockEntity;
import org.example.pz.quarryindustrial.block.pumpjack.PumpjackPartBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.EnergyPortBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryBlockEntity;
import org.example.pz.quarryindustrial.block.quarry.QuarryStatus;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.drone.DroneKind;
import org.example.pz.quarryindustrial.item.DrillBitItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Las líneas de información de cada máquina, del lado del servidor (con los números de verdad). Las muestra
 * Jade al mirar el bloque; las partes de los multibloques muestran las de su controlador.
 */
public final class MachineInfo {
    private MachineInfo() {
    }

    public static List<Component> lines(BlockEntity be) {
        List<Component> lines = new ArrayList<>();
        if (be instanceof PumpjackPartBlockEntity part && part.controllerEntity() != null) {
            be = part.controllerEntity();
        } else if (be instanceof OffshoreDeckBlockEntity deck && deck.controllerEntity() != null) {
            be = deck.controllerEntity();
        }
        switch (be) {
            case QuarryBlockEntity quarry -> quarry(quarry, lines);
            case PumpjackBlockEntity pumpjack -> {
                lines.add(status(pumpjack.status().label()));
                lines.add(energy(pumpjack.energy.getAmount(), pumpjack.energy.getCapacity()));
                lines.add(Component.translatable("jade.quarryindustrial.oil", pumpjack.oilMb(), pumpjack.tankCapacityMb()));
                if (pumpjack.fuel.amountMb() > 0) {
                    lines.add(Component.translatable("jade.quarryindustrial.fuel", pumpjack.fuel.amountMb(),
                            pumpjack.fuel.capacityMb()));
                }
                upgrades(pumpjack.speedUpgrades(), lines);
            }
            case OffshoreRigBlockEntity rig -> {
                lines.add(status(rig.status().label()));
                lines.add(Component.translatable("jade.quarryindustrial.drilled", (int) rig.drilled(),
                        QuarryConfig.get().offshore.drillDepth));
                lines.add(energy(rig.energy.getAmount(), rig.energy.getCapacity()));
                lines.add(Component.translatable("jade.quarryindustrial.oil", rig.oilMb(), rig.tankCapacityMb()));
                lines.add(Component.translatable("jade.quarryindustrial.fuel", rig.fuel.amountMb(), rig.fuel.capacityMb()));
                bit(rig.bit(), lines);
                upgrades(rig.speedUpgrades(), lines);
            }
            case GeneratorBlockEntity generator -> {
                lines.add(status(Component.translatable(generator.running() ? "status.quarryindustrial.generator.running"
                        : "status.quarryindustrial.generator.idle")));
                lines.add(Component.translatable("jade.quarryindustrial.output", generator.averageOutput()));
                lines.add(energy(generator.energy.getAmount(), generator.energy.getCapacity()));
                if (generator instanceof DieselGeneratorBlockEntity diesel) {
                    lines.add(Component.translatable("jade.quarryindustrial.fuel", diesel.fuel.amountMb(),
                            diesel.fuel.capacityMb()));
                } else if (generator instanceof SteamTurbineBlockEntity turbine) {
                    lines.add(Component.translatable("jade.quarryindustrial.steam", turbine.steamMb()));
                }
            }
            case DronePadBlockEntity pad -> {
                lines.add(status(pad.status().label()));
                DroneKind kind = pad.droneKind();
                lines.add(Component.translatable("jade.quarryindustrial.drone",
                        kind != null ? kind.label() : Component.translatable("status.quarryindustrial.drone_pad.none")));
                if (pad.link() != null) {
                    lines.add(Component.translatable("jade.quarryindustrial.link", pad.link().getX(), pad.link().getY(),
                            pad.link().getZ()));
                }
                lines.add(energy(pad.energy.getAmount(), pad.energy.getCapacity()));
            }
            case EnergyPortBlockEntity port -> lines.add(energy(port.energy.getAmount(), port.energy.getCapacity()));
            default -> {
            }
        }
        return lines;
    }

    private static void quarry(QuarryBlockEntity quarry, List<Component> lines) {
        lines.add(status(quarry.status().label()));
        lines.add(Component.translatable("jade.quarryindustrial.energy_in", quarry.energy.getAmount(),
                quarry.energy.getCapacity(), quarry.averageInput()));
        bit(quarry.bitSlot().getItem(0), lines);
        if (quarry.coolantMb() > 0) {
            lines.add(Component.translatable("jade.quarryindustrial.coolant", quarry.coolantMb(), quarry.coolantCapacityMb()));
        }
        if (quarry.tier().multiblock()) {
            lines.add(Component.translatable("jade.quarryindustrial.heat", quarry.heat() / 10));
            lines.add(Component.translatable("jade.quarryindustrial.ports", quarry.energyPortCount()));
        }
        if (quarry.status() != QuarryStatus.FINISHED && quarry.area() != null) {
            lines.add(Component.translatable("jade.quarryindustrial.layer", quarry.currentY(),
                    quarry.progressPermille() / 10));
        }
    }

    private static void bit(ItemStack bit, List<Component> lines) {
        if (bit.isEmpty()) {
            lines.add(Component.translatable("jade.quarryindustrial.no_bit").withStyle(ChatFormatting.RED));
            return;
        }
        int life = Math.round((1 - DrillBitItem.wear(bit)) * 100);
        lines.add(Component.translatable("jade.quarryindustrial.bit", bit.getHoverName(), life)
                .withStyle(life < 15 ? ChatFormatting.RED : ChatFormatting.WHITE));
    }

    private static void upgrades(int speed, List<Component> lines) {
        if (speed > 0) {
            lines.add(Component.translatable("jade.quarryindustrial.speed_upgrades", speed));
        }
    }

    private static Component status(Component label) {
        return Component.translatable("jade.quarryindustrial.status", label).withStyle(ChatFormatting.GOLD);
    }

    private static Component energy(long amount, long capacity) {
        return Component.translatable("jade.quarryindustrial.energy", amount, capacity);
    }
}
