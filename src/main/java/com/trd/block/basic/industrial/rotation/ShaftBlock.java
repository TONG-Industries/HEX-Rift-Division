package com.trd.block.basic.industrial.rotation;

import com.trd.api.rotation.KineticNetworkManager;
import com.trd.api.rotation.ShaftDiameter;
import com.trd.api.rotation.ShaftMaterial;
import com.trd.block.entity.industrial.rotation.ShaftBlockEntity;
import com.trd.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShaftBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty GEAR_SIZE = IntegerProperty.create("gear_size", 0, 3);
    public static final IntegerProperty PULLEY_SIZE = IntegerProperty.create("pulley_size", 0, 3);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty HAS_BEVEL_START = net.minecraft.world.level.block.state.properties.BooleanProperty.create("has_bevel_start");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty HAS_BEVEL_END = net.minecraft.world.level.block.state.properties.BooleanProperty.create("has_bevel_end");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty HAS_FLYWHEEL = net.minecraft.world.level.block.state.properties.BooleanProperty.create("has_flywheel");

    // НОВЫЕ ПЕРЕМЕННЫЕ
    private final ShaftMaterial material;
    private final ShaftDiameter diameter;

    // НОВЫЙ КОНСТРУКТОР НА 3 АРГУМЕНТА
    public ShaftBlock(Properties properties, ShaftMaterial material, ShaftDiameter diameter) {
        super(properties);
        this.material = material;
        this.diameter = diameter;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(GEAR_SIZE, 0)
                .setValue(PULLEY_SIZE, 0)
                .setValue(HAS_BEVEL_START, false)
                .setValue(HAS_BEVEL_END, false)
                .setValue(HAS_FLYWHEEL, false)); // По умолчанию вал пустой
    }

    // Геттеры для сущности (BlockEntity)
    public ShaftMaterial getMaterial() { return material; }
    public ShaftDiameter getDiameter() { return diameter; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trd.shaft_material").append(": ").append(Component.translatable("metal.trd." + material.name())).withStyle(ChatFormatting.GRAY));

        long maxSpeed = (long) (material.baseSpeed() * diameter.getSpeedMultiplier());
        long maxTorque = (long) (material.baseTorque() * diameter.getTorqueMultiplier());
        double inertia = material.baseInertia() * diameter.inertiaMod;

        tooltip.add(Component.translatable("tooltip.trd.max_speed").append(": ").append(Component.literal(String.valueOf(maxSpeed))).append(" RPM").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.trd.max_torque").append(": ").append(Component.literal(String.valueOf(maxTorque))).append(" Nm").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.trd.inertia").append(": ").append(Component.literal(String.format("%.2f", inertia))).withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GEAR_SIZE, PULLEY_SIZE, HAS_BEVEL_START, HAS_BEVEL_END, HAS_FLYWHEEL);
    }

    // ДИНАМИЧЕСКИЙ ХИТБОКС (зависит от диаметра)
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double p = diameter.pixels / 2.0;
        double min = 8.0 - p;
        double max = 8.0 + p;

        // Хитбокс самого вала
        VoxelShape shaftShape = switch (state.getValue(FACING).getAxis()) {
            case X -> Block.box(0.0, min, min, 16.0, max, max);
            case Y -> Block.box(min, 0.0, min, max, 16.0, max);
            case Z -> Block.box(min, min, 0.0, max, max, 16.0);
        };

        int gearSize = state.getValue(GEAR_SIZE);
        int pulleySize = state.getValue(PULLEY_SIZE);

        VoxelShape attachmentShape = Shapes.empty();
        double tMin = 6.0, tMax = 10.0; // Толщина детали по центру оси (4 пикселя)

        // 1. Хитбокс шкива (Динамический, основан на размере)
        if (pulleySize > 0) {
            // Размеры в пикселях: 1 = 12px, 2 = 24px, 3 = 36px
            double pulleyRadius = (pulleySize == 1 ? 6.0 : (pulleySize == 2 ? 12.0 : 18.0));
            double pMin = 8.0 - pulleyRadius;
            double pMax = 8.0 + pulleyRadius;

            attachmentShape = switch (state.getValue(FACING).getAxis()) {
                case X -> Block.box(tMin, pMin, pMin, tMax, pMax, pMax);
                case Y -> Block.box(pMin, tMin, pMin, pMax, tMax, pMax);
                case Z -> Block.box(pMin, pMin, tMin, pMax, pMax, tMax);
            };
            return Shapes.or(shaftShape, attachmentShape);
        }

        if (gearSize == 1) {
            double gMin = 0.0, gMax = 16.0;
            VoxelShape gearShape = switch (state.getValue(FACING).getAxis()) {
                case X -> Block.box(tMin, gMin, gMin, tMax, gMax, gMax);
                case Y -> Block.box(gMin, tMin, gMin, gMax, tMax, gMax);
                case Z -> Block.box(gMin, gMin, tMin, gMax, gMax, tMax);
            };
            attachmentShape = Shapes.or(attachmentShape, gearShape);
        } else if (gearSize >= 2) {
            double gMin = -8.0, gMax = 24.0;
            VoxelShape gearShape = switch (state.getValue(FACING).getAxis()) {
                case X -> Block.box(tMin, gMin, gMin, tMax, gMax, gMax);
                case Y -> Block.box(gMin, tMin, gMin, gMax, tMax, gMax);
                case Z -> Block.box(gMin, gMin, tMin, gMax, gMax, tMax);
            };
            attachmentShape = Shapes.or(attachmentShape, gearShape);
        }

        // 3. Хитбокс конических шестерней
        if (state.getValue(HAS_BEVEL_START)) {
            VoxelShape startShape = switch (state.getValue(FACING).getAxis()) {
                case X -> Block.box(0.0, 2.0, 2.0, 4.0, 14.0, 14.0);
                case Y -> Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
                case Z -> Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 4.0);
            };
            attachmentShape = Shapes.or(attachmentShape, startShape);
        }
        if (state.getValue(HAS_BEVEL_END)) {
            VoxelShape endShape = switch (state.getValue(FACING).getAxis()) {
                case X -> Block.box(12.0, 2.0, 2.0, 16.0, 14.0, 14.0);
                case Y -> Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0);
                case Z -> Block.box(2.0, 2.0, 12.0, 14.0, 14.0, 16.0);
            };
            attachmentShape = Shapes.or(attachmentShape, endShape);
        }

        // 4. Хитбокс маховика
        if (state.getValue(HAS_FLYWHEEL)) {
            double fMin = 0.0, fMax = 16.0;
            VoxelShape flywheelShape = switch (state.getValue(FACING).getAxis()) {
                case X -> Block.box(tMin, fMin, fMin, tMax, fMax, fMax);
                case Y -> Block.box(fMin, tMin, fMin, fMax, tMax, fMax);
                case Z -> Block.box(fMin, fMin, tMin, fMax, fMax, tMax);
            };
            attachmentShape = Shapes.or(attachmentShape, flywheelShape);
        }

        return Shapes.or(shaftShape, attachmentShape);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.SCREWDRIVER.get())) {
            boolean isGear = state.getValue(GEAR_SIZE) > 0;
            boolean isPulley = state.getValue(PULLEY_SIZE) > 0;
            boolean isBevelStart = state.getValue(HAS_BEVEL_START);
            boolean isBevelEnd = state.getValue(HAS_BEVEL_END);
            boolean isFlywheel = state.getValue(HAS_FLYWHEEL);
            boolean isRotor = false;
            
            BlockEntity preBe = level.getBlockEntity(pos);
            if (preBe instanceof ShaftBlockEntity shaftBE) {
                isRotor = shaftBE.hasRotor();
            }

            if (isGear || isPulley || isBevelStart || isBevelEnd || isFlywheel || isRotor) {
                if (!level.isClientSide) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ShaftBlockEntity shaftBE) {

                        // Определяем, по чему именно кликнули
                        double hitLoc = switch (state.getValue(FACING).getAxis()) {
                            case X -> hit.getLocation().x - pos.getX();
                            case Y -> hit.getLocation().y - pos.getY();
                            case Z -> hit.getLocation().z - pos.getZ();
                        };

                        if (isBevelStart && hitLoc < 0.375) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedBevelStart());
                            shaftBE.setAttachedBevelStart(net.minecraft.world.item.ItemStack.EMPTY);
                            level.setBlock(pos, state.setValue(HAS_BEVEL_START, false), 3);
                        } else if (isBevelEnd && hitLoc > 0.625) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedBevelEnd());
                            shaftBE.setAttachedBevelEnd(net.minecraft.world.item.ItemStack.EMPTY);
                            level.setBlock(pos, state.setValue(HAS_BEVEL_END, false), 3);
                        } else if (isGear && shaftBE.hasGear()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedGear());
                            shaftBE.setAttachedGear(net.minecraft.world.item.ItemStack.EMPTY);
                            level.setBlock(pos, state.setValue(GEAR_SIZE, 0), 3);
                        } else if (isPulley && shaftBE.hasPulley()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedPulley());
                            shaftBE.setAttachedPulley(net.minecraft.world.item.ItemStack.EMPTY);

                            // ОБРЫВ РЕМНЯ: Чистим память у всех связанных валов
                            com.trd.api.rotation.BeltConnectionHelper.breakBelt(level, pos);
                            shaftBE.setConnectedPulley(null);
                            level.setBlock(pos, state.setValue(PULLEY_SIZE, 0), 3);
                        } else if (isFlywheel && shaftBE.hasFlywheel()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedFlywheel());
                            shaftBE.setAttachedFlywheel(net.minecraft.world.item.ItemStack.EMPTY);
                            level.setBlock(pos, state.setValue(HAS_FLYWHEEL, false), 3);
                        } else if (isRotor && shaftBE.hasRotor()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedRotor());
                            shaftBE.setAttachedRotor(net.minecraft.world.item.ItemStack.EMPTY);
                            shaftBE.setChanged();
                            level.sendBlockUpdated(pos, state, state, 3);
                        } else {
                            return InteractionResult.PASS; // Не попали ни по одной детали
                        }

                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);

                        // Пересчет
                        KineticNetworkManager manager = KineticNetworkManager.get((ServerLevel) level);
                        manager.updateNetworkAfterRemove(pos);
                        manager.updateNetworkAfterPlace(pos);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        // Получаем позицию блока, по которому кликнул игрок
        BlockPos posAgainst = pos.relative(clickedFace.getOpposite());
        BlockState stateAgainst = level.getBlockState(posAgainst);
        BlockEntity beAgainst = level.getBlockEntity(posAgainst);

        // 1. ЖЕСТКИЙ ЗАПРЕТ: Разрешаем ставить только на кинетические блоки!
        // Поскольку все наши механизмы (мотор, подшипник, вал) имеют BlockEntity
        // с интерфейсом Rotational, эта проверка отсекает вообще всё лишнее (землю, камень, воздух).
        if (!(beAgainst instanceof com.trd.api.rotation.Rotational)) {
            return null; // Отменяем установку
        }

        // Изначально предполагаем направление по взгляду игрока
        Direction placementFacing = context.getNearestLookingDirection().getOpposite();

        // --- ЛОГИКА ВЫРАВНИВАНИЯ И ПРОВЕРКИ ДИАМЕТРОВ ---
        if (stateAgainst.getBlock() instanceof ShaftBlock clickedShaft) {
            // Если мы пытаемся продолжить линию вала (кликаем в торец)
            if (stateAgainst.getValue(FACING).getAxis() == clickedFace.getAxis()) {
                if (clickedShaft.getDiameter() != this.diameter) {
                    return null; // Разные диаметры соединять нельзя
                }
                placementFacing = stateAgainst.getValue(FACING);
            } else {
                // Если кликнули сбоку по валу, делаем новый вал параллельным
                placementFacing = stateAgainst.getValue(FACING);
            }
        } else if (stateAgainst.getBlock() instanceof BearingBlock) {
            // Перенимаем ось подшипника
            placementFacing = stateAgainst.getValue(BearingBlock.FACING);
        } else if (stateAgainst.getBlock() instanceof MotorElectroBlock) {
            // Перенимаем ось мотора
            placementFacing = stateAgainst.getValue(MotorElectroBlock.FACING);
        }

        // --- ЗАЩИТА ОТ СОСЕДЕЙ ---
        // Проверяем, не упрется ли новый вал в вал другого диаметра с другой стороны
        for (Direction dir : new Direction[]{placementFacing, placementFacing.getOpposite()}) {
            BlockState neighborState = level.getBlockState(pos.relative(dir));

            if (neighborState.getBlock() instanceof ShaftBlock neighborShaft) {
                if (neighborState.getValue(FACING).getAxis() == placementFacing.getAxis()) {
                    if (neighborShaft.getDiameter() != this.diameter) {
                        return null; // С другой стороны стоит вал другого размера!
                    }
                }
            }
        }

        // --- ПРОВЕРКА НА ПРОГИБ ---
        if (!level.isClientSide) {
            if (!canBeSupported(level, pos, placementFacing)) {
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("message.trd.too_far_from_support", diameter.maxSupportDistance)
                                    .withStyle(ChatFormatting.RED), true);
                }
                return null;
            }
        }

        return this.defaultBlockState().setValue(FACING, placementFacing);
    }

    private boolean canBeSupported(Level level, BlockPos pos, Direction facing) {
        int maxDist = diameter.maxSupportDistance;

        // Ищем опору в обе стороны по оси
        if (hasSupportInRange(level, pos, facing, maxDist)) return true;
        return hasSupportInRange(level, pos, facing.getOpposite(), maxDist);
    }

    private boolean hasSupportInRange(Level level, BlockPos startPos, Direction dir, int maxDist) {
        BlockPos.MutableBlockPos current = startPos.mutable();
        for (int i = 1; i <= maxDist; i++) {
            current.move(dir);
            BlockState state = level.getBlockState(current);

            if (isSupport(level, current, dir)) return true;

            // Если это такой же вал (того же диаметра) — идем дальше
            if (state.getBlock() instanceof ShaftBlock otherShaft && otherShaft.getDiameter() == this.diameter) {
                if (state.getValue(FACING).getAxis() == dir.getAxis()) {
                    continue;
                }
            }

            return false; // Любой другой блок прерывает линию
        }
        return false;
    }

    private boolean isSupport(Level level, BlockPos pos, Direction axisDir) {
        BlockState state = level.getBlockState(pos);
        net.minecraft.world.level.block.Block block = state.getBlock();
        if (block instanceof BearingBlock) return true;
        if (block instanceof MotorElectroBlock) {
            return state.getValue(MotorElectroBlock.FACING) == axisDir.getOpposite();
        }
        if (block instanceof com.trd.multiblock.industrial.SteamEngineBlock) {
            return state.getValue(com.trd.multiblock.industrial.SteamEngineBlock.FACING).getAxis() == axisDir.getAxis();
        }
        if (block instanceof TachometerBlock) {
            return state.getValue(TachometerBlock.FACING).getAxis() == axisDir.getAxis();
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterPlace(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ShaftBlockEntity shaftBE) {
                if (shaftBE.hasGear()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedGear());
                }
                if (shaftBE.hasBevelStart()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedBevelStart());
                }
                if (shaftBE.hasBevelEnd()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedBevelEnd());
                }
                if (shaftBE.hasPulley()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedPulley());

                    // ОБРЫВ РЕМНЯ при ломании блока киркой
                    com.trd.api.rotation.BeltConnectionHelper.breakBelt(level, pos);
                }
                if (shaftBE.hasFlywheel()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedFlywheel());
                }
                if (shaftBE.hasRotor()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shaftBE.getAttachedRotor());
                }
            }
            if (!level.isClientSide) {
                KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterRemove(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}