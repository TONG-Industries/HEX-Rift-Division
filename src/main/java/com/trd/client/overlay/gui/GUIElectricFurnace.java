package com.trd.client.overlay.gui;

import com.trd.main.MainRegistry;
import com.trd.menu.industrial.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIElectricFurnace extends AbstractContainerScreen<ElectricFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/machine/electro_furnace_gui.png");

    public GUIElectricFurnace(ElectricFurnaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Фон
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // === Энергобар: рендер (132, 16), UV (177, 1), размер 16×52 ===
        int energy = this.menu.getDataSlot(2);
        int maxEnergy = this.menu.getDataSlot(3);
        if (maxEnergy > 0 && energy > 0) {
            int totalHeight = 52;
            int barHeight = (int) (totalHeight * ((double) energy / maxEnergy));
            if (barHeight > 0) {
                gui.blit(TEXTURE,
                        x + 132, y + 16 + (totalHeight - barHeight),
                        177, 1 + (totalHeight - barHeight),
                        16, barHeight);
            }
        }

        // === Светодиод: рендер (60, 32), UV (194, 1), размер 8×8 ===
        // Горит ТОЛЬКО когда идёт активный процесс плавки
        int progress = this.menu.getDataSlot(0);
        int maxProgress = this.menu.getDataSlot(1);
        if (progress > 0 && maxProgress > 0) {
            gui.blit(TEXTURE, x + 60, y + 32, 194, 1, 8, 8);
        }

        // === Прогресс-бар плавки: рендер (76, 51), UV (177, 54), размер 24×6 ===
        if (maxProgress > 0 && progress > 0) {
            int barWidth = (int) (24.0 * progress / maxProgress);
            if (barWidth > 0) {
                gui.blit(TEXTURE,
                        x + 76, y + 52,
                        177, 54,  // <-- исправлено с 56 на 54
                        barWidth, 6);
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);

        // Тултип энергобара (132, 16, 16, 52)
        if (isHovering(132, 16, 16, 52, mouseX, mouseY)) {
            int energy = this.menu.getDataSlot(2);
            int maxEnergy = this.menu.getDataSlot(3);
            gui.renderTooltip(this.font,
                    Component.literal(energy + " / " + maxEnergy + " JE"), mouseX, mouseY);
        }

        // Тултип прогресса плавки (76, 51, 24, 6)
        if (isHovering(76, 51, 24, 6, mouseX, mouseY)) {
            int progress = this.menu.getDataSlot(0);
            int maxProgress = this.menu.getDataSlot(1);
            if (maxProgress > 0) {
                int remaining = maxProgress - progress;
                int seconds = (int) Math.ceil(remaining / 20.0);
                gui.renderTooltip(this.font,
                        Component.literal("§6Осталось: §f" + seconds + " сек"), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // Убираем стандартные надписи, если мешают
    }
}