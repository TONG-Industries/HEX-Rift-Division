package com.trd.block.entity.industrial.rotation;

import com.trd.api.rotation.Rotational;
import com.trd.api.rotation.ShaftDiameter;
import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public class ShaftBlockEntity extends KineticNodeBlockEntity {

    private ItemStack attachedPulley = ItemStack.EMPTY;
    private BlockPos connectedPulley = null;

    private ItemStack attachedGear = ItemStack.EMPTY;

    private ItemStack attachedBevelStart = ItemStack.EMPTY;
    private ItemStack attachedBevelEnd = ItemStack.EMPTY;
    private ItemStack attachedRotor = ItemStack.EMPTY;

    public boolean hasRotor() {
        return !attachedRotor.isEmpty();
    }

    public ItemStack getAttachedRotor() {
        return attachedRotor;
    }

    public void setAttachedRotor(ItemStack rotor) {
        this.attachedRotor = rotor;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public ShaftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT_BE.get(), pos, state);
    }

    public boolean hasGear() {
        return !attachedGear.isEmpty();
    }

    public ItemStack getAttachedGear() {
        return attachedGear;
    }

    public boolean hasPulley() {
        return !attachedPulley.isEmpty();
    }

    public ItemStack getAttachedPulley() {
        return attachedPulley;
    }

    public void setAttachedPulley(ItemStack pulley) {
        this.attachedPulley = pulley;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public BlockPos getConnectedPulley() {
        return connectedPulley;
    }

    public void setConnectedPulley(BlockPos pos) {
        this.connectedPulley = pos;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public void setAttachedGear(ItemStack gear) {
        this.attachedGear = gear;
        this.setChanged();

        if (level != null && !level.isClientSide) {
            // Флаг 2! Тихо обновляем данные на клиенте
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public boolean hasBevelStart() {
        return !attachedBevelStart.isEmpty();
    }

    public ItemStack getAttachedBevelStart() {
        return attachedBevelStart;
    }

    public void setAttachedBevelStart(ItemStack bevel) {
        this.attachedBevelStart = bevel;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public boolean hasBevelEnd() {
        return !attachedBevelEnd.isEmpty();
    }

    public ItemStack getAttachedBevelEnd() {
        return attachedBevelEnd;
    }

    public void setAttachedBevelEnd(ItemStack bevel) {
        this.attachedBevelEnd = bevel;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private boolean isPosBetween(BlockPos mid, BlockPos a, BlockPos b) {
        if (a.getX() == b.getX() && mid.getX() == a.getX() && a.getY() == b.getY() && mid.getY() == a.getY()) {
            return (mid.getZ() > Math.min(a.getZ(), b.getZ())) && (mid.getZ() < Math.max(a.getZ(), b.getZ()));
        }
        if (a.getX() == b.getX() && mid.getX() == a.getX() && a.getZ() == b.getZ() && mid.getZ() == a.getZ()) {
            return (mid.getY() > Math.min(a.getY(), b.getY())) && (mid.getY() < Math.max(a.getY(), b.getY()));
        }
        if (a.getY() == b.getY() && mid.getY() == a.getY() && a.getZ() == b.getZ() && mid.getZ() == a.getZ()) {
            return (mid.getX() > Math.min(a.getX(), b.getX())) && (mid.getX() < Math.max(a.getX(), b.getX()));
        }
        return false;
    }

    private java.util.List<BlockPos> getPulleysBetween(net.minecraft.world.level.Level level, BlockPos a, BlockPos b) {
        java.util.List<BlockPos> list = new java.util.ArrayList<>();
        int dx = Integer.compare(b.getX(), a.getX());
        int dy = Integer.compare(b.getY(), a.getY());
        int dz = Integer.compare(b.getZ(), a.getZ());

        // Только прямые линии
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1)
            return list;

        BlockPos current = a.offset(dx, dy, dz);
        while (!current.equals(b)) {
            if (level.isLoaded(current)) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(current);
                if (be instanceof ShaftBlockEntity shaft && shaft.hasPulley()) {
                    list.add(current.immutable());
                }
            }
            current = current.offset(dx, dy, dz);
        }
        return list;
    }

    @Override
    public Direction[] getPropagationDirections() {
        BlockState state = getBlockState();
        if (!state.hasProperty(ShaftBlock.FACING))
            return new Direction[0];
        Direction facing = state.getValue(ShaftBlock.FACING);
        if (hasGear() || hasBevelStart() || hasBevelEnd())
            return Direction.values();
        return new Direction[] { facing, facing.getOpposite() };
    }

    @Override
    public java.util.List<BlockPos> getPotentialConnections(net.minecraft.world.level.Level level, BlockPos myPos) {
        java.util.List<BlockPos> list = new java.util.ArrayList<>();
        BlockState state = getBlockState();
        if (!state.hasProperty(ShaftBlock.FACING))
            return list;

        Direction facing = state.getValue(ShaftBlock.FACING);
        Direction.Axis axis = facing.getAxis();
        int gearSize = state.getValue(ShaftBlock.GEAR_SIZE);

        // 2. РЕМЕННЫЕ СВЯЗИ (Динамическое сканирование)
        if (this.hasPulley()) {
            if (this.connectedPulley != null) {
                // Цель ремня уже известна — добавляем её и промежуточные шкивы
                list.add(this.connectedPulley);
                list.addAll(getPulleysBetween(level, myPos, this.connectedPulley));
                // Пропускаем полный скан: при построении сети BFS дойдёт до нас с другого конца
            } else {
                // connectedPulley == null: полный скан для поиска ремней, указывающих НА НАС
                // Это выполняется только при первом построении сети или когда связь не установлена
                int radius = 16;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= radius) {
                                if (dx == 0 && dy == 0 && dz == 0) continue;
                                BlockPos scanPos = myPos.offset(dx, dy, dz);
                                if (level.isLoaded(scanPos)) {
                                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(scanPos);
                                    if (be instanceof ShaftBlockEntity otherShaft && otherShaft.hasPulley()) {
                                        BlockPos theirTarget = otherShaft.getConnectedPulley();
                                        if (theirTarget != null) {
                                            if (theirTarget.equals(myPos) || isPosBetween(myPos, scanPos, theirTarget)) {
                                                list.add(scanPos);
                                                if (!theirTarget.equals(myPos)) {
                                                    list.add(theirTarget);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Осевые соединения (Вал-к-Валу). Всегда добавляем перед и зад.
        list.add(myPos.relative(facing));
        list.add(myPos.relative(facing.getOpposite()));

        // 2. Если есть шестерня, ищем соседей в плоскости
        if (gearSize > 0) {
            // Проверяем квадрат 5x5 вокруг шестерни
            for (BlockPos pos : BlockPos.betweenClosed(myPos.offset(-2, -2, -2), myPos.offset(2, 2, 2))) {
                if (pos.equals(myPos))
                    continue;

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ShaftBlockEntity otherShaft) {
                    if (!otherShaft.hasGear())
                        continue;
                    int otherSize = otherShaft.getBlockState().getValue(ShaftBlock.GEAR_SIZE);
                    Direction.Axis otherAxis = otherShaft.getBlockState().getValue(ShaftBlock.FACING).getAxis();

                    if (axis == otherAxis) {
                        // Отсекаем блоки не в нашей плоскости
                        if (axis == Direction.Axis.X && pos.getX() != myPos.getX())
                            continue;
                        if (axis == Direction.Axis.Y && pos.getY() != myPos.getY())
                            continue;
                        if (axis == Direction.Axis.Z && pos.getZ() != myPos.getZ())
                            continue;

                        // Считаем дистанцию по осям плоскости
                        int d1 = 0, d2 = 0;
                        if (axis == Direction.Axis.X) {
                            d1 = Math.abs(pos.getY() - myPos.getY());
                            d2 = Math.abs(pos.getZ() - myPos.getZ());
                        }
                        if (axis == Direction.Axis.Y) {
                            d1 = Math.abs(pos.getX() - myPos.getX());
                            d2 = Math.abs(pos.getZ() - myPos.getZ());
                        }
                        if (axis == Direction.Axis.Z) {
                            d1 = Math.abs(pos.getX() - myPos.getX());
                            d2 = Math.abs(pos.getY() - myPos.getY());
                        }

                        if (gearSize == 1) {
                            if (otherSize == 1 && ((d1 == 1 && d2 == 0) || (d1 == 0 && d2 == 1)))
                                list.add(pos.immutable());
                            if (otherSize == 2 && d1 == 1 && d2 == 1)
                                list.add(pos.immutable());
                        } else if (gearSize == 2) {
                            if (otherSize == 1 && d1 == 1 && d2 == 1)
                                list.add(pos.immutable());
                        }
                    } else {
                        // Перпендикулярные оси (только для больших шестерней)
                        if (gearSize == 2 && otherSize == 2) {
                            int dx = Math.abs(pos.getX() - myPos.getX());
                            int dy = Math.abs(pos.getY() - myPos.getY());
                            int dz = Math.abs(pos.getZ() - myPos.getZ());

                            // Для перпендикулярных 2x2 шестерней: смещение по двум осям шестерней должно
                            // быть 1, а по третьей 0
                            if (axis != Direction.Axis.X && otherAxis != Direction.Axis.X && dx != 0)
                                continue;
                            if (axis != Direction.Axis.Y && otherAxis != Direction.Axis.Y && dy != 0)
                                continue;
                            if (axis != Direction.Axis.Z && otherAxis != Direction.Axis.Z && dz != 0)
                                continue;

                            if (axis == Direction.Axis.X || otherAxis == Direction.Axis.X) {
                                if (dx != 1)
                                    continue;
                            }
                            if (axis == Direction.Axis.Y || otherAxis == Direction.Axis.Y) {
                                if (dy != 1)
                                    continue;
                            }
                            if (axis == Direction.Axis.Z || otherAxis == Direction.Axis.Z) {
                                if (dz != 1)
                                    continue;
                            }

                            list.add(pos.immutable());
                        }
                    }
                }
            }
        }

        // 3. Конические шестерни (Bevel Gears)
        if (this.hasBevelStart() || this.hasBevelEnd()) {
            for (BlockPos pos : BlockPos.betweenClosed(myPos.offset(-1, -1, -1), myPos.offset(1, 1, 1))) {
                if (pos.equals(myPos))
                    continue;
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ShaftBlockEntity otherShaft) {
                    if (otherShaft.hasBevelStart() || otherShaft.hasBevelEnd()) {
                        Direction.Axis otherAxis = otherShaft.getBlockState().getValue(ShaftBlock.FACING).getAxis();
                        if (axis != otherAxis) {
                            list.add(pos.immutable()); // Точная проверка расстояния будет в canConnectMechanically
                        }
                    }
                }
            }
        }

        // 4. СТАТОРЫ: проверяем все 4 боковых стороны (перпендикулярно оси вала)
        // Статор должен смотреть FACING прямо на вал, чтобы попасть в его сеть
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == axis) continue; // пропускаем вдоль оси — там уже добавлены
            BlockPos sidePos = myPos.relative(dir);
            if (level.isLoaded(sidePos)) {
                BlockEntity be = level.getBlockEntity(sidePos);
                if (be instanceof StatorBlockEntity stator) {
                    // Статор смотрит на нас?
                    BlockState statorState = stator.getBlockState();
                    if (statorState.hasProperty(com.trd.block.basic.industrial.rotation.StatorBlock.FACING)) {
                        Direction statorFacing = statorState.getValue(com.trd.block.basic.industrial.rotation.StatorBlock.FACING);
                        if (sidePos.relative(statorFacing).equals(myPos)) {
                            list.add(sidePos);
                        }
                    }
                }
            }
        }

        return list;
    }

    @Override
    public float calculateTransmissionRatio(BlockPos myPos, BlockPos neighborPos, Rotational neighbor) {

        if (this.hasPulley() && neighbor instanceof ShaftBlockEntity neighborShaft && neighborShaft.hasPulley()) {
            boolean isBeltConnection = neighborPos.equals(this.connectedPulley);

            if (!isBeltConnection && neighborShaft.getConnectedPulley() != null) {
                BlockPos theirTarget = neighborShaft.getConnectedPulley();
                if (theirTarget.equals(myPos) || isPosBetween(myPos, neighborPos, theirTarget)) {
                    isBeltConnection = true;
                }
            }
            if (!isBeltConnection && this.connectedPulley != null) {
                if (isPosBetween(neighborPos, myPos, this.connectedPulley)) {
                    isBeltConnection = true;
                }
            }

            if (isBeltConnection) {
                if (this.getAttachedPulley().getItem() instanceof com.trd.item.rotation.PulleyItem p1 &&
                        neighborShaft.getAttachedPulley().getItem() instanceof com.trd.item.rotation.PulleyItem p2) {
                    return (float) p1.getDiameterPixels() / p2.getDiameterPixels();
                }
            }
        }

        if (!(neighbor instanceof ShaftBlockEntity neighborShaft))
            return 1.0f;

        int mySize = this.getBlockState().getValue(ShaftBlock.GEAR_SIZE);
        int neighborSize = neighborShaft.getBlockState().getValue(ShaftBlock.GEAR_SIZE);

        Direction myFacing = getBlockState().getValue(ShaftBlock.FACING);
        Direction neighborFacing = neighborShaft.getBlockState().getValue(ShaftBlock.FACING);
        Direction.Axis myAxis = myFacing.getAxis();
        Direction.Axis neighborAxis = neighborFacing.getAxis();

        // Проверка соединения конических шестерней (Bevel Gears)
        if (myAxis != neighborAxis && (this.hasBevelStart() || this.hasBevelEnd())
                && (neighborShaft.hasBevelStart() || neighborShaft.hasBevelEnd())) {
            java.util.List<net.minecraft.world.phys.Vec3> myBevels = new java.util.ArrayList<>();
            if (this.hasBevelStart())
                myBevels.add(getBevelPos(myPos, myAxis, true));
            if (this.hasBevelEnd())
                myBevels.add(getBevelPos(myPos, myAxis, false));

            java.util.List<net.minecraft.world.phys.Vec3> neighborBevels = new java.util.ArrayList<>();
            if (neighborShaft.hasBevelStart())
                neighborBevels.add(getBevelPos(neighborPos, neighborAxis, true));
            if (neighborShaft.hasBevelEnd())
                neighborBevels.add(getBevelPos(neighborPos, neighborAxis, false));

            for (net.minecraft.world.phys.Vec3 g1 : myBevels) {
                for (net.minecraft.world.phys.Vec3 g2 : neighborBevels) {
                    if (g1.distanceToSqr(g2) < 0.6) { // Exactly 0.5 expected (0.5^2 + 0.5^2 = 0.5)
                        net.minecraft.world.phys.Vec3 diff = g2.subtract(g1);
                        double d1 = getAxisValue(diff, myAxis);
                        double d2 = getAxisValue(diff, neighborAxis);
                        return (float) Math.signum(d1 * d2); // 1:1 ratio, but sign depends on relative orientation
                    }
                }
            }
        }

        if (myAxis != neighborAxis && mySize == 2 && neighborSize == 2) {
            // Перпендикулярное соединение (большие шестерни)
            int diff1 = 0, diff2 = 0;
            if (myAxis == Direction.Axis.X)
                diff1 = neighborPos.getX() - myPos.getX();
            if (myAxis == Direction.Axis.Y)
                diff1 = neighborPos.getY() - myPos.getY();
            if (myAxis == Direction.Axis.Z)
                diff1 = neighborPos.getZ() - myPos.getZ();

            if (neighborAxis == Direction.Axis.X)
                diff2 = neighborPos.getX() - myPos.getX();
            if (neighborAxis == Direction.Axis.Y)
                diff2 = neighborPos.getY() - myPos.getY();
            if (neighborAxis == Direction.Axis.Z)
                diff2 = neighborPos.getZ() - myPos.getZ();

            return (float) (Math.signum(diff1) * Math.signum(diff2));
        }

        // Если соединение по оси (вал-вал) - передача 1:1, знак не меняется
        if (myPos.relative(myFacing).equals(neighborPos)
                || myPos.relative(myFacing.getOpposite()).equals(neighborPos)) {
            return 1.0f;
        }

        // Если соединение боковое (через зубья шестерней) - ЗНАК ВСЕГДА ИНВЕРТИРУЕТСЯ
        float ratio = -1.0f;

        // Считаем передаточное число
        if (mySize == 1 && neighborSize == 2) {
            ratio = -0.5f; // От малой к большой скорость падает в 2 раза
        } else if (mySize == 2 && neighborSize == 1) {
            ratio = -2.0f; // От большой к малой скорость возрастает в 2 раза
        }

        return ratio;
    }

    @Override
    public boolean canConnectMechanically(BlockPos myPos, BlockPos neighborPos, Rotational neighbor) {

        if (this.hasPulley() && neighbor instanceof ShaftBlockEntity neighborShaft && neighborShaft.hasPulley()) {
            if (neighborPos.equals(this.connectedPulley))
                return true;
            if (neighborShaft.getConnectedPulley() != null) {
                BlockPos theirTarget = neighborShaft.getConnectedPulley();
                if (theirTarget.equals(myPos) || isPosBetween(myPos, neighborPos, theirTarget))
                    return true;
            }
            if (this.connectedPulley != null && isPosBetween(neighborPos, myPos, this.connectedPulley))
                return true;
        }

        ShaftDiameter thisDiameter = ((ShaftBlock) this.getBlockState().getBlock()).getDiameter();
        Direction thisFacing = getBlockState().getValue(ShaftBlock.FACING);

        // Проверяем, находятся ли валы на одной прямой линии (торец к торцу)
        boolean isEndToEnd = myPos.relative(thisFacing).equals(neighborPos) ||
                myPos.relative(thisFacing.getOpposite()).equals(neighborPos);

        if (neighbor instanceof ShaftBlockEntity otherShaft) {
            Direction otherFacing = otherShaft.getBlockState().getValue(ShaftBlock.FACING);
            ShaftDiameter otherDiameter = ((ShaftBlock) otherShaft.getBlockState().getBlock()).getDiameter();

            Direction.Axis myAxis = thisFacing.getAxis();
            Direction.Axis otherAxis = otherFacing.getAxis();

            if (isEndToEnd) {
                return thisDiameter == otherDiameter && myAxis == otherAxis;
            } else {
                // Проверка соединения конических шестерней (Bevel Gears)
                if (myAxis != otherAxis && (this.hasBevelStart() || this.hasBevelEnd())
                        && (otherShaft.hasBevelStart() || otherShaft.hasBevelEnd())) {
                    java.util.List<net.minecraft.world.phys.Vec3> myBevels = new java.util.ArrayList<>();
                    if (this.hasBevelStart())
                        myBevels.add(getBevelPos(myPos, myAxis, true));
                    if (this.hasBevelEnd())
                        myBevels.add(getBevelPos(myPos, myAxis, false));

                    java.util.List<net.minecraft.world.phys.Vec3> neighborBevels = new java.util.ArrayList<>();
                    if (otherShaft.hasBevelStart())
                        neighborBevels.add(getBevelPos(neighborPos, otherAxis, true));
                    if (otherShaft.hasBevelEnd())
                        neighborBevels.add(getBevelPos(neighborPos, otherAxis, false));

                    for (net.minecraft.world.phys.Vec3 g1 : myBevels) {
                        for (net.minecraft.world.phys.Vec3 g2 : neighborBevels) {
                            if (g1.distanceToSqr(g2) < 0.6) { // Exactly 0.5 distance
                                return true;
                            }
                        }
                    }
                }

                // Боковое или диагональное соединение шестерней
                if (!this.hasGear() || !otherShaft.hasGear())
                    return false;

                if (myAxis == otherAxis) {
                    int mySize = this.getBlockState().getValue(ShaftBlock.GEAR_SIZE);
                    int otherSize = otherShaft.getBlockState().getValue(ShaftBlock.GEAR_SIZE);

                    int d1 = 0, d2 = 0;
                    if (myAxis == Direction.Axis.X) {
                        d1 = Math.abs(neighborPos.getY() - myPos.getY());
                        d2 = Math.abs(neighborPos.getZ() - myPos.getZ());
                    }
                    if (myAxis == Direction.Axis.Y) {
                        d1 = Math.abs(neighborPos.getX() - myPos.getX());
                        d2 = Math.abs(neighborPos.getZ() - myPos.getZ());
                    }
                    if (myAxis == Direction.Axis.Z) {
                        d1 = Math.abs(neighborPos.getX() - myPos.getX());
                        d2 = Math.abs(neighborPos.getY() - myPos.getY());
                    }

                    if (mySize == 1 && otherSize == 1) {
                        return (d1 == 1 && d2 == 0) || (d1 == 0 && d2 == 1);
                    } else if (mySize == 2 && otherSize == 2) {
                        return false; // Отключено по запросу
                    } else {
                        return d1 == 1 && d2 == 1;
                    }
                } else {
                    int mySize = this.getBlockState().getValue(ShaftBlock.GEAR_SIZE);
                    int otherSize = otherShaft.getBlockState().getValue(ShaftBlock.GEAR_SIZE);

                    if (mySize == 2 && otherSize == 2) {
                        int dx = Math.abs(neighborPos.getX() - myPos.getX());
                        int dy = Math.abs(neighborPos.getY() - myPos.getY());
                        int dz = Math.abs(neighborPos.getZ() - myPos.getZ());

                        if (myAxis != Direction.Axis.X && otherAxis != Direction.Axis.X && dx != 0)
                            return false;
                        if (myAxis != Direction.Axis.Y && otherAxis != Direction.Axis.Y && dy != 0)
                            return false;
                        if (myAxis != Direction.Axis.Z && otherAxis != Direction.Axis.Z && dz != 0)
                            return false;

                        if (myAxis == Direction.Axis.X || otherAxis == Direction.Axis.X) {
                            if (dx != 1)
                                return false;
                        }
                        if (myAxis == Direction.Axis.Y || otherAxis == Direction.Axis.Y) {
                            if (dy != 1)
                                return false;
                        }
                        if (myAxis == Direction.Axis.Z || otherAxis == Direction.Axis.Z) {
                            if (dz != 1)
                                return false;
                        }

                        return true;
                    }
                }
            }
        }
        if (neighbor instanceof BearingBlockEntity bearing) {
            return bearing.hasShaft() && bearing.getShaftDiameter() == thisDiameter;
        }
        if (neighbor instanceof MotorElectroBlockEntity) {
            return thisDiameter == ShaftDiameter.LIGHT;
        }
        if (neighbor instanceof StatorBlockEntity stator) {
            // Вал разрешает соединение со статором, если тот смотрит на вал
            return stator.canConnectMechanically(neighborPos, myPos, this);
        }
        return true;
    }

    private net.minecraft.world.phys.Vec3 getBevelPos(BlockPos pos, Direction.Axis axis, boolean isStart) {
        double offset = isStart ? -0.5 : 0.5;
        double x = pos.getX() + 0.5 + (axis == Direction.Axis.X ? offset : 0);
        double y = pos.getY() + 0.5 + (axis == Direction.Axis.Y ? offset : 0);
        double z = pos.getZ() + 0.5 + (axis == Direction.Axis.Z ? offset : 0);
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }

    private double getAxisValue(net.minecraft.world.phys.Vec3 vec, Direction.Axis axis) {
        return switch (axis) {
            case X -> vec.x;
            case Y -> vec.y;
            case Z -> vec.z;
        };
    }

    // setSpeed, shouldSyncSpeed, setNetworkScale, getNetworkScale — унаследованы от KineticNodeBlockEntity

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // speed, lastSyncedSpeed, networkScale

        if (!attachedGear.isEmpty())
            tag.put("AttachedGear", attachedGear.save(new CompoundTag()));
        if (!attachedBevelStart.isEmpty())
            tag.put("AttachedBevelStart", attachedBevelStart.save(new CompoundTag()));
        if (!attachedBevelEnd.isEmpty())
            tag.put("AttachedBevelEnd", attachedBevelEnd.save(new CompoundTag()));
        if (!attachedRotor.isEmpty())
            tag.put("AttachedRotor", attachedRotor.save(new CompoundTag()));
        if (!attachedPulley.isEmpty())
            tag.put("AttachedPulley", attachedPulley.save(new CompoundTag()));
        if (connectedPulley != null)
            tag.put("ConnectedPulley", net.minecraft.nbt.NbtUtils.writeBlockPos(connectedPulley));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag); // speed, lastSyncedSpeed, networkScale

        this.attachedGear = tag.contains("AttachedGear") ? ItemStack.of(tag.getCompound("AttachedGear"))
                : ItemStack.EMPTY;
        this.attachedBevelStart = tag.contains("AttachedBevelStart")
                ? ItemStack.of(tag.getCompound("AttachedBevelStart"))
                : ItemStack.EMPTY;
        this.attachedBevelEnd = tag.contains("AttachedBevelEnd") ? ItemStack.of(tag.getCompound("AttachedBevelEnd"))
                : ItemStack.EMPTY;
        this.attachedRotor = tag.contains("AttachedRotor") ? ItemStack.of(tag.getCompound("AttachedRotor"))
                : ItemStack.EMPTY;
        this.attachedPulley = tag.contains("AttachedPulley") ? ItemStack.of(tag.getCompound("AttachedPulley"))
                : ItemStack.EMPTY;
        this.connectedPulley = tag.contains("ConnectedPulley")
                ? net.minecraft.nbt.NbtUtils.readBlockPos(tag.getCompound("ConnectedPulley"))
                : null;
    }

    // getUpdateTag, getUpdatePacket, onDataPacket, onLoad — унаследованы от KineticNodeBlockEntity

    @Override
    public long getVisualSpeed() {
        BlockState state = getBlockState();
        if (!state.hasProperty(ShaftBlock.FACING))
            return this.speed;

        Direction facing = state.getValue(ShaftBlock.FACING);
        // Инвертируем визуальную скорость для позитивных осей (правило правой руки)
        if (facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.UP) {
            return -this.speed;
        }
        return this.speed;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        BlockPos connectedPos = getConnectedPulley();
        net.minecraft.world.phys.AABB box = super.getRenderBoundingBox();
        
        if (this.hasGear()) {
            box = box.inflate(1.5D); 
        } else {
            box = box.inflate(0.5D);
        }
        
        if (connectedPos != null) {
            return box.minmax(new net.minecraft.world.phys.AABB(connectedPos)).inflate(1.5D);
        }
        return box;
    }

    // getSpeed() — унаследован от KineticNodeBlockEntity

    @Override
    public long getTorque() {
        return 0;
    }

    @Override
    public long getMaxSpeed() {
        if (getBlockState().getBlock() instanceof ShaftBlock shaft) {
            return (long) (shaft.getMaterial().getBaseMaxSpeed() * shaft.getDiameter().getSpeedMultiplier());
        }
        return 256;
    }

    @Override
    public long getMaxTorque() {
        if (getBlockState().getBlock() instanceof ShaftBlock shaft) {
            return (long) (shaft.getMaterial().getBaseMaxTorque() * shaft.getDiameter().getTorqueMultiplier());
        }
        return 1024;
    }

    @Override
    public long getInertiaContribution() {
        if (getBlockState().getBlock() instanceof ShaftBlock shaft) {
            return (long) (shaft.getMaterial().baseInertia() * shaft.getDiameter().inertiaMod);
        }
        return 5;
    }

    @Override
    public long getFrictionContribution() {
        if (hasRotor()) return 6;

        // Если есть детали, проверяем, задействованы ли они в передаче
        if (hasGear() || hasPulley() || hasBevelStart() || hasBevelEnd()) {
            if (level != null) {
                for (BlockPos neighborPos : getPotentialConnections(level, worldPosition)) {
                    if (level.getBlockEntity(neighborPos) instanceof Rotational neighbor) {
                        // Если соединение механическое и НЕ осевое (т.е. через зубья/ремень)
                        if (this.canConnectMechanically(worldPosition, neighborPos, neighbor) &&
                            neighbor.canConnectMechanically(neighborPos, worldPosition, this)) {
                            
                            if (!isAxialConnection(worldPosition, neighborPos)) {
                                return 1;
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    private boolean isAxialConnection(BlockPos myPos, BlockPos neighborPos) {
        BlockState state = getBlockState();
        if (!state.hasProperty(ShaftBlock.FACING)) return false;
        Direction facing = state.getValue(ShaftBlock.FACING);
        return myPos.relative(facing).equals(neighborPos) || 
               myPos.relative(facing.getOpposite()).equals(neighborPos);
    }

    @Override
    public float getFrictionMultiplier() {
        return hasRotor() ? 1.2f : 1.0f;
    }

    /**
     * Вал не потребляет момент напрямую — каждый статор сам декларирует своё потребление
     * через NodeRole.CONSUMER и getConsumedTorque() в StatorBlockEntity.
     */
    @Override
    public long getConsumedTorque() {
        return 0;
    }

    @Override
    public long getMaxTorqueTolerance() {
        return 1000;
    }
}