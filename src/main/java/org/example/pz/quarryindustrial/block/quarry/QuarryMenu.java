package org.example.pz.quarryindustrial.block.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.item.DrillBitItem;
import org.example.pz.quarryindustrial.item.UpgradeItem;
import org.example.pz.quarryindustrial.registry.ModMenus;
import org.jetbrains.annotations.Nullable;

/**
 * GUI de la quarry: 4 slots de upgrades, el buffer de 27 slots (solo para sacar) y el inventario del jugador.
 * Los números (energía, entrada, estado, capa, avance, puertos) llegan por {@link ContainerData}.
 */
public class QuarryMenu extends AbstractContainerMenu {
    // Índices de ContainerData. ENERGY, CAPACITY e INPUT ocupan dos (mitad baja y alta)
    public static final int ENERGY = 0;
    public static final int CAPACITY = 2;
    public static final int INPUT = 4;
    public static final int STATUS = 6;
    public static final int LAYER = 7;
    public static final int PROGRESS = 8;
    public static final int PORTS = 9;
    public static final int SIDE = 10;
    public static final int PORT_INPUTS = 11;
    public static final int MAX_PORTS_SHOWN = 8;
    public static final int COOLANT = PORT_INPUTS + MAX_PORTS_SHOWN;
    public static final int COOLANT_CAPACITY = COOLANT + 1;
    public static final int HEAT = COOLANT + 2;
    public static final int DATA_COUNT = HEAT + 1;

    // Posiciones de los slots (las usa la pantalla para dibujar los huecos)
    /** Los upgrades van en un panel lateral, a la derecha del panel principal de 176 px. */
    public static final int UPGRADE_X = 183;
    public static final int UPGRADE_Y = 18;
    /** La broca va en el mismo panel lateral, debajo de los upgrades. */
    public static final int BIT_X = UPGRADE_X;
    public static final int BIT_Y = UPGRADE_Y + QuarryBlockEntity.UPGRADE_SLOTS * 18 + 12;
    public static final int BUFFER_X = 8;
    public static final int BUFFER_Y = 82;
    public static final int INVENTORY_Y = 148;

    private static final int UPGRADE_START = 0;
    private static final int BIT_SLOT = QuarryBlockEntity.UPGRADE_SLOTS;
    private static final int BUFFER_START = BIT_SLOT + 1;
    private static final int PLAYER_START = BUFFER_START + QuarryBlockEntity.BUFFER_SLOTS;

    private final ContainerData data;
    private final BlockPos pos;
    /** Del lado del servidor, la quarry de verdad; en el cliente, la copia sincronizada (puede faltar). */
    private final @Nullable QuarryBlockEntity quarry;

    /** Constructor del cliente: los contenedores son copias vacías que llena la sincronización de slots. */
    public QuarryMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, pos,
                inventory.player.level().getBlockEntity(pos) instanceof QuarryBlockEntity be ? be : null,
                new SimpleContainer(QuarryBlockEntity.UPGRADE_SLOTS), new SimpleContainer(1),
                new SimpleContainer(QuarryBlockEntity.BUFFER_SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    public QuarryMenu(int containerId, Inventory inventory, QuarryBlockEntity quarry, ContainerData data) {
        this(containerId, inventory, quarry.getBlockPos(), quarry, quarry.upgradeSlots(), quarry.bitSlot(),
                quarry.buffer(), data);
    }

    private QuarryMenu(int containerId, Inventory inventory, BlockPos pos, @Nullable QuarryBlockEntity quarry,
                       Container upgrades, Container bit, Container buffer, ContainerData data) {
        super(ModMenus.QUARRY, containerId);
        this.pos = pos;
        this.quarry = quarry;
        this.data = data;
        for (int i = 0; i < QuarryBlockEntity.UPGRADE_SLOTS; i++) {
            final int index = i;
            addSlot(new Slot(upgrades, i, UPGRADE_X, UPGRADE_Y + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return QuarryBlockEntity.canPlaceUpgrade(upgrades, index, stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return stack.getItem() instanceof UpgradeItem upgrade ? upgrade.type().maxInstalled() : 0;
                }
            });
        }
        addSlot(new Slot(bit, 0, BIT_X, BIT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DrillBitItem;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(buffer, col + row * 9, BUFFER_X + col * 18, BUFFER_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public @Nullable QuarryBlockEntity quarry() {
        return quarry;
    }

    private long joined(int index) {
        return (data.get(index) & 0xFFFFL) | ((data.get(index + 1) & 0xFFFFL) << 16);
    }

    public long energy() {
        return joined(ENERGY);
    }

    public long capacity() {
        return joined(CAPACITY);
    }

    public long input() {
        return joined(INPUT);
    }

    public QuarryStatus status() {
        QuarryStatus[] values = QuarryStatus.values();
        int ordinal = data.get(STATUS);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : QuarryStatus.IDLE;
    }

    public int layer() {
        return (short) data.get(LAYER);
    }

    public int progressPermille() {
        return data.get(PROGRESS);
    }

    public int ports() {
        return data.get(PORTS);
    }

    public int side() {
        return data.get(SIDE);
    }

    public int portInput(int port) {
        return data.get(PORT_INPUTS + port);
    }

    public int coolant() {
        return data.get(COOLANT);
    }

    public int coolantCapacity() {
        return data.get(COOLANT_CAPACITY);
    }

    public int heat() {
        return data.get(HEAT);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            // De la quarry al jugador
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof UpgradeItem) {
            // Del jugador a los upgrades
            if (!moveItemStackTo(stack, UPGRADE_START, BIT_SLOT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof DrillBitItem) {
            if (!moveItemStackTo(stack, BIT_SLOT, BIT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return quarry == null || (!quarry.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64);
    }
}
