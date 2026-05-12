package com.trd.event;

import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KineticPlacementEvents {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        if (level == null || level.isClientSide()) return;

        BlockPos placedPos = event.getPos();
        Block placedBlock = event.getPlacedBlock().getBlock();

        // Шаг 3: Сканирование зоны 3х3х3
        for (BlockPos neighborPos : BlockPos.betweenClosed(placedPos.offset(-1, -1, -1), placedPos.offset(1, 1, 1))) {
            if (neighborPos.equals(placedPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);

            // Шаг 4: Логика защиты габаритов шестерни
            if (neighborState.getBlock() instanceof ShaftBlock && neighborState.hasProperty(ShaftBlock.GEAR_SIZE)) {
                if (neighborState.getValue(ShaftBlock.GEAR_SIZE) == 2 && neighborState.hasProperty(ShaftBlock.FACING)) {
                    Direction.Axis axis = neighborState.getValue(ShaftBlock.FACING).getAxis();

                    int dx = Math.abs(placedPos.getX() - neighborPos.getX());
                    int dy = Math.abs(placedPos.getY() - neighborPos.getY());
                    int dz = Math.abs(placedPos.getZ() - neighborPos.getZ());

                    boolean inPlane = false;
                    int sum = 0;

                    if (axis == Direction.Axis.X && dx == 0) {
                        inPlane = true;
                        sum = dy + dz;
                    } else if (axis == Direction.Axis.Y && dy == 0) {
                        inPlane = true;
                        sum = dx + dz;
                    } else if (axis == Direction.Axis.Z && dz == 0) {
                        inPlane = true;
                        sum = dx + dy;
                    }

                    if (inPlane) {
                        if (sum == 1) {
                            // Крестовина: 100% заблокировано шестерней
                            event.setCanceled(true);
                            if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cЭто место заблокировано средней шестернёй!"), true);
                            }
                            return;
                        } else if (sum == 2) {
                            // Диагональ: Можно ставить только валы
                            if (!(placedBlock instanceof ShaftBlock)) {
                                event.setCanceled(true);
                                if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cПо диагонали от средней шестерни можно ставить только валы!"), true);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
