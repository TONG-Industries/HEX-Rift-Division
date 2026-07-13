package com.trd.block.entity.industrial;


import com.trd.block.basic.industrial.ConveyorBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public class ConveyorBlockEntity extends BlockEntity {

    public static final double SPEED = 1.0 / 16.0;
    public static final double ITEM_Y_OFFSET = 8.5 / 16.0;

    private double itemProgress = -0.5;
    private ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    // Клиентские данные для рендера
    public float renderX, renderY, renderZ;
    public float renderRotY;
    public float renderScale = 0.75f; // 12/16 = 0.75

    public ConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVEYOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBlockEntity be) {
        if (level.isClientSide) {
            be.updateRenderPos(state);
            return;
        }

        // Если пусто — ищем предметы сверху
        if (be.itemHandler.getStackInSlot(0).isEmpty()) {
            AABB searchBox = new AABB(pos).inflate(0, 0.3, 0).move(0, 0.2, 0);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox,
                    e -> !e.isRemoved() && e.getDeltaMovement().y == 0);

            for (ItemEntity item : items) {
                double relativeY = item.getY() - pos.getY();
                if (relativeY < 0.2 || relativeY > 1.0) continue;

                // Забираем предмет
                ItemStack stack = item.getItem();
                if (!stack.isEmpty()) {
                    be.itemHandler.insertItem(0, stack.copy(), false);
                    item.discard();
                    be.itemProgress = -0.5;
                    be.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                    break; // Один предмет за раз
                }
            }
            return;
        }

        // Двигаем предмет
        Direction facing = state.getValue(ConveyorBlock.FACING);
        be.itemProgress += SPEED;

        if (be.itemProgress >= 1.0) {
            BlockPos nextPos = pos.relative(facing);
            BlockState nextState = level.getBlockState(nextPos);

            if (nextState.getBlock() instanceof ConveyorBlock &&
                    nextState.getValue(ConveyorBlock.FACING) == facing) {

                BlockEntity nextBe = level.getBlockEntity(nextPos);
                if (nextBe instanceof ConveyorBlockEntity nextConveyor) {
                    if (nextConveyor.itemHandler.getStackInSlot(0).isEmpty()) {
                        ItemStack stack = be.itemHandler.extractItem(0, 64, false);
                        nextConveyor.itemHandler.insertItem(0, stack, false);
                        nextConveyor.itemProgress = -0.5;
                        be.itemProgress = -0.5;
                        be.setChanged();
                        nextConveyor.setChanged();
                        level.sendBlockUpdated(pos, state, state, 3);
                        level.sendBlockUpdated(nextPos, nextState, nextState, 3);
                    }
                }
            } else {
                be.ejectItem(facing);
            }
        } else {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void ejectItem(Direction facing) {
        ItemStack stack = itemHandler.extractItem(0, 64, false);
        if (stack.isEmpty()) return;

        Vec3 ejectPos = Vec3.atCenterOf(worldPosition)
                .add(facing.getStepX() * 0.6, 0.3, facing.getStepZ() * 0.6);

        ItemEntity itemEntity = new ItemEntity(level, ejectPos.x, ejectPos.y, ejectPos.z, stack);
        itemEntity.setDeltaMovement(
                facing.getStepX() * 0.15,
                0.1,
                facing.getStepZ() * 0.15
        );
        itemEntity.setPickUpDelay(10);
        level.addFreshEntity(itemEntity);

        itemProgress = -0.5;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void updateRenderPos(BlockState state) {
        Direction facing = state.getValue(ConveyorBlock.FACING);
        double progress = Math.max(0, Math.min(1, itemProgress + 0.5));

        double offset = (progress - 0.5) * 0.8;
        renderX = (float)(facing.getStepX() * offset);
        renderZ = (float)(facing.getStepZ() * offset);
        renderY = 0.35f;
        renderRotY = facing.toYRot();
    }

    public ItemStack getDisplayedItem() {
        return itemHandler.getStackInSlot(0);
    }

    public boolean hasItem() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Item"));
        itemProgress = tag.getDouble("Progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Item", itemHandler.serializeNBT());
        tag.putDouble("Progress", itemProgress);
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