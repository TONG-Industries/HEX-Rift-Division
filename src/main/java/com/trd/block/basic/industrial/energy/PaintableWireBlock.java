package com.trd.block.basic.industrial.energy;

import com.trd.block.entity.industrial.energy.PaintableWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

/**
 * Окрашиваемый провод. Наследует всю логику WireBlock (энергосеть/коннекты).
 * Форма — полный куб; камуфляж и чёрные жерла рисует PaintableConduitRenderer.
 */
public class PaintableWireBlock extends WireBlock {

    public PaintableWireBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaintableWireBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);

        // Покраска: клик блоком в руке -> копируем его вид (предмет НЕ забираем).
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block toMimic = blockItem.getBlock();
            if (isPaintable(toMimic)) {
                if (!level.isClientSide && level.getBlockEntity(pos) instanceof PaintableWireBlockEntity be) {
                    be.setMimic(toMimic.defaultBlockState());
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 0.7F, 1.4F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    private static boolean isPaintable(@Nullable Block b) {
        if (b == null) return false;
        BlockState s = b.defaultBlockState();
        // Только обычные модельные блоки; EntityBlock (мультиблоки/машины/трубы/провода) исключаем.
        return s.getRenderShape() == RenderShape.MODEL && !(b instanceof EntityBlock);
    }
}
