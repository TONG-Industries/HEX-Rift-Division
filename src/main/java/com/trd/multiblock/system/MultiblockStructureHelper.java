package com.trd.multiblock.system;

import com.trd.api.energy.EnergyNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

public class MultiblockStructureHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiblockStructureHelper.class);
    private static final ThreadLocal<Boolean> IS_DESTROYING = ThreadLocal.withInitial(() -> false);

    private final Map<BlockPos, Supplier<BlockState>> structureMap;
    private final Supplier<BlockState> phantomBlockState;
    private final Map<Character, PartRole> symbolRoleMap;
    private final Map<BlockPos, Character> positionSymbolMap;
    private final Map<Direction, VoxelShape> shapeCache = new HashMap<>();
    private final Map<BlockPos, Set<Direction>> ladderLocalDirections = new HashMap<>();
    private final Map<BlockPos, VoxelShape> partShapes;
    private final Map<BlockPos, VoxelShape> collisionShapes;
    private final BlockPos controllerOffset;
    private final Set<BlockPos> partOffsets;

    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap,
                                     Supplier<BlockState> phantomBlockState,
                                     Map<Character, PartRole> symbolRoleMap,
                                     Map<BlockPos, Character> positionSymbolMap,
                                     Map<BlockPos, VoxelShape> partShapes,
                                     Map<BlockPos, VoxelShape> collisionShapes,
                                     BlockPos controllerOffset) {
        this.structureMap = Collections.unmodifiableMap(structureMap);
        this.phantomBlockState = phantomBlockState;
        this.symbolRoleMap = symbolRoleMap != null ? Collections.unmodifiableMap(symbolRoleMap) : Collections.emptyMap();
        this.positionSymbolMap = positionSymbolMap != null ? Collections.unmodifiableMap(positionSymbolMap) : Collections.emptyMap();
        this.controllerOffset = controllerOffset != null ? controllerOffset : BlockPos.ZERO;
        this.partShapes = partShapes != null ? Collections.unmodifiableMap(partShapes) : Collections.emptyMap();
        this.collisionShapes = collisionShapes != null ? Collections.unmodifiableMap(collisionShapes) : Collections.emptyMap();
        this.partOffsets = Collections.unmodifiableSet(structureMap.keySet());
    }

    public static MultiblockStructureHelper createFromLayersWithRoles(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap,
            Map<Character, VoxelShape> shapeMap,
            Map<Character, VoxelShape> collisionMap ) {

        Map<BlockPos, Supplier<BlockState>> structureMap = new HashMap<>();
        Map<BlockPos, Character> positionSymbolMap = new HashMap<>();
        Map<BlockPos, Set<Direction>> ladderDirs = new HashMap<>();
        Map<BlockPos, VoxelShape> specificPartShapes = new HashMap<>();
        Map<BlockPos, VoxelShape> specificCollisionShapes = new HashMap<>();
        List<BlockPos> foundControllerPositions = new ArrayList<>();

        if (roleMap == null || roleMap.isEmpty()) throw new IllegalArgumentException("roleMap cannot be null or empty");
        if (!roleMap.containsValue(PartRole.CONTROLLER)) throw new IllegalArgumentException("roleMap must contain a symbol with PartRole.CONTROLLER");
        if (layers == null || layers.length == 0) throw new IllegalArgumentException("layers cannot be null or empty");

        int maxDepth = 0, maxWidth = 0;
        for (String[] layer : layers) {
            if (layer == null) continue;
            maxDepth = Math.max(maxDepth, layer.length);
            for (String row : layer) {
                int actualWidth = 0;
                for (char c : row.toCharArray()) {
                    if (!isClimbPrefix(c)) actualWidth++;
                }
                maxWidth = Math.max(maxWidth, actualWidth);
            }
        }

        int centerX = (maxWidth - 1) / 2;
        int centerZ = (maxDepth - 1) / 2;

        for (int y = 0; y < layers.length; y++) {
            String[] layer = layers[y];
            if (layer == null) continue;
            for (int z = 0; z < layer.length; z++) {
                String row = layer[z];
                if (row == null) continue;

                int gridX = 0;
                EnumSet<Direction> currentPrefixes = EnumSet.noneOf(Direction.class);

                for (int strIdx = 0; strIdx < row.length(); strIdx++) {
                    char symbol = row.charAt(strIdx);

                    if (symbol == '<') { currentPrefixes.add(Direction.WEST); continue; }
                    if (symbol == '>') { currentPrefixes.add(Direction.EAST); continue; }
                    if (symbol == '!') { currentPrefixes.add(Direction.NORTH); continue; }
                    if (symbol == '?') { currentPrefixes.add(Direction.SOUTH); continue; }

                    PartRole role = roleMap.get(symbol);
                    int relX = gridX - centerX;
                    int relZ = z - centerZ;
                    BlockPos pos = new BlockPos(relX, y, relZ);

                    if (role != null) {
                        if (role == PartRole.CONTROLLER) {
                            foundControllerPositions.add(pos);
                        } else {
                            structureMap.put(pos, symbolMap.get(symbol));
                            if (role == PartRole.LADDER && !currentPrefixes.isEmpty()) {
                                ladderDirs.put(pos, EnumSet.copyOf(currentPrefixes));
                            }
                        }
                        positionSymbolMap.put(pos, symbol);

                        if (shapeMap != null && shapeMap.containsKey(symbol)) {
                            specificPartShapes.put(pos, shapeMap.get(symbol));
                            if (collisionMap != null && collisionMap.containsKey(symbol)) {
                                specificCollisionShapes.put(pos, collisionMap.get(symbol));
                            } else {
                                specificCollisionShapes.put(pos, Shapes.empty());
                            }
                        }
                    }
                    gridX++;
                    currentPrefixes = EnumSet.noneOf(Direction.class);
                }
            }
        }

        if (foundControllerPositions.isEmpty() || foundControllerPositions.size() > 1) {
            throw new IllegalArgumentException("Structure must contain exactly ONE controller!");
        }

        BlockPos finalControllerPos = foundControllerPositions.get(0);

        MultiblockStructureHelper helper = new MultiblockStructureHelper(
                structureMap, phantomBlockState, roleMap, positionSymbolMap,
                specificPartShapes, specificCollisionShapes, finalControllerPos
        );
        helper.ladderLocalDirections.putAll(ladderDirs);
        return helper;
    }

    public static MultiblockStructureHelper createFromLayersWithRoles(
            String[][] layers, Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState, Map<Character, PartRole> roleMap) {
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, null, null);
    }

    public BlockPos getControllerOffset() { return this.controllerOffset; }

    public VoxelShape getSpecificPartShape(BlockPos gridPos, Direction facing) {
        if (partShapes.containsKey(gridPos)) return rotateShape(partShapes.get(gridPos), facing);
        return Shapes.block();
    }

    public VoxelShape getSpecificCollisionShape(BlockPos gridPos, Direction facing) {
        if (collisionShapes.containsKey(gridPos)) return rotateShape(collisionShapes.get(gridPos), facing);
        return Shapes.block();
    }

    public boolean isFullBlock(BlockPos gridPos, Direction facing) {
        VoxelShape shape = getSpecificCollisionShape(gridPos, facing);
        if (shape.isEmpty()) return false;
        AABB bounds = shape.bounds();
        return bounds.minX <= 0.001 && bounds.minY <= 0.001 && bounds.minZ <= 0.001 &&
                bounds.maxX >= 0.999 && bounds.maxY >= 0.999 && bounds.maxZ >= 0.999;
    }

    public PartRole resolvePartRole(BlockPos gridPos, IMultiblockController controller) {
        Character symbol = positionSymbolMap.get(gridPos);
        if (symbol != null && symbolRoleMap.containsKey(symbol)) return symbolRoleMap.get(symbol);
        return controller.getPartRole(gridPos);
    }

    private final Set<Block> replaceableBlocks = Set.of(
            Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.SNOW, Blocks.VINE, Blocks.WATER, Blocks.LAVA
    );

    private boolean isBlockReplaceable(BlockState state) {
        if (replaceableBlocks.contains(state.getBlock())) return true;
        return state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS);
    }

    public boolean checkPlacement(Level level, BlockPos controllerPos, Direction facing, Player player) {
        List<BlockPos> obstructions = new ArrayList<>();
        for (BlockPos relativePos : structureMap.keySet()) {
            if (relativePos.equals(BlockPos.ZERO)) continue;
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState existingState = level.getBlockState(worldPos);

            if (!isBlockReplaceable(existingState)) {
                obstructions.add(worldPos);
            } else if (level instanceof ServerLevel serverLevel && EnergyNetworkManager.get(serverLevel).isBlockObstructingAnyWire(worldPos)) {
                // Также проверяем, не проходит ли сквозь это место провод
                obstructions.add(worldPos);
            }
        }

        if (!obstructions.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.literal("§cCannot place multiblock here! Area is obstructed."), true);
            }
            // TODO: Вы можете добавить пакет подсветки красным цветом (HighlightBlocksPacket) здесь
            return false;
        }

        return true;
    }

    public boolean checkPlacement(Level level, BlockPos controllerPos, Direction.Axis axis, Player player) {
        List<BlockPos> obstructions = new ArrayList<>();
        for (BlockPos relativePos : structureMap.keySet()) {
            if (relativePos.equals(BlockPos.ZERO)) continue;
            BlockPos worldPos = getRotatedPosAxis(controllerPos, relativePos, axis);
            BlockState existingState = level.getBlockState(worldPos);

            if (!isBlockReplaceable(existingState)) {
                obstructions.add(worldPos);
            } else if (level instanceof ServerLevel serverLevel && EnergyNetworkManager.get(serverLevel).isBlockObstructingAnyWire(worldPos)) {
                obstructions.add(worldPos);
            }
        }

        if (!obstructions.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.literal("§cCannot place multiblock here! Area is obstructed."), true);
            }
            return false;
        }

        return true;
    }

    public boolean checkPlacementStator(Level level, BlockPos controllerPos, Direction facing, Direction.Axis axis, Player player) {
        List<BlockPos> obstructions = new ArrayList<>();
        for (BlockPos relativePos : structureMap.keySet()) {
            if (relativePos.equals(BlockPos.ZERO)) continue;
            BlockPos worldPos = getRotatedStatorPos(controllerPos, relativePos, facing, axis);
            BlockState existingState = level.getBlockState(worldPos);

            if (!isBlockReplaceable(existingState)) {
                obstructions.add(worldPos);
            } else if (level instanceof ServerLevel serverLevel && EnergyNetworkManager.get(serverLevel).isBlockObstructingAnyWire(worldPos)) {
                obstructions.add(worldPos);
            }
        }

        if (!obstructions.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.literal("§cCannot place multiblock here! Area is obstructed."), true);
            }
            return false;
        }

        return true;
    }

    private static boolean isClimbPrefix(char c) {
        return c == '<' || c == '>' || c == '!' || c == '?';
    }

    public Set<BlockPos> getPartOffsets() { return this.partOffsets; }

    public synchronized void placeStructure(Level level, BlockPos controllerPos, Direction.Axis axis, IMultiblockController controller) {
        if (level.isClientSide) return;

        List<BlockPos> allPlacedPositions = new ArrayList<>();

        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos gridPos = entry.getKey();
            BlockPos worldPos = getRotatedPosAxis(controllerPos, gridPos, axis);

            if (worldPos.equals(controllerPos)) continue;

            BlockState partState = phantomBlockState.get();
            if (partState.hasProperty(BlockStateProperties.AXIS)) {
                partState = partState.setValue(BlockStateProperties.AXIS, axis);
            }
            level.setBlock(worldPos, partState, 3);
            allPlacedPositions.add(worldPos);

            BlockEntity be = level.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(controllerPos);
                PartRole role = resolvePartRole(gridPos, controller);
                partBe.setPartRole(role);
            }
        }

        LOGGER.info("Multiblock placed at {} with {} parts (Axis {}).", controllerPos, allPlacedPositions.size(), axis);
        updateFrameForController(level, controllerPos);
    }

    public synchronized void placeStructure(Level level, BlockPos controllerPos, Direction facing, IMultiblockController controller) {
        if (level.isClientSide) return;

        List<BlockPos> allPlacedPositions = new ArrayList<>();

        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos gridPos = entry.getKey();
            BlockPos worldPos = getRotatedPos(controllerPos, gridPos, facing);

            if (worldPos.equals(controllerPos)) continue;

            BlockState partState = phantomBlockState.get();
            if (partState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                partState = partState.setValue(HorizontalDirectionalBlock.FACING, facing);
            }
            level.setBlock(worldPos, partState, 3);
            allPlacedPositions.add(worldPos);

            BlockEntity be = level.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(controllerPos);
                PartRole role = resolvePartRole(gridPos, controller);
                partBe.setPartRole(role);

                if (role.isLadder()) {
                    Set<Direction> localSides = ladderLocalDirections.get(gridPos);
                    EnumSet<Direction> worldSides = EnumSet.noneOf(Direction.class);

                    if (localSides != null && !localSides.isEmpty()) {
                        for (Direction localDir : localSides) {
                            BlockPos rotatedVec = rotate(new BlockPos(localDir.getNormal()), facing);
                            worldSides.add(Direction.getNearest(rotatedVec.getX(), rotatedVec.getY(), rotatedVec.getZ()));
                        }
                    } else {
                        worldSides.add(Direction.NORTH); worldSides.add(Direction.SOUTH);
                        worldSides.add(Direction.EAST); worldSides.add(Direction.WEST);
                    }
                    partBe.setAllowedClimbSides(worldSides);
                }
            }
        }

        LOGGER.info("Multiblock placed at {} with {} parts.", controllerPos, allPlacedPositions.size());
        updateFrameForController(level, controllerPos);
    }

    public synchronized void placeStructureStator(Level level, BlockPos controllerPos, Direction facing, Direction.Axis axis, IMultiblockController controller) {
        if (level.isClientSide) return;

        List<BlockPos> allPlacedPositions = new ArrayList<>();

        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos gridPos = entry.getKey();
            BlockPos worldPos = getRotatedStatorPos(controllerPos, gridPos, facing, axis);

            if (worldPos.equals(controllerPos)) continue;

            BlockState partState = phantomBlockState.get();
            if (partState.hasProperty(BlockStateProperties.FACING)) {
                partState = partState.setValue(BlockStateProperties.FACING, facing);
            }
            if (partState.hasProperty(BlockStateProperties.AXIS)) {
                partState = partState.setValue(BlockStateProperties.AXIS, axis);
            }
            level.setBlock(worldPos, partState, 3);
            allPlacedPositions.add(worldPos);

            BlockEntity be = level.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(controllerPos);
                PartRole role = resolvePartRole(gridPos, controller);
                partBe.setPartRole(role);
            }
        }

        LOGGER.info("Stator Multiblock placed at {} with {} parts (Facing {}, Axis {}).", controllerPos, allPlacedPositions.size(), facing, axis);
        updateFrameForController(level, controllerPos);
    }

    public void destroyStructure(Level level, BlockPos controllerPos, Direction.Axis axis) {
        if (level.isClientSide || IS_DESTROYING.get()) return;
        IS_DESTROYING.set(true);
        try {
            for (BlockPos gridPos : structureMap.keySet()) {
                BlockPos worldPos = getRotatedPosAxis(controllerPos, gridPos, axis);
                BlockEntity be = level.getBlockEntity(worldPos);
                if (be instanceof IMultiblockPart) {
                    level.levelEvent(2001, worldPos, Block.getId(level.getBlockState(worldPos)));
                    level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } finally { IS_DESTROYING.set(false); }
    }

    public void destroyStructure(Level level, BlockPos controllerPos, Direction facing) {
        if (level.isClientSide || IS_DESTROYING.get()) return;
        IS_DESTROYING.set(true);
        try {
            for (BlockPos gridPos : structureMap.keySet()) {
                BlockPos worldPos = getRotatedPos(controllerPos, gridPos, facing);
                BlockEntity be = level.getBlockEntity(worldPos);
                // Если блок в мире всё ещё является частью мультиблока, сносим его
                if (be instanceof IMultiblockPart) {
                    level.levelEvent(2001, worldPos, Block.getId(level.getBlockState(worldPos)));
                    level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } finally { IS_DESTROYING.set(false); }
    }

    public void destroyStructureStator(Level level, BlockPos controllerPos, Direction facing, Direction.Axis axis) {
        if (level.isClientSide || IS_DESTROYING.get()) return;
        IS_DESTROYING.set(true);
        try {
            for (BlockPos gridPos : structureMap.keySet()) {
                BlockPos worldPos = getRotatedStatorPos(controllerPos, gridPos, facing, axis);
                BlockEntity be = level.getBlockEntity(worldPos);
                if (be instanceof IMultiblockPart) {
                    level.levelEvent(2001, worldPos, Block.getId(level.getBlockState(worldPos)));
                    level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } finally { IS_DESTROYING.set(false); }
    }

    private int getMaxY() {
        int maxY = Integer.MIN_VALUE;
        for (BlockPos local : structureMap.keySet()) {
            if (local.getY() > maxY) maxY = local.getY();
        }
        return maxY;
    }

    public Map<BlockPos, Supplier<BlockState>> getStructureMap() { return structureMap; }

    public boolean isTopRingPart(BlockPos localOffset) {
        return localOffset.getY() == getMaxY();
    }

    public List<BlockPos> getTopRingWorldPositions(BlockPos controllerPos, Direction facing) {
        List<BlockPos> topRing = new ArrayList<>();
        int maxY = getMaxY();
        for (BlockPos localOffset : structureMap.keySet()) {
            if (localOffset.getY() == maxY) {
                topRing.add(getRotatedPos(controllerPos, localOffset, facing));
            }
        }
        return topRing;
    }

    public boolean computeFrameVisible(Level level, BlockPos controllerPos, Direction facing) {
        for (BlockPos p : getTopRingWorldPositions(controllerPos, facing)) {
            if (!level.isEmptyBlock(p.above())) return true;
        }
        return false;
    }

    public static void updateFrameForController(Level level, BlockPos controllerPos) {
        if (level.isClientSide()) return;

        BlockState state = level.getBlockState(controllerPos);
        if (!(state.getBlock() instanceof IMultiblockController controller)) return;

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof IFrameSupportable fs)) return;

        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper != null) {
            fs.setFrameVisible(helper.computeFrameVisible(level, controllerPos, facing));
        }
    }

    public VoxelShape generateShapeFromParts(Direction facing) {
        return shapeCache.computeIfAbsent(facing, f -> {
            VoxelShape finalShape = Shapes.empty();

            VoxelShape controllerShape = partShapes.getOrDefault(this.controllerOffset, Block.box(0, 0, 0, 16, 16, 16));
            finalShape = Shapes.or(finalShape, rotateShape(controllerShape, f));

            for (BlockPos gridPos : structureMap.keySet()) {
                BlockPos relative = gridPos.subtract(this.controllerOffset);
                BlockPos rotatedPos = rotate(relative, f);

                VoxelShape rawShape = partShapes.getOrDefault(gridPos, Block.box(0, 0, 0, 16, 16, 16));
                VoxelShape rotatedShape = rotateShape(rawShape, f);

                VoxelShape placedShape = rotatedShape.move(rotatedPos.getX(), rotatedPos.getY(), rotatedPos.getZ());
                finalShape = Shapes.or(finalShape, placedShape);
            }
            return finalShape.optimize();
        });
    }

    private final Map<String, VoxelShape> statorShapeCache = new HashMap<>();

    public VoxelShape generateStatorShape(Direction facing, Direction.Axis axis) {
        String key = facing.name() + "_" + axis.name();
        return statorShapeCache.computeIfAbsent(key, k -> {
            // Box 1: X [-0.5, 1.5], Y [0.0, 3.0], Z [0.0, 1.0] -> -8, 0, 0 to 24, 48, 16
            VoxelShape box1 = Block.box(-8, 0, 0, 24, 48, 16);
            // Box 2: X [-1.0, 2.0], Y [0.5, 2.5], Z [0.0, 1.0] -> -16, 8, 0 to 32, 40, 16
            VoxelShape box2 = Block.box(-16, 8, 0, 32, 40, 16);
            VoxelShape baseShape = Shapes.or(box1, box2);
            
            // Hole: X [0.0, 1.0], Y [1.0, 2.0], Z [0.0, 1.0] -> 0, 16, 0 to 16, 32, 16
            VoxelShape hole = Block.box(0, 16, 0, 16, 32, 16);
            
            baseShape = Shapes.joinUnoptimized(baseShape, hole, BooleanOp.ONLY_FIRST);
            
            return rotateShapeStatorExact(baseShape, facing, axis).optimize();
        });
    }

    private static VoxelShape rotateShapeStatorExact(VoxelShape shape, Direction facing, Direction.Axis axis) {
        VoxelShape[] buffer = { Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            net.minecraft.world.phys.Vec3 center = new net.minecraft.world.phys.Vec3(0.5, 0.5, 0.5);
            net.minecraft.world.phys.Vec3 min = new net.minecraft.world.phys.Vec3(minX, minY, minZ).subtract(center);
            net.minecraft.world.phys.Vec3 max = new net.minecraft.world.phys.Vec3(maxX, maxY, maxZ).subtract(center);
            
            // Rotate the vector directly
            net.minecraft.world.phys.Vec3 p1 = rotateVecStator(min, facing, axis);
            net.minecraft.world.phys.Vec3 p2 = rotateVecStator(max, facing, axis);
            
            double nx1 = p1.x + 0.5; double ny1 = p1.y + 0.5; double nz1 = p1.z + 0.5;
            double nx2 = p2.x + 0.5; double ny2 = p2.y + 0.5; double nz2 = p2.z + 0.5;
            
            buffer[0] = Shapes.joinUnoptimized(buffer[0], Shapes.box(
                Math.min(nx1, nx2), Math.min(ny1, ny2), Math.min(nz1, nz2),
                Math.max(nx1, nx2), Math.max(ny1, ny2), Math.max(nz1, nz2)
            ), BooleanOp.OR);
        });
        return buffer[0];
    }
    
    private static net.minecraft.world.phys.Vec3 rotateVecStator(net.minecraft.world.phys.Vec3 localVec, Direction facing, Direction.Axis axis) {
        net.minecraft.core.Vec3i vy = facing.getOpposite().getNormal();
        net.minecraft.core.Vec3i vz = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).getNormal();
        net.minecraft.core.Vec3i vx = new net.minecraft.core.Vec3i(
                vy.getY() * vz.getZ() - vy.getZ() * vz.getY(),
                vy.getZ() * vz.getX() - vy.getX() * vz.getZ(),
                vy.getX() * vz.getY() - vy.getY() * vz.getX()
        );

        return new net.minecraft.world.phys.Vec3(
                localVec.x * vx.getX() + localVec.y * vy.getX() + localVec.z * vz.getX(),
                localVec.x * vx.getY() + localVec.y * vy.getY() + localVec.z * vz.getY(),
                localVec.x * vx.getZ() + localVec.y * vy.getZ() + localVec.z * vz.getZ()
        );
    }

    public static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) return shape;

        VoxelShape[] buffer = { Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double newMinX = minX, newMinZ = minZ, newMaxX = maxX, newMaxZ = maxZ;
            switch (facing) {
                case SOUTH -> { newMinX = 1.0 - maxX; newMaxX = 1.0 - minX; newMinZ = 1.0 - maxZ; newMaxZ = 1.0 - minZ; }
                case WEST  -> { newMinX = minZ; newMaxX = maxZ; newMinZ = 1.0 - maxX; newMaxZ = 1.0 - minX; }
                case EAST  -> { newMinX = 1.0 - maxZ; newMaxX = 1.0 - minZ; newMinZ = minX; newMaxZ = maxX; }
                case UP    -> {
                    buffer[0] = Shapes.joinUnoptimized(buffer[0], Shapes.box(minX, 1.0 - maxZ, minY, maxX, 1.0 - minZ, maxY), BooleanOp.OR);
                    return;
                }
                case DOWN  -> {
                    buffer[0] = Shapes.joinUnoptimized(buffer[0], Shapes.box(minX, minZ, 1.0 - maxY, maxX, maxZ, 1.0 - minY), BooleanOp.OR);
                    return;
                }
                default -> {}
            }
            buffer[0] = Shapes.joinUnoptimized(buffer[0], Shapes.box(newMinX, minY, newMinZ, newMaxX, maxY, newMaxZ), BooleanOp.OR);
        });
        return buffer[0];
    }

    public BlockPos getRotatedPos(BlockPos controllerWorldPos, BlockPos partLocalPos, Direction facing) {
        BlockPos offsetFromController = partLocalPos.subtract(this.controllerOffset);
        return controllerWorldPos.offset(rotate(offsetFromController, facing));
    }

    public BlockPos getRotatedPosAxis(BlockPos controllerWorldPos, BlockPos partLocalPos, Direction.Axis axis) {
        BlockPos offsetFromController = partLocalPos.subtract(this.controllerOffset);
        return controllerWorldPos.offset(rotateAxis(offsetFromController, axis));
    }

    public static BlockPos rotate(BlockPos pos, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case UP -> new BlockPos(pos.getX(), -pos.getZ(), pos.getY());
            case DOWN -> new BlockPos(pos.getX(), pos.getZ(), -pos.getY());
            default -> pos;
        };
    }

    public static BlockPos rotateAxis(BlockPos pos, Direction.Axis axis) {
        // Assume default orientation is Axis.Y (flat on ground, Y is up)
        return switch (axis) {
            case X -> new BlockPos(pos.getY(), -pos.getX(), pos.getZ());
            case Z -> new BlockPos(pos.getX(), -pos.getZ(), pos.getY());
            case Y -> pos;
        };
    }

    public BlockPos getRotatedStatorPos(BlockPos controllerWorldPos, BlockPos partLocalPos, Direction facing, Direction.Axis axis) {
        BlockPos offsetFromController = partLocalPos.subtract(this.controllerOffset);
        return controllerWorldPos.offset(rotateStatorPos(offsetFromController, facing, axis));
    }

    public static BlockPos rotateStatorPos(BlockPos localPos, Direction facing, Direction.Axis axis) {
        // local Y maps to -facing (Controller is at bottom relative to hole)
        net.minecraft.core.Vec3i vy = facing.getOpposite().getNormal();
        // local Z maps to the provided axis
        net.minecraft.core.Vec3i vz = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).getNormal();
        // local X is cross product of vy and vz
        net.minecraft.core.Vec3i vx = new net.minecraft.core.Vec3i(
                vy.getY() * vz.getZ() - vy.getZ() * vz.getY(),
                vy.getZ() * vz.getX() - vy.getX() * vz.getZ(),
                vy.getX() * vz.getY() - vy.getY() * vz.getX()
        );

        return new BlockPos(
                localPos.getX() * vx.getX() + localPos.getY() * vy.getX() + localPos.getZ() * vz.getX(),
                localPos.getX() * vx.getY() + localPos.getY() * vy.getY() + localPos.getZ() * vz.getY(),
                localPos.getX() * vx.getZ() + localPos.getY() * vy.getZ() + localPos.getZ() * vz.getZ()
        );
    }

    public static VoxelShape rotateShapeAxis(VoxelShape shape, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) return shape;

        VoxelShape[] buffer = { Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            switch (axis) {
                case X -> buffer[0] = Shapes.joinUnoptimized(buffer[0], Shapes.box(1.0 - maxY, minX, minZ, 1.0 - minY, maxX, maxZ), BooleanOp.OR);
                case Z -> buffer[0] = Shapes.joinUnoptimized(buffer[0], Shapes.box(minX, 1.0 - maxZ, minY, maxX, 1.0 - minZ, maxY), BooleanOp.OR);
                default -> {}
            }
        });
        return buffer[0];
    }
}