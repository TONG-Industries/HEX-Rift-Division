package com.trd.item.rotation;

import com.trd.api.rotation.ShaftDiameter;
import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.entity.industrial.rotation.ShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

public class FlywheelItem extends Item {
    private final List<ShaftDiameter> compatibleShafts;

    public FlywheelItem(Properties properties, ShaftDiameter... compatibleShafts) {
        super(properties);
        this.compatibleShafts = Arrays.asList(compatibleShafts);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof ShaftBlock shaftBlock) {
            if (level.getBlockEntity(pos) instanceof ShaftBlockEntity targetBE) {
                if (targetBE.hasCentralAttachment()) {
                    return InteractionResult.PASS;
                }
            } else {
                return InteractionResult.PASS;
            }

            // 2. Проверка совместимости диаметра вала
            if (!compatibleShafts.contains(shaftBlock.getDiameter())) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {
                if (level.getBlockEntity(pos) instanceof ShaftBlockEntity shaftBE) {
                    ItemStack flywheelStack = context.getItemInHand().copy();
                    flywheelStack.setCount(1);
                    shaftBE.setAttachedFlywheel(flywheelStack);
                }

                // 3. Обновляем стейт блока
                level.setBlock(pos, state.setValue(ShaftBlock.HAS_FLYWHEEL, true), 3);

                context.getItemInHand().shrink(1);
                level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                // 4. Пересобираем кинетическую сеть
                com.trd.api.rotation.KineticNetworkManager manager = com.trd.api.rotation.KineticNetworkManager.get((net.minecraft.server.level.ServerLevel) level);
                manager.updateNetworkAfterRemove(pos);
                manager.updateNetworkAfterPlace(pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
