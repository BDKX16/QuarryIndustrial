package org.example.pz.quarryindustrial.block.generator;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.material.Fluid;
import org.example.pz.quarryindustrial.compat.AlaSteamNozzle;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.energy.CombustionFuels;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;

/**
 * Turbina de vapor. En Ala Industrial el vapor del reactor sale por los caños de vapor hasta una Steam Nozzle,
 * que lo ventea al aire. La turbina se pone tapando la boca de la nozzle: se toma el vapor antes de que salga
 * y lo convierte en EU. También acepta vapor de cualquier caño o mod que se lo empuje.
 */
public class SteamTurbineBlockEntity extends GeneratorBlockEntity {
    private static final long MB = FluidConstants.BUCKET / 1000;

    public final SingleVariantStorage<FluidVariant> steam = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return QuarryConfig.get().generators.turbineTankBuckets * FluidConstants.BUCKET;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return CombustionFuels.isSteam(variant);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };
    private final Storage<FluidVariant> exposedSteam = FilteringStorage.insertOnlyOf(steam);

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_TURBINE, pos, state, QuarryConfig.get().generators.turbineCapacity,
                QuarryConfig.get().generators.turbineMaxOutputPerTick);
    }

    @Override
    protected long generate(ServerLevel level, BlockPos pos) {
        QuarryConfig.Generators config = QuarryConfig.get().generators;
        pullSteam(level, pos, config.turbineMaxSteamMbPerTick * MB);
        long perMb = Math.max(1, config.turbineEuPerMbSteam);
        long mb = Math.min(Math.min(steam.amount / MB, config.turbineMaxSteamMbPerTick), space() / perMb);
        if (mb <= 0) {
            return 0;
        }
        steam.amount -= mb * MB;
        if (steam.amount == 0) {
            steam.variant = FluidVariant.blank();
        }
        energy.amount += mb * perMb;
        return mb * perMb;
    }

    /**
     * Junta vapor de los vecinos: de una Steam Nozzle de Ala que tenga la boca hacia la turbina, o de
     * cualquier tanque que lo deje sacar por la API de fluidos.
     */
    private void pullSteam(ServerLevel level, BlockPos pos, long max) {
        for (Direction direction : Direction.values()) {
            long room = steam.getCapacity() - steam.amount;
            if (room <= 0) {
                return;
            }
            Fluid steamFluid = CombustionFuels.steamFluid();
            long fromNozzle = steamFluid == null ? 0 : AlaSteamNozzle.drain(level, pos, direction, Math.min(max, room) / MB);
            if (fromNozzle > 0) {
                steam.variant = FluidVariant.of(steamFluid);
                steam.amount += fromNozzle * MB;
                setChanged();
                continue;
            }
            Storage<FluidVariant> source = FluidStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (source != null && source.supportsExtraction()) {
                StorageUtil.move(source, steam, CombustionFuels::isSteam, Math.min(max, room), null);
            }
        }
    }

    @Override
    public Storage<FluidVariant> fluidInput() {
        return exposedSteam;
    }

    public long steamMb() {
        return steam.amount / MB;
    }

    @Override
    public Component statusMessage() {
        return Component.translatable("message.quarryindustrial.steam_turbine.status",
                Component.translatable(running() ? "status.quarryindustrial.generator.running"
                        : "status.quarryindustrial.generator.idle"),
                averageOutput(), energy.amount, energy.getCapacity(), steamMb(), steam.getCapacity() / MB);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        SingleVariantStorage.writeValue(steam, FluidVariant.CODEC, output.child("Steam"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        SingleVariantStorage.readValue(steam, FluidVariant.CODEC, FluidVariant::blank, input.childOrEmpty("Steam"));
    }
}
