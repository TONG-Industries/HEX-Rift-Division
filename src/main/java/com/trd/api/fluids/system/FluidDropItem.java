package com.trd.api.fluids.system;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FluidDropItem extends Item {
    private final Supplier<FluidType> fluidTypeSupplier;

    public FluidDropItem(Supplier<FluidType> fluidTypeSupplier, Properties properties) {
        super(properties);
        this.fluidTypeSupplier = fluidTypeSupplier;
    }

    public FluidType getFluidType() {
        return fluidTypeSupplier.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 1 капля = 1000 mB (1 ведро)
        int volume = stack.getCount() * 1000;
        tooltip.add(Component.literal("  Объём: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(volume + " mB").withStyle(ChatFormatting.AQUA)));
        tooltip.addAll(getFluidPropertiesTooltip(getFluidType()));
    }

    public static List<Component> getFluidPropertiesTooltip(FluidType fluidType) {
        List<Component> tooltip = new ArrayList<>();
        if (fluidType == null) return tooltip;

        int tempC = 20;
        int corrosivity = 0;

        if (fluidType instanceof BaseFluidType base) {
            tempC = base.getDisplayTemperature();
            corrosivity = base.getCorrosivity();
        } else {
            if (fluidType == net.minecraftforge.common.ForgeMod.WATER_TYPE.get()) tempC = 20;
            else if (fluidType == net.minecraftforge.common.ForgeMod.LAVA_TYPE.get()) tempC = 1000;
            else tempC = fluidType.getTemperature() - 273;
        }

        ChatFormatting tempColor = ChatFormatting.GOLD;
        String tempDesc = "Обычная";
        if (tempC >= 2000) { tempColor = ChatFormatting.DARK_RED; tempDesc = "Критическая"; }
        else if (tempC >= 1000) { tempColor = ChatFormatting.RED; tempDesc = "Экстремальная"; }
        else if (tempC >= 500) { tempColor = ChatFormatting.GOLD; tempDesc = "Перегретая"; }
        else if (tempC > 100) { tempColor = ChatFormatting.YELLOW; tempDesc = "Горячая"; }
        else if (tempC < 0) { tempColor = ChatFormatting.AQUA; tempDesc = "Холодная"; }

        tooltip.add(Component.literal("  Температура: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(tempC + "°C (" + tempDesc + ")").withStyle(tempColor)));

        if (corrosivity > 0) {
            ChatFormatting acidColor = ChatFormatting.YELLOW;
            String acidDesc = "Следы";
            if (corrosivity >= 150) { acidColor = ChatFormatting.DARK_RED; acidDesc = "Экстремальная коррозия"; }
            else if (corrosivity >= 100) { acidColor = ChatFormatting.RED; acidDesc = "Высококоррозионная"; }
            else if (corrosivity >= 60) { acidColor = ChatFormatting.YELLOW; acidDesc = "Коррозионная"; }
            else if (corrosivity >= 20) { acidColor = ChatFormatting.GOLD; acidDesc = "Слабокоррозионная"; }

            tooltip.add(Component.literal("  Коррозионность: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(corrosivity + " (" + acidDesc + ")").withStyle(acidColor)));
        }
        return tooltip;
    }

    public int getFluidTintColor() {
        FluidType type = getFluidType();
        if (type instanceof BaseFluidType base) return base.getTintColor();
        return 0xFFFFFFFF;
    }
}