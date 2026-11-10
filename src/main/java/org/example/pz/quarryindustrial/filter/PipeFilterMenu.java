package org.example.pz.quarryindustrial.filter;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.example.pz.quarryindustrial.registry.ModMenus;
import org.jetbrains.annotations.Nullable;

/**
 * GUI del filtro de una cara de caño: 5 casilleros fantasma (se marca con el ítem del cursor, no se consume), el slot
 * de la mejora, y botones para lista blanca/negra y ítem/tag. Los ajustes y el tag elegido de cada casillero llegan
 * por {@link ContainerData}; los ítems, por la sincronización normal de slots.
 */
public class PipeFilterMenu extends AbstractContainerMenu {
    public record Open(BlockPos pos, Direction face) {
        public static final StreamCodec<ByteBuf, Open> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::pos,
                Direction.STREAM_CODEC, Open::face,
                Open::new);
    }

    // Botones del menú
    public static final int BUTTON_MODE = 0;
    public static final int BUTTON_MATCH = 1;

    // Medidas de la pantalla
    public static final int WIDTH = 176;
    public static final int HEIGHT = 178;
    public static final int CELL_Y = 30;
    /** Posición x del casillero 0 (el del medio); los demás se abren hacia los costados al activarse. */
    private static final int CELL_CENTER_X = 80;
    private static final int[] CELL_OFFSETS = {0, -18, 18, -36, 36};
    /** La expansión va en un panel a la derecha del principal, como las mejoras de la quarry. */
    public static final int UPGRADE_X = WIDTH + 25;
    public static final int UPGRADE_Y = 30;
    public static final int INVENTORY_Y = 96;

    // Índices de ContainerData
    private static final int D_ALLOW = 0;
    private static final int D_TAG = 1;
    private static final int D_TAGS = 2;
    private static final int D_COUNT = D_TAGS + PipeFilterData.CELLS;

    private final BlockPos pos;
    private final Direction face;
    private final PipeFilterData filter;
    private final ContainerData data;
    private final @Nullable BlockEntity pipe;

    /** Constructor del cliente. */
    public PipeFilterMenu(int containerId, Inventory inventory, Open open) {
        this(containerId, inventory, open.pos(), open.face(), new PipeFilterData(), null);
    }

    public PipeFilterMenu(int containerId, Inventory inventory, BlockPos pos, Direction face, PipeFilterData filter,
                          @Nullable BlockEntity pipe) {
        super(ModMenus.PIPE_FILTER, containerId);
        this.pos = pos;
        this.face = face;
        this.filter = filter;
        this.pipe = pipe;
        this.data = pipe == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_ALLOW -> filter.allow ? 1 : 0;
                    case D_TAG -> filter.tagMatch ? 1 : 0;
                    default -> filter.tagIndex[index - D_TAGS];
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return D_COUNT;
            }
        };
        addDataSlots(data);

        for (int i = 0; i < PipeFilterData.CELLS; i++) {
            // Casilleros fantasma: no se puede poner ni sacar nada, el clic se resuelve en clicked()
            int cell = i;
            addSlot(new Slot(filter, i, cellX(i), CELL_Y) {
                /** Sin la expansión se ve un solo casillero. */
                @Override
                public boolean isActive() {
                    return cell < filter.activeCells();
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addSlot(new Slot(filter, PipeFilterData.UPGRADE_SLOT, UPGRADE_X, UPGRADE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PIPE_FILTER_UPGRADE);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    /** Dónde va el casillero {@code cell}: el primero al medio, los otros alternando a izquierda y derecha. */
    public static int cellX(int cell) {
        return CELL_CENTER_X + CELL_OFFSETS[cell];
    }

    public BlockPos pos() {
        return pos;
    }

    public Direction face() {
        return face;
    }

    public PipeFilterData filter() {
        return filter;
    }

    public boolean allowList() {
        return data.get(D_ALLOW) != 0;
    }

    public boolean tagMatch() {
        return data.get(D_TAG) != 0;
    }

    public int tagIndex(int cell) {
        return data.get(D_TAGS + cell);
    }

    /** Casilleros que cuentan según la mejora instalada (el cliente la ve por el slot sincronizado). */
    public int activeCells() {
        return filter.activeCells();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pipe == null) {
            return false;
        }
        if (id == BUTTON_MODE) {
            filter.allow = !filter.allow;
        } else if (id == BUTTON_MATCH) {
            filter.tagMatch = !filter.tagMatch;
        } else {
            return false;
        }
        filter.setChanged();
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < PipeFilterData.CELLS) {
            ghostClick(slotId, button);
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    /**
     * Clic en un casillero: con un ítem en el cursor lo marca (sin gastarlo); con la mano vacía, en modo tag cambia
     * al siguiente tag del ítem y en modo ítem lo borra; el clic derecho siempre lo borra.
     */
    private void ghostClick(int cell, int button) {
        if (pipe == null || cell >= filter.activeCells()) {
            return;
        }
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            filter.setSample(cell, carried);
        } else if (filter.getItem(cell).isEmpty()) {
            return;
        } else if (button == 0 && filter.tagMatch) {
            filter.cycleTag(cell);
        } else {
            filter.setSample(cell, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index < PipeFilterData.CELLS) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int upgrade = PipeFilterData.UPGRADE_SLOT;
        if (index == upgrade) {
            if (!moveItemStackTo(stack, upgrade + 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(ModItems.PIPE_FILTER_UPGRADE) || !moveItemStackTo(stack, upgrade, upgrade + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override
    public boolean stillValid(Player player) {
        return pipe == null || (!pipe.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64);
    }
}
