package org.example.pz.quarryindustrial.menu;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.registry.ModMenus;
import org.jetbrains.annotations.Nullable;

/**
 * Menú de solo lectura de las máquinas {@link InfoMachine}: sin slots, los números llegan por
 * {@link ContainerData}. Cada valor viaja en dos mitades de 16 bits (por la red van como short).
 */
public class InfoMenu extends AbstractContainerMenu {
    /** Lo que manda el servidor al abrir: qué máquina es y dónde está. */
    public record Open(InfoKind kind, BlockPos pos) {
        public static final StreamCodec<ByteBuf, Open> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT.map(ordinal -> InfoKind.values()[Math.floorMod(ordinal, InfoKind.values().length)],
                        InfoKind::ordinal), Open::kind,
                BlockPos.STREAM_CODEC, Open::pos,
                Open::new);
    }

    private final InfoKind kind;
    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable InfoMachine machine;

    /** Constructor del cliente. */
    public InfoMenu(int containerId, Inventory inventory, Open open) {
        this(containerId, open.kind(), open.pos(), null);
    }

    public InfoMenu(int containerId, InfoKind kind, BlockPos pos, @Nullable InfoMachine machine) {
        super(ModMenus.INFO, containerId);
        this.kind = kind;
        this.pos = pos;
        this.machine = machine;
        int count = kind.valueCount() * 2;
        if (machine == null) {
            this.data = new SimpleContainerData(count);
        } else {
            long[] cache = new long[kind.valueCount()];
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    if (index == 0) {
                        machine.fillInfo(cache);
                    }
                    return (int) ((cache[index / 2] >>> (16 * (index % 2))) & 0xFFFFL);
                }

                @Override
                public void set(int index, int value) {
                }

                @Override
                public int getCount() {
                    return count;
                }
            };
        }
        addDataSlots(data);
    }

    public InfoKind kind() {
        return kind;
    }

    /** El valor {@code index} del arreglo de {@link InfoMachine#fillInfo}. */
    public long value(int index) {
        return (data.get(index * 2) & 0xFFFFL) | ((data.get(index * 2 + 1) & 0xFFFFL) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (machine != null && player instanceof ServerPlayer serverPlayer) {
            machine.onInfoButton(serverPlayer);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null || (!(machine instanceof BlockEntity be && be.isRemoved())
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64);
    }
}
