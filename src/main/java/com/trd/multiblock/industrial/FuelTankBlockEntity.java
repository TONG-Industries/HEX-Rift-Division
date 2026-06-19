package com.trd.multiblock.industrial;

import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.industrial.fluids.FluidBarrelBlockEntity;
import com.trd.menu.FuelTankMenu;
import com.trd.multiblock.system.IFluidTankProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Цистерна (FuelTank) — мультиблочный бак большой ёмкости.
 * Наследуется от FluidBarrelBlockEntity, переопределяя ёмкость и скорость трансфера.
 * Цистерна не подвержена коррозии/нагреву (нет checkDamage/processLeaking).
 */
public class FuelTankBlockEntity extends FluidBarrelBlockEntity implements IFluidTankProvider {

    public static final int CAPACITY = 768_000;
    public static final int FUEL_TANK_MAX_TRANSFER_RATE = Integer.MAX_VALUE;

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK_BE.get(), pos, state);
    }

    // === HOOK OVERRIDES ===

    @Override
    protected int getMaxTransferRate() {
        return FUEL_TANK_MAX_TRANSFER_RATE;
    }

    @Override
    protected int getTankCapacity() {
        return CAPACITY;
    }

    // === IFluidTankProvider (для делегации через MultiblockPartEntity) ===

    @Override
    public LazyOptional<IFluidHandler> getFluidHandlerCapability() {
        return getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER);
    }

    // === Геттеры для совместимости с FuelTankMenu/GUI ===

    public FluidStack getFluid() { return fluidTank.getFluid(); }
    public int getCapacity() { return CAPACITY; }
    public ContainerData getData() { return data; }
    // Геттер для совместимости со старым кодом (дроп при разрушении блока)
    public ItemStackHandler getInventory() { return itemHandler; }

    // === Tick — цистерна НЕ подвержена коррозии и утечкам ===

    public static void tick(Level level, BlockPos pos, BlockState state, FuelTankBlockEntity be) {
        if (level.isClientSide) return;
        be.processBuckets();
    }

    // === Menu ===

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.trd.fuel_tank_big");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FuelTankMenu(id, inv, this, this.data);
    }
}