package com.trd.item.energy;

import com.trd.block.entity.industrial.energy.ConnectorBlockEntity;
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
import com.trd.util.CatenaryHelper;
import com.trd.multiblock.system.IMultiblockPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WireCoilItem extends Item {
    public WireCoilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);

        // Если кликнули НЕ по коннектору
        if (!(be instanceof ConnectorBlockEntity currentConnector)) {
            // Если игрок кликнул с Shift по воздуху/другому блоку — сбрасываем сохраненные координаты
            if (player != null && player.isShiftKeyDown() && stack.hasTag() && stack.getTag().contains("FirstPos")) {
                stack.getTag().remove("FirstPos");
                player.displayClientMessage(Component.literal("§eСоединение отменено."), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        CompoundTag tag = stack.getOrCreateTag();

        // ================= ПЕРВЫЙ КЛИК =================
        if (!tag.contains("FirstPos")) {
            // Проверяем, есть ли свободные слоты для проводов у этого коннектора
            if (currentConnector.getConnections().size() >= currentConnector.getTier().maxConnections()) {
                if (player != null) player.displayClientMessage(Component.literal("§cЭтот коннектор уже полностью занят!"), true);
                return InteractionResult.FAIL;
            }

            // Сохраняем координаты в предмет
            tag.put("FirstPos", NbtUtils.writeBlockPos(pos));
            if (player != null) player.displayClientMessage(Component.literal("§aНачато соединение... Кликните по второму коннектору."), true);
            return InteractionResult.SUCCESS;
        }

        // ================= ВТОРОЙ КЛИК =================
        else {
            BlockPos firstPos = NbtUtils.readBlockPos(tag.getCompound("FirstPos"));

            // Очищаем тег в любом случае, чтобы игрок не застрял, если произойдёт ошибка
            tag.remove("FirstPos");

            // 1. Проверка на клик по тому же самому блоку
            if (pos.equals(firstPos)) {
                if (player != null) player.displayClientMessage(Component.literal("§cНельзя соединить коннектор с самим собой!"), true);
                return InteractionResult.FAIL;
            }

            BlockEntity firstBe = level.getBlockEntity(firstPos);
            if (!(firstBe instanceof ConnectorBlockEntity firstConnector)) {
                if (player != null) player.displayClientMessage(Component.literal("§cПервый коннектор был разрушен или потерян."), true);
                return InteractionResult.FAIL;
            }

            // 2. Проверка лимитов подключений для ОБОИХ коннекторов
            if (firstConnector.getConnections().size() >= firstConnector.getTier().maxConnections()) {
                if (player != null) player.displayClientMessage(Component.literal("§cПервый коннектор уже полностью занят!"), true);
                return InteractionResult.FAIL;
            }
            if (currentConnector.getConnections().size() >= currentConnector.getTier().maxConnections()) {
                if (player != null) player.displayClientMessage(Component.literal("§cВторой коннектор уже полностью занят!"), true);
                return InteractionResult.FAIL;
            }

            // 3. Проверка: не соединены ли они уже друг с другом?
            if (firstConnector.getConnections().contains(pos) || currentConnector.getConnections().contains(firstPos)) {
                if (player != null) player.displayClientMessage(Component.literal("§cЭти коннекторы уже соединены!"), true);
                return InteractionResult.FAIL;
            }

            // 4. Проверка дистанции (берём наименьшую из двух, чтобы нельзя было обмануть систему слабым коннектором)
            double distance = Math.sqrt(firstPos.distSqr(pos));
            int maxDist1 = firstConnector.getTier().maxLength();
            int maxDist2 = currentConnector.getTier().maxLength();
            int maxAllowed = Math.min(maxDist1, maxDist2);

            if (distance > maxAllowed) {
                if (player != null) player.displayClientMessage(Component.literal("§cСлишком далеко! Максимальная длина: " + maxAllowed + " блоков."), true);
                return InteractionResult.FAIL;
            }

            // 5. Проверка на препятствия
            if (isPathBlocked(level, firstConnector, currentConnector, player)) {
                return InteractionResult.FAIL;
            }

            // ================= УСПЕХ: СОЕДИНЯЕМ =================
            // Записываем друг друга в память
            firstConnector.connectTo(pos);
            currentConnector.connectTo(firstPos);

            if (player != null) player.displayClientMessage(Component.literal("§bСоединение успешно установлено!"), true);
            return InteractionResult.SUCCESS;
        }
    }

    private boolean isPathBlocked(Level level, ConnectorBlockEntity startBe, ConnectorBlockEntity endBe, Player player) {
        Vec3 start = startBe.getWireAttachmentPoint();
        Vec3 end = endBe.getWireAttachmentPoint();
        BlockPos startPos = startBe.getBlockPos();
        BlockPos endPos = endBe.getBlockPos();

        CatenaryHelper.CatenaryData data = CatenaryHelper.compute(start, end);
        int segments = 12; // Достаточно для проверки коллизий
        Vec3 prev = start;

        for (int i = 1; i <= segments; i++) {
            Vec3 current = data.getPoint((double) i / segments);
            BlockHitResult hit = level.clip(new ClipContext(prev, current, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hit.getBlockPos();
                if (!hitPos.equals(startPos) && !hitPos.equals(endPos)) {
                    if (player != null) {
                        BlockState hitState = level.getBlockState(hitPos);
                        String blockName = hitState.getBlock().getName().getString();

                        // Если это часть мультиблока, показываем название всей машины
                        BlockEntity be = level.getBlockEntity(hitPos);
                        if (be instanceof IMultiblockPart part && part.getControllerPos() != null) {
                            blockName = level.getBlockState(part.getControllerPos()).getBlock().getName().getString();
                        }

                        player.displayClientMessage(Component.literal("§cПуть заблокирован: " + blockName), true);
                    }
                    return true;
                }
            }
            prev = current;
        }
        return false;
    }
}