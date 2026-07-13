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

    // ------------------------------------------------------------
    //  Тик
    // ------------------------------------------------------------
    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBufferBlockEntity be) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(BlockStateProperties.FACING);
        Direction machineSide = (be.mode == Mode.EXTRACTOR) ? facing.getOpposite() : facing;

        // Обновляем светодиод
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

        // 1. Забираем из станка (до 3 шт за раз)
        IItemHandler machine = findMachine(back);
        if (machine != null) {
            int extracted = 0;
            for (int i = 0; i < machine.getSlots() && extracted < 3; i++) {
                ItemStack sim = machine.extractItem(i, 1, true);
                if (!sim.isEmpty() && ItemHandlerHelper.insertItem(inventory, sim, true).isEmpty()) {
                    machine.extractItem(i, 1, false);
                    ItemHandlerHelper.insertItem(inventory, sim, false);
                    extracted++;
                }
            }
            if (extracted > 0) setChanged();
        }

        // 2. Передаём на конвейер спереди
        BlockEntity frontBe = level.getBlockEntity(worldPosition.relative(facing));
        if (frontBe instanceof ConveyorBlockEntity conveyor) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack single = stack.split(1);
                    if (conveyor.tryAcceptItem(single)) {
                        setChanged();
                        break; // 1 за тик
                    } else {
                        stack.grow(1); // не принял — возвращаем
                    }
                }
            }
        }
    }

    // Вставщик: конвейер сзади → буфер → станок спереди
    private void tickInserter(Direction facing) {
        Direction back = facing.getOpposite();

        // 1. Принимаем от конвейера сзади
        BlockEntity backBe = level.getBlockEntity(worldPosition.relative(back));
        if (backBe instanceof ConveyorBlockEntity conveyor) {
            // Конвейер сам пушит предметы через tryAcceptItem,
            // но если он не может — мы ничего не делаем здесь.
            // Однако можно добавить активное вытягивание, если нужно.
        }

        // 2. Пушим в станок спереди
        IItemHandler machine = findMachine(facing);
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

        // 3. Переполнение — выбросить сверху
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

    // ------------------------------------------------------------
    //  Приём от конвейера (вызывается из ConveyorBlockEntity.tick)
    // ------------------------------------------------------------
    public boolean tryAcceptItem(ItemStack stack) {
        ItemStack rem = ItemHandlerHelper.insertItem(inventory, stack, false);
        if (rem.isEmpty()) {
            setChanged();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------
    //  Утилиты
    // ------------------------------------------------------------
    @Nullable
    private IItemHandler findMachine(Direction side) {
        BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
        if (be == null) return null;
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
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

    // ------------------------------------------------------------
    //  Capabilities
    // ------------------------------------------------------------
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

    // ------------------------------------------------------------
    //  GUI
    // ------------------------------------------------------------
    @Override
    public Component getDisplayName() {
        return Component.translatable(mode == Mode.EXTRACTOR ? "block.trd.conveyor_izvlekatel" : "block.trd.conveyor_vstavshik");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new ConveyorBufferMenu(id, playerInv, this);
    }

    // ------------------------------------------------------------
    //  NBT
    // ------------------------------------------------------------
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