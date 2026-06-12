package com.trd.block.basic.industrial.rotation;

import com.trd.api.energy.EnergyNetworkManager;
import com.trd.api.rotation.KineticNetworkManager;
import com.trd.block.entity.industrial.rotation.StatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import com.trd.block.basic.ModBlocks;
import com.trd.multiblock.system.IMultiblockController;
import com.trd.multiblock.system.MultiblockStructureHelper;
import com.trd.multiblock.system.PartRole;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import com.trd.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class StatorBlock extends BaseEntityBlock implements IMultiblockController {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public StatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AXIS, Direction.Axis.Z));
    }

    private static MultiblockStructureHelper helper;

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AXIS);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (helper == null) {
            Map<Character, Supplier<BlockState>> symbols = Map.of(
                    '#', () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState(),
                    'O', () -> this.defaultBlockState()
            );
            Map<Character, PartRole> roles = Map.of(
                    '#', PartRole.DEFAULT,
                    'O', PartRole.CONTROLLER
            );
            helper = MultiblockStructureHelper.createFromLayersWithRoles(
                    new String[][]{
                            {"#O#"},
                            {"# #"},
                            {"###"}
                    },
                    symbols,
                    () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState(),
                    roles
            );
        }
        return helper;
    }

    @Override
    public PartRole getPartRole(BlockPos pos) {
        return PartRole.DEFAULT;
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        Direction facing = state.getValue(FACING);
        Direction.Axis axis = state.getValue(AXIS);
        return getStructureHelper().generateStatorShape(facing, axis);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction.Axis axis = state.getValue(AXIS);
            getStructureHelper().placeStructureStator(level, pos, facing, axis, this);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        Direction.Axis axis;

        if (facing.getAxis() == Direction.Axis.Y) {
            axis = context.getHorizontalDirection().getAxis();
        } else {
            axis = Direction.Axis.Y;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState target = context.getLevel().getBlockState(clickedPos.relative(facing));

        if (target.getBlock() instanceof ShaftBlock) {
            axis = target.getValue(ShaftBlock.FACING).getAxis();
        }

        return this.defaultBlockState().setValue(FACING, facing).setValue(AXIS, axis);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && state.getBlock() != oldState.getBlock()) {
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterPlace(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterRemove(pos);
            
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                });
            }

            Direction facing = state.getValue(FACING);
            Direction.Axis axis = state.getValue(AXIS);
            getStructureHelper().destroyStructureStator(level, pos, facing, axis);

            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StatorBlockEntity)) return InteractionResult.PASS;

        ItemStack stackInHand = player.getItemInHand(hand);
        boolean isCoil = stackInHand.getItem() == ModItems.COPPER_COIL.get() || stackInHand.getItem() == ModItems.WIRE_COIL.get();

        // Calculate slot based on hit location
        // The stator is a 3x3x1 multiblock, but this block is the center.
        // We calculate angle from the center of the block.
        net.minecraft.world.phys.Vec3 hitVec = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Direction.Axis axis = state.getValue(AXIS);

        double u = 0, v = 0;
        if (axis == Direction.Axis.X) {
            u = hitVec.y; v = hitVec.z;
        } else if (axis == Direction.Axis.Y) {
            u = hitVec.x; v = hitVec.z;
        } else { // Z
            u = hitVec.x; v = hitVec.y;
        }

        // Calculate angle and slot index
        double angle = Math.toDegrees(Math.atan2(v, u));
        if (angle < 0) angle += 360;
        int slot = (int) Math.round(angle / 30.0) % 12;

        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null) return InteractionResult.PASS;

        if (isCoil) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack toInsert = stackInHand.copy();
                    toInsert.setCount(1);
                    handler.insertItem(slot, toInsert, false);
                    if (!player.isCreative()) {
                        stackInHand.shrink(1);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        } else if (stackInHand.isEmpty() && player.isCrouching()) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (!inSlot.isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack extracted = handler.extractItem(slot, 1, false);
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, com.trd.block.entity.ModBlockEntities.STATOR_BE.get(), StatorBlockEntity.createTicker());
    }
}
