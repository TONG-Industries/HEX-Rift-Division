package com.trd.item.rotation;

import com.trd.api.rotation.KineticNetworkManager;
import com.trd.api.rotation.ShaftMaterial;
import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.entity.industrial.rotation.ShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BevelGearItem extends Item {
    private final ShaftMaterial material;

    public BevelGearItem(Properties properties, ShaftMaterial material) {
        super(properties);
        this.material = material;
    }

    public ShaftMaterial getMaterial() {
        return material;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof ShaftBlock) {
            Direction.Axis axis = state.getValue(ShaftBlock.FACING).getAxis();
            double hitOffset = 0;

            switch (axis) {
                case X -> hitOffset = context.getClickLocation().x - pos.getX();
                case Y -> hitOffset = context.getClickLocation().y - pos.getY();
                case Z -> hitOffset = context.getClickLocation().z - pos.getZ();
            }

            boolean isStart = hitOffset < 0.5;

            if (isStart && state.getValue(ShaftBlock.HAS_BEVEL_START)) return InteractionResult.PASS;
            if (!isStart && state.getValue(ShaftBlock.HAS_BEVEL_END)) return InteractionResult.PASS;

            if (!level.isClientSide) {
                Player player = context.getPlayer();
                ItemStack stack = context.getItemInHand();

                if (level.getBlockEntity(pos) instanceof ShaftBlockEntity shaftBE) {
                    if (isStart) {
                        level.setBlock(pos, state.setValue(ShaftBlock.HAS_BEVEL_START, true), 3);
                        ItemStack copy = stack.copy();
                        copy.setCount(1);
                        shaftBE.setAttachedBevelStart(copy);
                    } else {
                        level.setBlock(pos, state.setValue(ShaftBlock.HAS_BEVEL_END, true), 3);
                        ItemStack copy = stack.copy();
                        copy.setCount(1);
                        shaftBE.setAttachedBevelEnd(copy);
                    }

                    level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (player != null && !player.isCreative()) {
                        stack.shrink(1);
                    }

                    KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterPlace(pos);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }
}
