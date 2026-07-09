package com.trd.block.basic.industrial.fluids;

import com.trd.api.fluids.system.*;
import com.trd.block.entity.industrial.fluids.FluidPipeBlockEntity;
import com.trd.block.entity.industrial.fluids.FluidBarrelBlockEntity;
import com.trd.item.tools.FluidIdentifierItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

public class FluidPipeBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NONE = BooleanProperty.create("none");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final java.util.Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = java.util.Map.of(
            Direction.NORTH, NORTH,
            Direction.SOUTH, SOUTH,
            Direction.EAST, EAST,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN
    );

    private final PipeTier tier;

    private static final VoxelShape CORE = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    private static final VoxelShape UP_SHAPE = Block.box(5.0D, 11.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape DOWN_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 5.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0D, 5.0D, 11.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 5.0D, 5.0D, 5.0D, 11.0D, 11.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(11.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);

    public FluidPipeBlock(PipeTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(NONE, true)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, NONE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NONE)) {
            return Shapes.or(shape, NORTH_SHAPE, SOUTH_SHAPE, EAST_SHAPE, WEST_SHAPE, UP_SHAPE, DOWN_SHAPE);
        }
        boolean n = state.getValue(NORTH), s = state.getValue(SOUTH), e = state.getValue(EAST),
                w = state.getValue(WEST), u = state.getValue(UP), d = state.getValue(DOWN);
        if (n) shape = Shapes.or(shape, NORTH_SHAPE);
        if (s) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (e) shape = Shapes.or(shape, EAST_SHAPE);
        if (w) shape = Shapes.or(shape, WEST_SHAPE);
        if (u) shape = Shapes.or(shape, UP_SHAPE);
        if (d) shape = Shapes.or(shape, DOWN_SHAPE);

        if (n && !s && !(e || w || u || d)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (s && !n && !(e || w || u || d)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (e && !w && !(n || s || u || d)) shape = Shapes.or(shape, WEST_SHAPE);
        if (w && !e && !(n || s || u || d)) shape = Shapes.or(shape, EAST_SHAPE);
        if (u && !d && !(n || s || e || w)) shape = Shapes.or(shape, DOWN_SHAPE);
        if (d && !u && !(n || s || e || w)) shape = Shapes.or(shape, UP_SHAPE);
        return shape;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(this)) {
            FluidNetworkManager.get((ServerLevel) level).addNode(pos);
            level.scheduleTick(pos, this, 2);
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            FluidNetworkManager.get((ServerLevel) level).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public PipeTier getTier() { return this.tier; }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.literal("Макс. температура: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(tier.getMaxTemperature() + " °C").withStyle(ChatFormatting.GOLD)));

        tooltip.add(Component.literal("Макс. коррозия: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(tier.getMaxCorrosivity())).withStyle(ChatFormatting.YELLOW)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && stack.getItem() instanceof FluidIdentifierItem) {
            ServerLevel serverLevel = (ServerLevel) level;
            String selectedFluidId = FluidIdentifierItem.getSelectedFluid(stack);
            Fluid fluidToSet = Fluids.EMPTY;
            if (!selectedFluidId.equals("none")) {
                fluidToSet = ForgeRegistries.FLUIDS.getValue(new net.minecraft.resources.ResourceLocation(selectedFluidId));
                if (fluidToSet == null) fluidToSet = Fluids.EMPTY;
            }

            if (player.isCrouching()) {
                BlockEntity startBe = level.getBlockEntity(pos);
                if (!(startBe instanceof FluidPipeBlockEntity startPipe)) return InteractionResult.sidedSuccess(level.isClientSide);
                Fluid originalFilter = startPipe.getFilterFluid();

                java.util.Set<BlockPos> positionsToUpdate = new java.util.HashSet<>();
                java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
                
                queue.add(pos);
                positionsToUpdate.add(pos);
                
                while (!queue.isEmpty()) {
                    BlockPos currentPos = queue.poll();
                    for (Direction dir : Direction.values()) {
                        BlockPos neighborPos = currentPos.relative(dir);
                        if (!positionsToUpdate.contains(neighborPos) && serverLevel.isLoaded(neighborPos)) {
                            BlockEntity neighborBE = serverLevel.getBlockEntity(neighborPos);
                            if (neighborBE instanceof FluidPipeBlockEntity neighborPipe) {
                                if (neighborPipe.getFilterFluid() == originalFilter) {
                                    queue.add(neighborPos);
                                    positionsToUpdate.add(neighborPos);
                                }
                            }
                        }
                    }
                }

                FluidNetworkManager manager = FluidNetworkManager.get(serverLevel);
                for (BlockPos nodePos : positionsToUpdate) {
                    com.trd.api.fluids.system.FluidNetwork net = manager.getNetwork(nodePos);
                    if (net != null) {
                        net.invalidateFluidCache();
                    }
                }
                for (BlockPos nodePos : positionsToUpdate) manager.removeNode(nodePos);

                int updateCount = 0;
                for (BlockPos nodePos : positionsToUpdate) {
                    if (serverLevel.isLoaded(nodePos)) {
                        BlockEntity nodeBe = serverLevel.getBlockEntity(nodePos);
                        if (nodeBe instanceof FluidPipeBlockEntity pipeBE) {
                            pipeBE.setFilterFluid(fluidToSet); 
                            updateCount++;
                        }
                        manager.addNode(nodePos);
                        serverLevel.getBlockState(nodePos).updateNeighbourShapes(serverLevel, nodePos, 3);
                    }
                }
                
                boolean isResetting = selectedFluidId.equals("none");
                level.playSound(null, pos, isResetting ? SoundEvents.ENDERMAN_TELEPORT : SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, isResetting ? 0.8F : 1.2F);
                String fluidName = isResetting ? "EMPTY" : Component.translatable(fluidToSet.getFluidType().getDescriptionId()).getString();
                player.displayClientMessage(Component.literal("§a" + (isResetting ? "Pipe line filter reset." : "Pipe line filter set: " + fluidName) + " §7(" + updateCount + " pipes)"), true);
            } else {
                if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
                    FluidNetworkManager manager = FluidNetworkManager.get(serverLevel);
                    com.trd.api.fluids.system.FluidNetwork net = manager.getNetwork(pos);
                    if (net != null) {
                        net.invalidateFluidCache();
                    }
                    manager.removeNode(pos);
                    be.setFilterFluid(fluidToSet);
                    manager.addNode(pos);
                    level.setBlock(pos, this.updateConnections(level, pos, state), 3);
                    if (selectedFluidId.equals("none")) {
                        player.displayClientMessage(Component.literal("§eFilter reset (Pipe accepts all)"), true);
                        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.8F);
                    } else {
                        String fluidName = Component.translatable(fluidToSet.getFluidType().getDescriptionId()).getString();
                        player.displayClientMessage(Component.literal("§aFilter: §f" + fluidName), true);
                        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    private BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        boolean n = canConnectTo(level, pos, pos.north(), Direction.NORTH);
        boolean s = canConnectTo(level, pos, pos.south(), Direction.SOUTH);
        boolean e = canConnectTo(level, pos, pos.east(), Direction.EAST);
        boolean w = canConnectTo(level, pos, pos.west(), Direction.WEST);
        boolean u = canConnectTo(level, pos, pos.above(), Direction.UP);
        boolean d = canConnectTo(level, pos, pos.below(), Direction.DOWN);
        boolean none = !(n || s || e || w || u || d);
        return state.setValue(NORTH, n).setValue(SOUTH, s).setValue(EAST, e).setValue(WEST, w).setValue(UP, u).setValue(DOWN, d).setValue(NONE, none);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (checkExternalMeltdown(level, pos)) return;
    }

    private boolean checkExternalMeltdown(ServerLevel level, BlockPos pos) {
        com.trd.api.fluids.system.PipeTier tier = this.getTier();
        java.util.List<BlockPos> positionsToCheck = new java.util.ArrayList<>();
        positionsToCheck.add(pos);
        for (Direction dir : Direction.values()) positionsToCheck.add(pos.relative(dir));

        for (BlockPos checkPos : positionsToCheck) {
            net.minecraft.world.level.material.FluidState fluidState = level.getFluidState(checkPos);
            if (!fluidState.isEmpty()) {
                Fluid fluid = fluidState.getType();
                int tempC = getFluidTemperatureCelsius(fluid);
                int corr = 0;
                if (fluid.getFluidType() instanceof BaseFluidType base) corr = base.getCorrosivity();
                else if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) tempC = 1000;

                if (tempC > tier.getMaxTemperature() || corr > tier.getMaxCorrosivity()) {
                    level.destroyBlock(pos, false);
                    BlockState fluidBlock = fluidState.createLegacyBlock();
                    if (!fluidBlock.isAir()) level.setBlock(pos, fluidBlock, 3);
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    return true;
                }
            }
        }
        return false;
    }

    private static int getFluidTemperatureCelsius(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) return 20;
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) return 1000;
        if (fluid.getFluidType() instanceof BaseFluidType base) return base.getDisplayTemperature();
        return fluid.getFluidType().getTemperature() - 273;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (!level.isClientSide()) level.scheduleTick(currentPos, this, 2);
        return updateConnections(level, currentPos, state);
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos myPos, BlockPos neighborPos, Direction dir) {
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof FluidPipeBlock) {
            if (level.getBlockEntity(myPos) instanceof FluidPipeBlockEntity myBE &&
                    level.getBlockEntity(neighborPos) instanceof FluidPipeBlockEntity neighborBE) {
                return myBE.getFilterFluid() == neighborBE.getFilterFluid();
            }
        }
        BlockEntity neighborBE = level.getBlockEntity(neighborPos);
        if (neighborBE != null) return neighborBE.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).isPresent();
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(WATERLOGGED)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity pipeBE && pipeBE.hasFlowed()) {
            Fluid fluid = pipeBE.getFilterFluid();
            if (fluid != Fluids.EMPTY && getFluidTemperatureCelsius(fluid) > 100) {
                if (random.nextInt(3) == 0) {
                    double d0 = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D;
                    double d1 = pos.getY() + 0.6D + (random.nextDouble() * 0.4D);
                    double d2 = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP, d0, d1, d2, 0.0D, 0.05D, 0.0D);
                }
            }
        }
    }
}