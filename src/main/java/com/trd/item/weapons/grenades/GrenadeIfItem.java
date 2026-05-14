package com.trd.item.weapons.grenades;

import com.trd.entity.weapons.grenades.GrenadeIfProjectileEntity;
import com.trd.entity.weapons.grenades.GrenadeIfType;
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

import javax.annotation.Nullable;
import java.util.List;

public class GrenadeIfItem extends Item {

    private final GrenadeIfType grenadeType;
    private final RegistryObject<EntityType<GrenadeIfProjectileEntity>> entityType;

    public GrenadeIfItem(Properties properties, GrenadeIfType grenadeIf, RegistryObject<EntityType<GrenadeIfProjectileEntity>> entityType) {
        super(properties);
        this.grenadeType = grenadeIf;
        this.entityType = entityType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.trd.grenade_if.common.line1").withStyle(ChatFormatting.YELLOW));
        String key = switch (grenadeType) {
            case GRENADE_IF_HE -> "tooltip.trd.grenade_if.he.line2";
            case GRENADE_IF_SLIME -> "tooltip.trd.grenade_if.slime.line2";
            case GRENADE_IF_FIRE -> "tooltip.trd.grenade_if.fire.line2";
            case GRENADE_IF -> "tooltip.trd.grenade_if.standard.line2";
            default -> "tooltip.trd.grenade_if.default.line2";
        };
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            GrenadeIfProjectileEntity grenade = new GrenadeIfProjectileEntity(
                    entityType.get(), level, player, grenadeType
            );
            grenade.setItem(itemstack);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(grenade);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}