package com.trd.block.entity.industrial;

import com.trd.block.entity.ModBlockEntities;
import com.trd.menu.industrial.ConveyorBufferMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConveyorBufferBlockEntity extends BlockEntity implements MenuProvider {

    public enum Mode { EXTRACTOR, INSERTER }

    private final Mode mode;
    private final ItemStackHandler inventory;
    private final LazyOptional<IItemHandler> lazyInventory;

    private int transferCooldown = 0;
    private static final int TRANSFER_INTERVAL = 20;
    private boolean connectedToMachine = false;

    public ConveyorBufferBlockEntity(BlockPos pos, BlockState state, Mode mode) {
        super(ModBlockEntities.CONVEYOR_BUFFER_BE.get(), pos, state);
        this.mode = mode;
        this.inventory = new ItemStackHandler(27) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.lazyInventory = LazyOptional.of(() -> inventory);
    }

    public Mode getMode() { return mode; }
    public IItemHandler getInventory() { return inventory; }
    public boolean isConnectedToMachine() { return connectedToMachine; }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBufferBlockEntity be) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(BlockStateProperties.FACING);
        // === ОБА: станок сзади (back), front — к конвейеру ===
        Direction machineSide = facing.getOpposite();

        boolean wasConnected = be.connectedToMachine;
        be.connectedToMachine = be.findMachine(machineSide) != null;
        if (wasConnected != be.connectedToMachine) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
        }

        if (be.transferCooldown-- > 0) return;

        if (be.mode == Mode.EXTRACTOR) {
            be.tickExtractor(facing);
        } else {
            be.tickInserter(facing);
        }

        be.transferCooldown = TRANSFER_INTERVAL;
    }

    // Извлекатель: станок сзади → буфер → конвейер спереди
    private void tickExtractor(Direction facing) {
        Direction back = facing.getOpposite();

        // 1. Забираем из станка сзади — до 3 из ОДНОГО слота за раз
        IItemHandler machine = findMachine(back);
        if (machine != null) {
            int extracted = 0;
            for (int i = 0; i < machine.getSlots() && extracted == 0; i++) {
                ItemStack sim = machine.extractItem(i, 3, true);
                if (!sim.isEmpty() && ItemHandlerHelper.insertItem(inventory, sim, true).isEmpty()) {
                    ItemStack actual = machine.extractItem(i, sim.getCount(), false);
                    if (!actual.isEmpty()) {
                        ItemStack rem = ItemHandlerHelper.insertItem(inventory, actual, false);
                        extracted = actual.getCount() - rem.getCount();
                    }
                }
            }
            if (extracted > 0) setChanged();
        }

        // 2. Передаём на конвейер спереди — до 3 за раз
        BlockEntity frontBe = level.getBlockEntity(worldPosition.relative(facing));
        if (frontBe instanceof ConveyorBlockEntity conveyor) {
            int pushed = 0;
            int i = 0;
            while (i < inventory.getSlots() && pushed < 3) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack single = stack.split(1);
                    if (conveyor.tryAcceptItem(single)) {
                        pushed++;
                        setChanged();
                        // НЕ инкрементируем i — проверяем тот же слот ещё раз
                    } else {
                        stack.grow(1); // не принял — возвращаем
                        i++; // идём к следующему слоту
                    }
                } else {
                    i++; // пустой слот — следующий
                }
            }
        }
    }

    // Вставщик: конвейер спереди (через tryAcceptItem) → буфер → станок сзади
    private void tickInserter(Direction facing) {
        Direction back = facing.getOpposite();

        // 1. Пушим в станок сзади
        IItemHandler machine = findMachine(back);
        if (machine != null) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack rem = ItemHandlerHelper.insertItem(machine, stack, false);
                    if (rem.getCount() != stack.getCount()) {
                        inventory.setStackInSlot(i, rem);
                        setChanged();
                    }
                }
            }
        }

        // 2. Переполнение — выбросить сверху
        if (isBufferFull()) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack drop = stack.split(1);
                    ejectFromTop(drop);
                    setChanged();
                    break;
                }
            }
        }
    }

    // Вызывается из ConveyorBlockEntity когда конвейер пушит предмет
    public boolean tryAcceptItem(ItemStack stack) {
        ItemStack rem = ItemHandlerHelper.insertItem(inventory, stack, false);
        if (rem.isEmpty()) {
            setChanged();
            return true;
        }
        return false;
    }

    @Nullable
    private IItemHandler findMachine(Direction side) {
        BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
        if (be == null) return null;

        // Сначала пытаемся со стороны (как воронка)
        LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite());
        if (cap.isPresent()) return cap.orElse(null);

        // Fallback: внутренний инвентарь без учёта стороны (для извлекателя сбоку/сверху)
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
    }

    private boolean isBufferFull() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < inventory.getSlotLimit(i)) return false;
        }
        return true;
    }

    private void ejectFromTop(ItemStack stack) {
        if (level == null || stack.isEmpty()) return;
        Vec3 p = Vec3.atCenterOf(worldPosition).add(0, 0.8, 0);
        ItemEntity e = new ItemEntity(level, p.x, p.y, p.z, stack);
        e.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.1, 0.15, (level.random.nextDouble() - 0.5) * 0.1);
        e.setPickUpDelay(10);
        level.addFreshEntity(e);
    }

    public void dropAllContents() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s);
        }
    }

    @Override
    public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyInventory.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyInventory.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(mode == Mode.EXTRACTOR ? "block.trd.conveyor_izvlekatel" : "block.trd.conveyor_vstavshik");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new ConveyorBufferMenu(id, playerInv, this);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putBoolean("Connected", connectedToMachine);
        tag.putInt("Cooldown", transferCooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        connectedToMachine = tag.getBoolean("Connected");
        transferCooldown = tag.getInt("Cooldown");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}