package com.trd.block.entity.industrial;

import com.trd.block.basic.industrial.ConveyorBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConveyorBlockEntity extends BlockEntity {

    public static final double SPEED = 1.5 / 20.0;
    public static final double ITEM_Y_OFFSET = 2 / 16.0;

    private static final int MAX_ITEMS = 64;

    private final List<ConveyorItem> items = new ArrayList<>();
    private final List<ConveyorItem> clientItems = new ArrayList<>();
    private final List<ConveyorItem> prevClientItems = new ArrayList<>();

    public ConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVEYOR_BE.get(), pos, state);
    }

    public boolean tryAcceptItem(ItemStack stack) {
        if (items.size() >= MAX_ITEMS) return false;
        items.add(new ConveyorItem(stack.copy(), 0.0));
        syncClient();
        return true;
    }

    public List<ConveyorItem> getClientItems() {
        return clientItems;
    }

    public List<ConveyorItem> getPrevClientItems() {
        return prevClientItems;
    }

    private void syncClient() {
        clientItems.clear();
        prevClientItems.clear();
        for (ConveyorItem ci : items) {
            clientItems.add(new ConveyorItem(ci.stack, ci.progress));
            prevClientItems.add(new ConveyorItem(ci.stack, ci.progress));
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBlockEntity be) {
        if (level.isClientSide) {
            be.tickClient();
            return;
        }

        AABB box = new AABB(pos).inflate(0.1, 0.3, 0.1).move(0, 0.2, 0);
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> !e.isRemoved() && e.getDeltaMovement().y <= 0.01);
        List<ConveyorItem> newItems = new ArrayList<>();
        for (ItemEntity item : entities) {
            ItemStack stack = item.getItem();
            if (!stack.isEmpty()) {
                newItems.add(new ConveyorItem(stack));
                item.discard();
            }
        }

        Direction facing = state.getValue(ConveyorBlock.FACING);
        Iterator<ConveyorItem> iterator = be.items.iterator();
        while (iterator.hasNext()) {
            ConveyorItem ci = iterator.next();
            ci.progress += SPEED;

            if (ci.progress >= 1.0) {
                BlockPos nextPos = pos.relative(facing);
                BlockState nextState = level.getBlockState(nextPos);
                BlockEntity nextBe = level.getBlockEntity(nextPos);

                // === ВСТАВЩИК ===
                // Принимаем ТОЛЬКО если вставщик смотрит на нас своей передней (FACING) стороной
                if (nextBe instanceof ConveyorBufferBlockEntity buffer && buffer.getMode() == ConveyorBufferBlockEntity.Mode.INSERTER) {
                    Direction bufferFacing = nextState.getValue(BlockStateProperties.FACING);
                    if (bufferFacing == facing.getOpposite()) {
                        if (buffer.tryAcceptItem(ci.stack.copy())) {
                            iterator.remove();
                            be.syncClient();
                            continue;
                        } else {
                            ci.progress = 1.0 - 0.001; // задерживаем, буфер полон
                            continue;
                        }
                    }
                }

                if (nextState.getBlock() instanceof ConveyorBlock &&
                        nextState.getValue(ConveyorBlock.FACING) == facing) {
                    if (nextBe instanceof ConveyorBlockEntity nextConveyor) {
                        ConveyorItem transferred = new ConveyorItem(ci.stack);
                        transferred.progress = 0.0;
                        nextConveyor.items.add(transferred);
                        nextConveyor.syncClient();
                        iterator.remove();
                        be.syncClient();
                        continue;
                    }
                }
                be.ejectItem(level, facing, ci.stack);
                iterator.remove();
                be.syncClient();
            }
        }

        if (!newItems.isEmpty()) {
            be.items.addAll(newItems);
            be.syncClient();
        }
    }

    private void tickClient() {
        prevClientItems.clear();
        for (ConveyorItem ci : clientItems) {
            prevClientItems.add(new ConveyorItem(ci.stack, ci.progress));
        }

        for (ConveyorItem ci : clientItems) {
            ci.progress += SPEED;
            if (ci.progress > 1.0) ci.progress = 1.0;
        }
    }

    private void ejectItem(Level level, Direction facing, ItemStack stack) {
        if (stack.isEmpty()) return;

        Vec3 ejectPos = Vec3.atCenterOf(worldPosition)
                .add(facing.getStepX() * 0.55, 0.35, facing.getStepZ() * 0.55);

        ItemEntity itemEntity = new ItemEntity(level, ejectPos.x, ejectPos.y, ejectPos.z, stack);
        itemEntity.setDeltaMovement(
                facing.getStepX() * 0.12,
                0.08,
                facing.getStepZ() * 0.12
        );
        itemEntity.setPickUpDelay(15);
        level.addFreshEntity(itemEntity);
    }

    public ItemStack popItem() {
        if (items.isEmpty()) return ItemStack.EMPTY;
        ConveyorItem ci = items.remove(items.size() - 1);
        syncClient();
        return ci.stack;
    }

    public void dropAllItems(Level level, BlockPos pos) {
        if (items.isEmpty()) return;
        Direction facing = getBlockState().getValue(ConveyorBlock.FACING);
        for (ConveyorItem ci : items) {
            ejectItem(level, facing, ci.stack);
        }
        items.clear();
        syncClient();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (ConveyorItem ci : items) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.put("Stack", ci.stack.save(new CompoundTag()));
            itemTag.putDouble("Progress", ci.progress);
            list.add(itemTag);
        }
        tag.put("Items", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ListTag list = tag.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            ItemStack stack = ItemStack.of(itemTag.getCompound("Stack"));
            double progress = itemTag.getDouble("Progress");
            ConveyorItem ci = new ConveyorItem(stack, progress);
            items.add(ci);
        }
        syncClient();
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

    public static class ConveyorItem {
        public ItemStack stack;
        public double progress;

        public ConveyorItem(ItemStack stack) {
            this.stack = stack.copy();
            this.progress = 0.0;
        }

        public ConveyorItem(ItemStack stack, double progress) {
            this.stack = stack.copy();
            this.progress = progress;
        }
    }
}