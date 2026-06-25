package com.trd.menu;

import com.trd.block.basic.industrial.rotation.MotorElectroBlock;
import com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MotorElectroMenu extends AbstractContainerMenu {

    public final MotorElectroBlockEntity blockEntity;
    private final ContainerData data;

    // ===================== СЕРВЕРНЫЙ КОНСТРУКТОР =====================
    public MotorElectroMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MOTOR_ELECTRO_MENU.get(), containerId);

        if (!(entity instanceof MotorElectroBlockEntity)) {
            throw new IllegalArgumentException("Expected MotorElectroBlockEntity, got: " + entity);
        }
        this.blockEntity = (MotorElectroBlockEntity) entity;
        this.data = data;

        // Регистрируем 4 слота ContainerData (targetRpm, energy/10, JE/s, torque)
        addDataSlots(data);
    }

    // ===================== КЛИЕНТСКИЙ КОНСТРУКТОР (через FriendlyByteBuf) =====================
    public MotorElectroMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv,
                inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(4));
    }

    // ===================== ГЕТТЕРЫ ДЛЯ ЭКРАНА =====================

    /** Текущий targetRpm (100–1000) */
    public int getTargetRpm() {
        return data.get(0);
    }

    /** Текущая энергия (реальное значение = data[1] * 10) */
    public long getEnergyStored() {
        return (long) data.get(1) * 10;
    }

    /** Максимальная ёмкость */
    public long getMaxEnergy() {
        return MotorElectroBlockEntity.MAX_ENERGY;
    }

    /** Потребление в JE/сек */
    public int getConsumptionPerSecond() {
        return data.get(2);
    }

    /** Крутящий момент в Нм */
    public int getTorqueNm() {
        return data.get(3);
    }

    // ===================== СТАНДАРТНОЕ =====================

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos()
        ).evaluate((level, pos) -> {
            if (!(level.getBlockState(pos).getBlock() instanceof MotorElectroBlock)) return false;
            return player.distanceToSqr(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // нет слотов предметов
    }
}
