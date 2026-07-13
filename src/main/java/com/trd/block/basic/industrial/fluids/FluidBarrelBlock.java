package com.trd.block.basic.industrial.fluids;

import com.trd.api.fluids.system.BarrelTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.industrial.fluids.FluidBarrelBlockEntity;

public class FluidBarrelBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private final BarrelTier tier;

    public FluidBarrelBlock(BarrelTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public BarrelTier getTier() { return tier; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidBarrelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FLUID_BARREL_BE.get(), FluidBarrelBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof com.trd.item.tools.FluidIdentifierItem) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity be) {
                String selectedFluidId = com.trd.item.tools.FluidIdentifierItem.getSelectedFluid(stack);
                be.setFilter(selectedFluidId);
                if (selectedFluidId.equals("none")) {
                    player.displayClientMessage(Component.literal("§eBarrel filter reset (Closed)"), true);
                    level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.8F);
                } else {
                    net.minecraft.world.level.material.Fluid fluidToSet = ForgeRegistries.FLUIDS.getValue(new net.minecraft.resources.ResourceLocation(selectedFluidId));
                    String fluidName = fluidToSet != null ? Component.translatable(fluidToSet.getFluidType().getDescriptionId()).getString() : selectedFluidId;
                    player.displayClientMessage(Component.literal("§aBarrel Filter: §f" + fluidName), true);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof FluidBarrelBlockEntity) {
                net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, (FluidBarrelBlockEntity) entity, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // ФИКС ДЮПА: не дропаем предметы при переходе между типами бочек (повреждение)
            if (!(newState.getBlock() instanceof FluidBarrelBlock)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof FluidBarrelBlockEntity barrel) {
                    barrel.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                        }
                    });
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level pLevel, net.minecraft.core.BlockPos pPos, net.minecraft.world.level.block.state.BlockState pState, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity pPlacer, net.minecraft.world.item.ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide) {
            net.minecraft.world.level.block.entity.BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof FluidBarrelBlockEntity barrelBE) {
                net.minecraft.nbt.CompoundTag itemNbt = pStack.getTag();
                if (itemNbt != null && itemNbt.contains("BlockEntityTag")) {
                    barrelBE.load(itemNbt.getCompound("BlockEntityTag"));
                    barrelBE.setChanged();
                }
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState pState, net.minecraft.world.level.storage.loot.LootParams.Builder pParams) {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = pParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluidBarrelBlockEntity barrel) {
            net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(this);
            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
            barrel.saveAdditional(nbt);
            itemStack.addTagElement("BlockEntityTag", nbt);
            return java.util.Collections.singletonList(itemStack);
        }
        return super.getDrops(pState, pParams);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack pStack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter pLevel, java.util.List<net.minecraft.network.chat.Component> pTooltip, net.minecraft.world.item.TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);

        pTooltip.add(Component.literal("Ёмкость: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(tier.getCapacity() + "mB").withStyle(ChatFormatting.WHITE)));

        pTooltip.add(Component.literal("Точка плавления: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(tier.getMeltingPoint() + "°C").withStyle(ChatFormatting.GOLD)));

        pTooltip.add(Component.literal("Коррозионная стойкость: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(tier.getCorrosionResistance())).withStyle(ChatFormatting.YELLOW)));

        if (tier.isLeaking()) {
            pTooltip.add(Component.literal("⚠ Протекает: ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal(tier.getLeakRate() + "mB/сек").withStyle(ChatFormatting.RED)));
        }

        net.minecraft.nbt.CompoundTag nbt = pStack.getTag();
        net.minecraft.nbt.CompoundTag beTag = (nbt != null && nbt.contains("BlockEntityTag")) ? nbt.getCompound("BlockEntityTag") : null;

        // Что реально налито в бак
        String fluidName = beTag != null ? beTag.getString("FluidName") : "";
        int amount = beTag != null ? beTag.getInt("Amount") : 0;
        boolean hasFluid = !fluidName.isEmpty() && !fluidName.equals("minecraft:empty") && amount > 0;

        // Заданный тип (фильтр)
        String filter = beTag != null ? beTag.getString("FluidFilter") : "";
        boolean hasFilter = filter != null && !filter.isEmpty() && !filter.equals("none");

        // Какую жидкость показывать: сначала содержимое, иначе заданный тип-фильтр
        String displayId = hasFluid ? fluidName : (hasFilter ? filter : "");

        if (!displayId.isEmpty()) {
            net.minecraft.world.level.material.Fluid fluid = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(new net.minecraft.resources.ResourceLocation(displayId));
            String localizedName = fluid != null ? net.minecraft.network.chat.Component.translatable(fluid.getFluidType().getDescriptionId()).getString() : displayId;
            pTooltip.add(net.minecraft.network.chat.Component.literal("§bЖидкость: §f" + localizedName));
            pTooltip.add(net.minecraft.network.chat.Component.literal("§eОбъём: §f" + amount + " / " + tier.getCapacity() + " mB"));
        } else {
            pTooltip.add(net.minecraft.network.chat.Component.literal("§bЖидкость: §7Пусто"));
        }

        // Строка типа (фильтра)
        if (hasFilter) {
            net.minecraft.world.level.material.Fluid f = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(new net.minecraft.resources.ResourceLocation(filter));
            String fName = f != null ? net.minecraft.network.chat.Component.translatable(f.getFluidType().getDescriptionId()).getString() : filter;
            pTooltip.add(net.minecraft.network.chat.Component.literal("§aФильтр: §f" + fName));
        } else {
            pTooltip.add(net.minecraft.network.chat.Component.literal("§aФильтр: §cЗакрыто"));
        }
    }
}