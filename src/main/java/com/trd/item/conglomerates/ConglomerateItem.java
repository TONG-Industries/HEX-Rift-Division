package com.trd.item.conglomerates;

import com.trd.api.vein.FractionType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Кусок конгломерата. Содержит фракции (не готовые металлы),
 * которые нужно перерабатывать на станках.
 */
public class ConglomerateItem extends Item {
    public ConglomerateItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFromVein(Map<FractionType, Integer> fractions, int ou, String typeName) {
        ItemStack stack = new ItemStack(com.trd.item.ModItems.CONGLOMERATE_CHUNK.get());
        CompoundTag tag = new CompoundTag();

        CompoundTag fractionsTag = new CompoundTag();
        fractions.forEach((fraction, percent) -> fractionsTag.putInt(fraction.name(), percent));
        tag.put("Fractions", fractionsTag);

        tag.putInt("OU", ou);
        tag.putString("VeinType", typeName);

        stack.setTag(tag);
        return stack;
    }

    public static Map<FractionType, Integer> getFractions(ItemStack stack) {
        if (!stack.hasTag()) return Collections.emptyMap();
        CompoundTag tag = stack.getTag();
        if (!tag.contains("Fractions", CompoundTag.TAG_COMPOUND)) return Collections.emptyMap();

        Map<FractionType, Integer> result = new HashMap<>();
        CompoundTag fractionsTag = tag.getCompound("Fractions");
        for (String key : fractionsTag.getAllKeys()) {
            try {
                FractionType fraction = FractionType.valueOf(key);
                result.put(fraction, fractionsTag.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // Неизвестная фракция — пропускаем
            }
        }
        return result;
    }

    public static int getOU(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        return stack.getTag().getInt("OU");
    }

    public static String getVeinType(ItemStack stack) {
        if (!stack.hasTag()) return "unknown";
        return stack.getTag().getString("VeinType");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Map<FractionType, Integer> fractions = getFractions(stack);
        if (fractions.isEmpty()) {
            tooltip.add(Component.literal("§7Пустой кусок"));
            return;
        }

        tooltip.add(Component.literal("§eСодержит фракции:"));
        fractions.forEach((fraction, percent) -> {
            tooltip.add(Component.literal(String.format(" §7- %s: %d%%", fraction.getName(), percent))
                    .withStyle(style -> style.withColor(fraction.getColor())));
        });

        int ou = getOU(stack);
        tooltip.add(Component.literal(String.format("§8OU: %d", ou)));

        String type = getVeinType(stack);
        if (!type.equals("unknown")) {
            tooltip.add(Component.literal("§8Тип жилы: " + type));
        }
    }
}