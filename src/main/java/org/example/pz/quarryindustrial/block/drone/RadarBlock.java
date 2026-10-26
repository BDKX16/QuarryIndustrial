package org.example.pz.quarryindustrial.block.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.example.pz.quarryindustrial.drone.DroneNetwork;
import org.jetbrains.annotations.Nullable;

/**
 * Radar repetidor: amplía lo que ve la centralita. Si está dentro de la zona que ya cubre una centralita (o
 * otro radar), suma su propio radio alrededor; así se pueden encadenar hasta donde haga falta. No guarda
 * nada: se anota en {@link DroneNetwork} al ponerlo y la centralita lo borra del registro si ya no está.
 */
public class RadarBlock extends Block {
    public RadarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        if (level instanceof ServerLevel serverLevel) {
            DroneNetwork.get(serverLevel).addRadar(pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        DroneNetwork.get(level).removeRadar(pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
