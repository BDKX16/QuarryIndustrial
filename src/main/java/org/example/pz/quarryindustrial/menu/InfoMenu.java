package org.example.pz.quarryindustrial.menu;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.registry.ModMenus;
import org.jetbrains.annotations.Nullable;

/**
 * Menú de las máquinas {@link InfoMachine}: los números llegan por {@link ContainerData}, cada valor en dos
 * mitades de 16 bits (por la red van como short). El pad suma sus buzones (salida y llegada) y el inventario del
 * jugador; las demás máquinas no tienen slots.
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

    // Medidas de la pantalla: las comparten el menú (dónde van los slots) y la pantalla (qué se dibuja)
    public static final int WIDTH = 176;
    public static final int MARGIN = 8;
    public static final int TOP = 20;
    public static final int LINE = 11;
    public static final int BAR_ROW = 21;
    public static final int BUTTON_H = 20;
    public static final int SLOT = 18;
    /** Alto del rótulo que va arriba de cada fila de slots. */
    public static final int LABEL = 11;
    /** Cuántos slots tiene cada buzón del pad. */
    public static final int BOX_SLOTS = 9;

    private final InfoKind kind;
    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable InfoMachine machine;
    /** Cantidad de slots de la máquina (los del jugador vienen después). */
    private final int machineSlots;

    /** Constructor del cliente. */
    public InfoMenu(int containerId, Inventory inventory, Open open) {
        this(containerId, inventory, open.kind(), open.pos(), null);
    }

    public InfoMenu(int containerId, Inventory inventory, InfoKind kind, BlockPos pos, @Nullable InfoMachine machine) {
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

        if (kind.hasInventory()) {
            Container outbox = machine == null ? new SimpleContainer(BOX_SLOTS) : machine.outbox();
            Container inbox = machine == null ? new SimpleContainer(BOX_SLOTS) : machine.inbox();
            for (int i = 0; i < BOX_SLOTS; i++) {
                addSlot(new Slot(outbox, i, MARGIN + i * SLOT, outboxY(kind)));
            }
            for (int i = 0; i < BOX_SLOTS; i++) {
                // Lo que llega se saca, no se pone
                addSlot(new Slot(inbox, i, MARGIN + i * SLOT, inboxY(kind)) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
            machineSlots = BOX_SLOTS * 2;
            int playerY = playerY(kind);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(inventory, 9 + row * 9 + col, MARGIN + col * SLOT, playerY + row * SLOT));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col, MARGIN + col * SLOT, playerY + 3 * SLOT + 4));
            }
        } else {
            machineSlots = 0;
        }
    }

    /** Alto de la parte de información: título, filas de valores y el botón. */
    public static int infoHeight(InfoKind kind) {
        int height = TOP;
        for (InfoKind.Field field : kind.fields()) {
            if (!kind.hidden(field)) {
                height += field.type() == InfoKind.Type.BAR ? BAR_ROW : LINE;
            }
        }
        return height + (kind.hasAction() ? BUTTON_H + 8 : 0) + MARGIN;
    }

    /** Alto total de la pantalla, con los slots si los hay. */
    public static int screenHeight(InfoKind kind) {
        return kind.hasInventory() ? playerY(kind) + 3 * SLOT + 4 + SLOT + MARGIN : infoHeight(kind);
    }

    public static int outboxY(InfoKind kind) {
        return infoHeight(kind) + LABEL;
    }

    public static int inboxY(InfoKind kind) {
        return outboxY(kind) + SLOT + 4 + LABEL;
    }

    public static int playerY(InfoKind kind) {
        return inboxY(kind) + SLOT + 4 + LABEL;
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

    /** Shift-clic: de la máquina al jugador, y del jugador al buzón de salida. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (machineSlots == 0 || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, BOX_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null || (!(machine instanceof BlockEntity be && be.isRemoved())
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64);
    }
}
