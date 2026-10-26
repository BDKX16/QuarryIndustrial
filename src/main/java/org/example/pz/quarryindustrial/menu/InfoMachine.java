package org.example.pz.quarryindustrial.menu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Una máquina con GUI de solo lectura (y, si hace falta, un botón). Es un {@code BlockEntity}: la implementan
 * el pad, la cigüeña, la plataforma offshore y los generadores.
 */
public interface InfoMachine extends ExtendedMenuProvider<InfoMenu.Open> {
    InfoKind infoKind();

    BlockPos getBlockPos();

    /** Escribe los valores en el orden de {@link InfoKind#fields()}. */
    void fillInfo(long[] values);

    /** El botón de la GUI (solo si {@link InfoKind#hasAction}). */
    default void onInfoButton(ServerPlayer player) {
    }

    @Override
    default Component getDisplayName() {
        return infoKind().title();
    }

    @Override
    default InfoMenu.Open getScreenOpeningData(ServerPlayer player) {
        return new InfoMenu.Open(infoKind(), getBlockPos());
    }

    @Override
    default AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfoMenu(containerId, infoKind(), getBlockPos(), this);
    }
}
