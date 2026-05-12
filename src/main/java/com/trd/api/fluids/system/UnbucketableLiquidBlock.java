package com.trd.api.fluids.system;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

public class UnbucketableLiquidBlock extends LiquidBlock {

    public UnbucketableLiquidBlock(Supplier<? extends FlowingFluid> pFluid, Properties pProperties) {
        super(pFluid, pProperties);
    }

    // Блокируем ванильную логику зачерпывания ведром!
    @Override
    public ItemStack pickupBlock(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        // Мы НЕ удаляем блок жидкости и просто возвращаем "пустоту"
        return ItemStack.EMPTY;
    }
}