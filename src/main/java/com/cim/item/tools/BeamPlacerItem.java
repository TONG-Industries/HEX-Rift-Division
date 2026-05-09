package com.cim.item.tools;

import com.cim.block.basic.ModBlocks;
import com.cim.block.entity.deco.BeamCollisionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class BeamPlacerItem extends Item {
    public BeamPlacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        ItemStack toolStack = context.getItemInHand();
        CompoundTag nbt = toolStack.getOrCreateTag();

        // Берем именно тот блок, по которому кликнули (например, бетон)
        BlockPos currentPos = context.getClickedPos();

        if (nbt.contains("FirstPos")) {
            BlockPos firstPos = NbtUtils.readBlockPos(nbt.getCompound("FirstPos"));

            if (firstPos.equals(currentPos)) {
                player.sendSystemMessage(Component.literal("§cТочки не могут совпадать! Сброс связи."));
                nbt.remove("FirstPos");
                return InteractionResult.FAIL;
            }

            // Высчитываем ЦЕНТРЫ блоков
            Vec3 startVec = Vec3.atCenterOf(firstPos);
            Vec3 endVec = Vec3.atCenterOf(currentPos);

            double distance = startVec.distanceTo(endVec);
            int requiredBeams = (int) Math.ceil(distance);
            Item beamItem = ModBlocks.BEAM_BLOCK.get().asItem();

            if (!player.isCreative() && countItems(player, beamItem) < requiredBeams) {
                player.sendSystemMessage(Component.literal("§cНедостаточно балок! Требуется: §e" + requiredBeams));
                return InteractionResult.FAIL;
            }

            // --- АЛГОРИТМ ПРОКЛАДКИ ЛУЧА ---
            Vec3 direction = endVec.subtract(startVec).normalize();
            double stepSize = 0.5;
            int steps = (int) Math.ceil(distance / stepSize);

            boolean masterPlaced = false;
            BlockPos masterPos = null;

            java.util.List<BlockPos> placedBlocks = new java.util.ArrayList<>();

            for (int i = 1; i <= steps; i++) {
                double currentDist = Math.min(i * stepSize, distance - 0.01);
                Vec3 stepVec = startVec.add(direction.scale(currentDist));
                BlockPos posOnLine = BlockPos.containing(stepVec);

                // Если оригинальная позиция занята твердым блоком (не заменяема и не балка),
                // ищем соседний пустой блок, чтобы не создавать огромных "слепых зон" для рендера!
                if (!level.getBlockState(posOnLine).canBeReplaced() && !level.getBlockState(posOnLine).is(ModBlocks.BEAM_COLLISION.get())) {
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                        BlockPos adj = posOnLine.relative(dir);
                        if (level.getBlockState(adj).canBeReplaced() || level.getBlockState(adj).is(ModBlocks.BEAM_COLLISION.get())) {
                            posOnLine = adj;
                            break;
                        }
                    }
                }

                if (level.getBlockState(posOnLine).canBeReplaced() || level.getBlockState(posOnLine).is(ModBlocks.BEAM_COLLISION.get())) {
                    if (level.getBlockState(posOnLine).canBeReplaced()) {
                        level.setBlock(posOnLine, ModBlocks.BEAM_COLLISION.get().defaultBlockState(), 3);
                    }
                    if (!placedBlocks.contains(posOnLine)) {
                        placedBlocks.add(posOnLine);
                    }
                }
            }

            // Распределяем сегменты рендера по блокам
            int fullBlocks = (int) distance;
            float remainder = (float) (distance - fullBlocks);
            java.util.Map<BlockPos, java.util.List<Integer>> segmentMap = new java.util.HashMap<>();
            
            for (int i = 0; i <= fullBlocks; i++) {
                if (i == fullBlocks && remainder <= 0.001f) break;
                double segLength = (i == fullBlocks) ? remainder : 1.0;
                Vec3 segCenter = startVec.add(direction.scale(i + segLength / 2.0));
                
                BlockPos closest = null;
                double minDist = Double.MAX_VALUE;
                for (BlockPos p : placedBlocks) {
                    double dist = Vec3.atCenterOf(p).distanceTo(segCenter);
                    if (dist < minDist) {
                        minDist = dist;
                        closest = p;
                    }
                }
                
                if (closest != null) {
                    segmentMap.computeIfAbsent(closest, k -> new java.util.ArrayList<>()).add(i);
                }
            }

            // Назначаем данные блокам
            for (BlockPos posOnLine : placedBlocks) {
                BlockEntity be = level.getBlockEntity(posOnLine);
                if (be instanceof BeamCollisionBlockEntity collisionBE) {
                    java.util.List<Integer> segs = segmentMap.getOrDefault(posOnLine, new java.util.ArrayList<>());
                    int[] segArray = segs.stream().mapToInt(Integer::intValue).toArray();

                    if (!masterPlaced) {
                        collisionBE.addMasterData(startVec, endVec, segArray);
                        masterPlaced = true;
                        masterPos = posOnLine;
                    } else {
                        collisionBE.addSlaveData(masterPos, startVec, endVec, segArray);
                    }
                }
            }

            if (!player.isCreative()) consumeItems(player, beamItem, requiredBeams);
            player.sendSystemMessage(Component.literal("§aБалка установлена! Потрачено: " + requiredBeams));
            nbt.remove("FirstPos");

        } else {
            nbt.put("FirstPos", NbtUtils.writeBlockPos(currentPos));
            player.sendSystemMessage(Component.literal("§aПервая точка (центр) закреплена."));
        }

        return InteractionResult.SUCCESS;
    }

    private int countItems(Player player, Item item) {
        return player.getInventory().items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private void consumeItems(Player player, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                int toTake = Math.min(stack.getCount(), remaining);
                stack.shrink(toTake);
                remaining -= toTake;
                if (remaining <= 0) break;
            }
        }
    }
}