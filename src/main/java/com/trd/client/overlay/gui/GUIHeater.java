package com.trd.client.overlay.gui;

import com.trd.main.MainRegistry;
import com.trd.menu.industrial.HeaterMenu;
import com.trd.multiblock.industrial.HeaterBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GUIHeater extends AbstractContainerScreen<HeaterMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MainRegistry.MOD_ID, "textures/gui/machine/heater_gui.png");

    public GUIHeater(HeaterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // === Полоска нагрева с float ===
        float temp = menu.getTemperatureFloat();
        float maxTemp = HeaterBlockEntity.MAX_TEMP;
        int barWidth = 15;
        int barHeight = 51;
        int filledHeight = (int) ((temp / maxTemp) * barHeight);

        if (filledHeight > 0) {
            guiGraphics.blit(TEXTURE,
                    x + 64, y + 9 + (barHeight - filledHeight),
                    177, 19 + (barHeight - filledHeight),
                    barWidth, filledHeight);
        }

        if (menu.isBurning()) {
            guiGraphics.blit(TEXTURE, x + 104, y + 25, 177, 0, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (this.isHovering(85, 12, 16, 16, mouseX, mouseY)) {
            renderFuelTooltip(guiGraphics, mouseX, mouseY);
        } else {
            renderStandardTooltips(guiGraphics, mouseX, mouseY, x, y);
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderStandardTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        // Тултип температуры — округление до целых
        if (this.isHovering(64, 9, 15, 51, mouseX, mouseY)) {
            float temp = menu.getTemperatureFloat();
            float maxTemp = HeaterBlockEntity.MAX_TEMP;
            float percent = temp / maxTemp;
            int color = getSmoothTemperatureColor(percent);

            Component tempText = Component.literal(String.format("%.0f / %.0f °C", temp, maxTemp))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));

            guiGraphics.renderTooltip(this.font, tempText, mouseX, mouseY);
        }

        // Тултип индикатора работы — секунды без десятых
        if (this.isHovering(104, 25, 18, 18, mouseX, mouseY)) {
            if (menu.isBurning()) {
                int seconds = menu.getBurnTime() / 20;
                int totalSeconds = menu.getTotalBurnTime() / 20;

                Component timeText = Component.literal(
                        String.format("§6Осталось: §f%d§7/§f%d сек", seconds, totalSeconds)
                );
                guiGraphics.renderTooltip(this.font, timeText, mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font, Component.literal("§7Остановлен"), mouseX, mouseY);
            }
        }
    }

    private void renderFuelTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<HeaterBlockEntity.FuelTierInfo> tiers = HeaterBlockEntity.getAllTierInfos();
        int tierCount = tiers.size();

        int lineHeight = 11;
        int padding = 4;
        int iconSize = 12;
        int iconTextGap = 2;

        // Заголовок
        Component header = Component.literal("§6§lТопливные тиры:");
        int maxTextWidth = this.font.width(header);

        // Считаем максимальную ширину текста
        for (HeaterBlockEntity.FuelTierInfo info : tiers) {
            String line = String.format("§8Тир %d: §f%.0f°C, §f%d§7с.",
                    info.tier(), info.heatPerTick(), info.getBurnSeconds());
            maxTextWidth = Math.max(maxTextWidth, this.font.width(line));
        }

        int tooltipWidth = padding + iconSize + iconTextGap + maxTextWidth + padding;
        int tooltipHeight = (1 + tierCount) * lineHeight + padding * 2;

        int tooltipX = mouseX + 8;
        int tooltipY = mouseY - tooltipHeight / 2;

        if (tooltipX + tooltipWidth > this.width) tooltipX = mouseX - tooltipWidth - 8;
        if (tooltipY < 4) tooltipY = 4;
        if (tooltipY + tooltipHeight > this.height) tooltipY = this.height - tooltipHeight - 4;

        // Фон тултипа
        guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF0100010);
        guiGraphics.fill(tooltipX + 1, tooltipY, tooltipX + tooltipWidth - 1, tooltipY + 1, 0xF0500070);
        guiGraphics.fill(tooltipX + 1, tooltipY + tooltipHeight - 1, tooltipX + tooltipWidth - 1, tooltipY + tooltipHeight, 0xF0500070);
        guiGraphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + tooltipHeight, 0xF0500070);
        guiGraphics.fill(tooltipX + tooltipWidth - 1, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF0500070);

        long currentSecond = System.currentTimeMillis() / 1000;

        // Рендерим заголовок
        int headerY = tooltipY + padding;
        guiGraphics.drawString(this.font, header, tooltipX + padding, headerY + 2, 0xFFFFFF, true);

        // Рендерим каждый тир
        for (int i = 0; i < tierCount; i++) {
            HeaterBlockEntity.FuelTierInfo info = tiers.get(i);
            int lineY = headerY + (i + 1) * lineHeight;

            // Получаем предметы для анимации
            List<ItemStack> items = HeaterBlockEntity.getFuelItemsForTier(info.tier());
            if (!items.isEmpty()) {
                int itemIndex = (int) ((currentSecond + info.tier()) % items.size());
                ItemStack stack = items.get(itemIndex);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(tooltipX + padding, lineY, 100);
                guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.renderItemDecorations(this.font, stack, 0, 0);
                guiGraphics.pose().popPose();
            }

            // Текст с характеристиками
            String line = String.format("§8Тир %d: §f%.0f°C, §f%d§7с.",
                    info.tier(), info.heatPerTick(), info.getBurnSeconds());
            int textX = tooltipX + padding + iconSize + iconTextGap;
            guiGraphics.drawString(this.font, line, textX, lineY + 2, 0xFFFFFF, true);
        }
    }

    private static int getSmoothTemperatureColor(float percent) {
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        int colorGrey = 0xAAAAAA;
        int colorOrange = 0xFFAA00;
        int colorRed = 0xFF2222;

        if (percent <= 0.3f) {
            return lerpColor(colorGrey, colorOrange, percent / 0.3f);
        } else if (percent <= 0.7f) {
            return lerpColor(colorOrange, colorRed, (percent - 0.3f) / 0.4f);
        } else {
            return colorRed;
        }
    }

    private static int lerpColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }
}