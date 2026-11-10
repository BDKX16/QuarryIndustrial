package org.example.pz.quarryindustrial.filter;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.example.pz.quarryindustrial.registry.ModComponents;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * Cómo se usa el filtro de caño, todo con clic derecho sobre el brazo del caño que toca el inventario:
 * <ul>
 *   <li>con el filtro en la mano: se engancha a esa cara;</li>
 *   <li>con la mano vacía: abre su GUI (agachado, lo saca y lo devuelve con su configuración).</li>
 * </ul>
 * La llave de Ala sigue sirviendo para cambiar el modo de la cara.
 */
public final class PipeFilterEvents {
    /** Más allá de esto, desde el centro del bloque, el clic está sobre un brazo y no sobre el núcleo. */
    private static final double ARM_FROM_CENTER = 0.2;

    private PipeFilterEvents() {
    }

    public static void init() {
        UseBlockCallback.EVENT.register(PipeFilterEvents::onUse);
    }

    private static InteractionResult onUse(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        BlockEntity be = level.getBlockEntity(hit.getBlockPos());
        if (!(be instanceof PipeFilterHolder holder)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean placing = held.is(ModItems.PIPE_FILTER);
        if (!placing && !held.isEmpty()) {
            return InteractionResult.PASS; // la llave y demás ítems siguen con su comportamiento
        }
        Direction face = faceAt(hit, holder, be);
        if (face == null) {
            return placing ? fail(player, level, "message.quarryindustrial.pipe_filter.no_face") : InteractionResult.PASS;
        }
        PipeFilterData existing = holder.quarry$getFilter(face);
        if (placing) {
            if (existing != null) {
                return fail(player, level, "message.quarryindustrial.pipe_filter.occupied");
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            PipeFilterData stored = held.get(ModComponents.PIPE_FILTER);
            holder.quarry$setFilter(face, stored == null ? new PipeFilterData() : stored.copy());
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (existing == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            holder.quarry$setFilter(face, null);
            ItemStack back = existing.toStack();
            if (!player.getInventory().add(back)) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), back);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        open((ServerPlayer) player, be, face, existing);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult fail(Player player, Level level, String key) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable(key));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * La cara del caño que se está apuntando: el brazo sobre el que cae el clic (si lo hay y el caño se conecta por
     * ahí) o, si no, la cara del bloque que se tocó.
     */
    static @Nullable Direction faceAt(BlockHitResult hit, PipeFilterHolder holder, BlockEntity pipe) {
        Vec3 local = hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos()));
        Direction best = null;
        double bestDistance = ARM_FROM_CENTER;
        for (Direction direction : Direction.values()) {
            double along = local.get(direction.getAxis()) * direction.getAxisDirection().getStep();
            if (along > bestDistance && connectsToInventory(pipe, direction)) {
                best = direction;
                bestDistance = along;
            }
        }
        if (best != null) {
            return best;
        }
        Direction clicked = hit.getDirection();
        if (connectsToInventory(pipe, clicked)) {
            return clicked;
        }
        return holder.quarry$getFilter(clicked) != null ? clicked : null;
    }

    /** ¿El caño se conecta por esta cara con algo que no sea otro caño (un inventario)? */
    private static boolean connectsToInventory(BlockEntity pipe, Direction direction) {
        Level level = pipe.getLevel();
        if (level == null || level.getBlockEntity(pipe.getBlockPos().relative(direction)) instanceof PipeFilterHolder) {
            return false;
        }
        // Ala dibuja el brazo solo si la cara se conecta: se le pregunta a su propio método
        try {
            return (boolean) pipe.getClass().getMethod("connects", Direction.class).invoke(pipe, direction);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }

    /** Abre la GUI del filtro de esa cara. */
    public static void open(ServerPlayer player, BlockEntity pipe, Direction face, PipeFilterData data) {
        BlockPos pos = pipe.getBlockPos();
        player.openMenu(new ExtendedMenuProvider<PipeFilterMenu.Open>() {
            @Override
            public PipeFilterMenu.Open getScreenOpeningData(ServerPlayer p) {
                return new PipeFilterMenu.Open(pos, face);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.quarryindustrial.pipe_filter.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return new PipeFilterMenu(containerId, inventory, pos, face, data, pipe);
            }
        });
    }
}
