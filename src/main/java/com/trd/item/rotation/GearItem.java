package com.trd.item.rotation;

import com.trd.api.rotation.ShaftDiameter;
import com.trd.api.rotation.ShaftMaterial;
import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.entity.industrial.rotation.ShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

public class GearItem extends Item {
    private final int gearSize;
    private final ShaftMaterial material;

    public GearItem(Properties properties, int gearSize, ShaftMaterial material) {
        super(properties);
        this.gearSize = gearSize;
        this.material = material;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof ShaftBlock shaftBlock) {
            if (level.getBlockEntity(pos) instanceof ShaftBlockEntity targetBE) {
                if (targetBE.hasCentralAttachment()) {
                    return InteractionResult.PASS;
                }
            } else {
                return InteractionResult.PASS;
            }

            ShaftDiameter shaftDiameter = shaftBlock.getDiameter();

            // Логика совместимости:
            // gear1 -> LIGHT
            // gear2 -> LIGHT, MEDIUM
            // gear3 -> MEDIUM, HEAVY
            boolean canAttach = false;
            if (gearSize == 1 && shaftDiameter == ShaftDiameter.LIGHT) canAttach = true;
            else if (gearSize == 2 && (shaftDiameter == ShaftDiameter.LIGHT || shaftDiameter == ShaftDiameter.MEDIUM)) canAttach = true;
            else if (gearSize == 3 && (shaftDiameter == ShaftDiameter.MEDIUM || shaftDiameter == ShaftDiameter.HEAVY)) canAttach = true;

            if (canAttach) {
                if (gearSize == 2) {
                    Direction.Axis axis = state.getValue(ShaftBlock.FACING).getAxis();
                    Direction[] crossDirs = new Direction[4];
                    Direction[][] diagDirs = new Direction[4][2];
                    
                    if (axis == Direction.Axis.X) {
                        crossDirs = new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
                        diagDirs = new Direction[][]{{Direction.UP, Direction.NORTH}, {Direction.UP, Direction.SOUTH}, {Direction.DOWN, Direction.NORTH}, {Direction.DOWN, Direction.SOUTH}};
                    } else if (axis == Direction.Axis.Y) {
                        crossDirs = new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
                        diagDirs = new Direction[][]{{Direction.EAST, Direction.NORTH}, {Direction.EAST, Direction.SOUTH}, {Direction.WEST, Direction.NORTH}, {Direction.WEST, Direction.SOUTH}};
                    } else {
                        crossDirs = new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
                        diagDirs = new Direction[][]{{Direction.UP, Direction.EAST}, {Direction.UP, Direction.WEST}, {Direction.DOWN, Direction.EAST}, {Direction.DOWN, Direction.WEST}};
                    }

                    // Проверка крестовины
                    for (Direction dir : crossDirs) {
                        BlockPos checkPos = pos.relative(dir);
                        if (!level.getBlockState(checkPos).canBeReplaced()) {
                            if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("§cНет места для шестерни! (Крестовина)"), true);
                            return InteractionResult.FAIL;
                        }
                    }

                    // Проверка диагоналей
                    for (Direction[] diag : diagDirs) {
                        BlockPos checkPos = pos.relative(diag[0]).relative(diag[1]);
                        BlockState diagState = level.getBlockState(checkPos);
                        if (!diagState.canBeReplaced()) {
                            if (diagState.getBlock() instanceof ShaftBlock) {
                                if (diagState.getValue(ShaftBlock.GEAR_SIZE) == 2) {
                                    if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("§cНет места для шестерни! (Диагональ занята другой шестерней 2x2)"), true);
                                    return InteractionResult.FAIL;
                                }
                            } else {
                                if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("§cНет места для шестерни! (Диагональ заблокирована)"), true);
                                return InteractionResult.FAIL;
                            }
                        }
                    }
                }

                if (!level.isClientSide) {

                    if (level.getBlockEntity(pos) instanceof ShaftBlockEntity shaftBE) {
                        ItemStack gearStack = context.getItemInHand().copy();
                        gearStack.setCount(1);
                        shaftBE.setAttachedGear(gearStack);
                    }

                    // 2. ПОТОМ обновляем BlockState (это вызовет рендер на клиенте с уже готовыми данными)
                    level.setBlock(pos, state.setValue(ShaftBlock.GEAR_SIZE, gearSize), 3);

                    // 3. Тратим предмет
                    context.getItemInHand().shrink(1);
                    level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // 4. ПЕРЕСОБИРАЕМ СЕТЬ (ОБЯЗАТЕЛЬНО!)
                    com.trd.api.rotation.KineticNetworkManager manager = com.trd.api.rotation.KineticNetworkManager.get((net.minecraft.server.level.ServerLevel) level);
                    manager.updateNetworkAfterRemove(pos);
                    manager.updateNetworkAfterPlace(pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    public int getGearSize() { return gearSize; }
    public ShaftMaterial getMaterial() { return material; }
}
