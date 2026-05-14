package com.trd.item.weapons.grenades;

import com.trd.entity.weapons.grenades.GrenadeNucProjectileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GrenadeNucItem extends Item {

    private final RegistryObject<? extends EntityType<?>> entityType;

    public GrenadeNucItem(Properties properties, RegistryObject<? extends EntityType<?>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, @Nullable List<Component> tooltip, TooltipFlag flag) {
        if (tooltip == null) return;
        tooltip.add(Component.translatable("tooltip.trd.grenade_nuc.line1").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.trd.grenade_nuc.line2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.trd.grenade_nuc.line3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                1.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            GrenadeNucProjectileEntity grenade = new GrenadeNucProjectileEntity(
                    entityType.get(), level, player
            );
            grenade.setItem(itemstack);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.2F, 1.0F);
            level.addFreshEntity(grenade);

            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}