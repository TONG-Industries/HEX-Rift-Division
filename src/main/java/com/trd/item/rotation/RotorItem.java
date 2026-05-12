package com.trd.item.rotation;

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

public class RotorItem extends Item {
    public RotorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof ShaftBlock) {
            if (level.getBlockEntity(pos) instanceof ShaftBlockEntity shaftBE) {
                if (!shaftBE.hasRotor()) {
                    if (!level.isClientSide) {
                        ItemStack rotorStack = context.getItemInHand().copy();
                        rotorStack.setCount(1);
                        shaftBE.setAttachedRotor(rotorStack);
                        
                        context.getItemInHand().shrink(1);
                        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 1.0F);

                        // Trigger recalculation
                        com.trd.api.rotation.KineticNetworkManager manager = com.trd.api.rotation.KineticNetworkManager.get((net.minecraft.server.level.ServerLevel) level);
                        var net = manager.getNetworkFor(pos);
                        if (net != null) {
                            net.requestRecalculation();
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
