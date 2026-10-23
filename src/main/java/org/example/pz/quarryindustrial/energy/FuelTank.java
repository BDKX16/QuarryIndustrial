package org.example.pz.quarryindustrial.energy;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Tanque de combustible líquido (diésel, fuel oil, biocombustible). Lo usan el generador diésel, la cigüeña
 * con motor diésel y la plataforma offshore. {@link #burn(long)} quema lo justo para entregar la energía
 * pedida; lo que sobra de un mB queda como crédito para la próxima vez.
 */
public class FuelTank extends SingleVariantStorage<FluidVariant> {
    private static final long MB = FluidConstants.BUCKET / 1000;

    private final long capacity;
    private final Runnable onChange;
    /** EU ya pagados con combustible quemado y todavía no entregados. */
    private long credit;

    public FuelTank(int buckets, Runnable onChange) {
        this.capacity = buckets * FluidConstants.BUCKET;
        this.onChange = onChange;
    }

    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return capacity;
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        return CombustionFuels.isFuel(variant);
    }

    @Override
    protected void onFinalCommit() {
        onChange.run();
    }

    /** Entrega hasta {@code eu} EU quemando combustible. Devuelve cuánto entregó. */
    public long burn(long eu) {
        long perMb = CombustionFuels.euPerMb(variant);
        while (credit < eu && perMb > 0 && amount >= MB) {
            amount -= MB;
            credit += perMb;
        }
        if (amount < MB && amount > 0 && credit < eu) {
            // Restos de menos de un mB (de otros mods con otra unidad): se descartan
            amount = 0;
        }
        if (amount == 0) {
            variant = FluidVariant.blank();
        }
        long delivered = Math.min(eu, credit);
        credit -= delivered;
        if (delivered > 0) {
            onChange.run();
        }
        return delivered;
    }

    public boolean hasFuel() {
        return credit > 0 || (amount >= MB && CombustionFuels.isFuel(variant));
    }

    public long amountMb() {
        return amount / MB;
    }

    public long capacityMb() {
        return capacity / MB;
    }

    public void save(ValueOutput output) {
        writeValue(this, FluidVariant.CODEC, output);
        output.putLong("Credit", credit);
    }

    public void load(ValueInput input) {
        readValue(this, FluidVariant.CODEC, FluidVariant::blank, input);
        credit = input.getLongOr("Credit", 0L);
    }
}
