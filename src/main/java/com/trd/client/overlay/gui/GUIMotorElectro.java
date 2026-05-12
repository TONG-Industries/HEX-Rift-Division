package com.trd.client.overlay.gui;

import com.trd.main.MainRegistry;
import com.trd.menu.MotorElectroMenu;
import com.trd.network.ModPacketHandler;
import com.trd.network.packet.energy.SyncMotorRpmPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;

public class GUIMotorElectro extends AbstractContainerScreen<MotorElectroMenu> {

    // Текстура 128×128
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MainRegistry.MOD_ID, "textures/gui/machine/electro_motor_gui.png");

    // Размер окна GUI
    private static final int WIN_W = 126;
    private static final int WIN_H = 46;

    // --- Плашка отображения RPM ---
    private static final int LABEL_X = 17;   // относительно leftPos
    private static final int LABEL_Y = 10;
    private static final int LABEL_W = 94;
    private static final int LABEL_H = 16;

    // --- Ползунок ---
    private static final int SLIDER_X     = 17;   // левый край области ползунка
    private static final int SLIDER_Y     = 28;
    private static final int SLIDER_LEN   = 94;   // длина дорожки
    private static final int SLIDER_H     = 8;

    // Текстура ручки ползунка (u=0, v=47, 15×8)
    private static final int KNOB_U  = 0;
    private static final int KNOB_V  = 47;
    private static final int KNOB_W  = 15;
    private static final int KNOB_H  = 8;

    // ===================== СОСТОЯНИЕ =====================
    private boolean isDragging = false;

    /** Текущее значение RPM, которое мы отображаем на клиенте (до отправки на сервер) */
    private int localRpm;

    public GUIMotorElectro(MotorElectroMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = WIN_W;
        this.imageHeight = WIN_H;
        // Отключаем заголовок и инвентарь игрока — GUI компактный
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        localRpm = menu.getTargetRpm();
    }

    // ===================== РЕНДЕР =====================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // Синхронизируем localRpm с сервером если не тащим ползунок
        if (!isDragging) {
            localRpm = menu.getTargetRpm();
        }

        int x = leftPos;
        int y = topPos;

        // ── Основной фон GUI (u=0,v=0, 126×46) ──
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 128, 128);

        // ── Ползунок: ручка ──
        // Позиция ручки: нормализуем localRpm [100..1000] → [0..1] → пиксели [0..(SLIDER_LEN-KNOB_W)]
        float t = (float)(localRpm - 100) / (1000f - 100f);
        int knobOffset = Math.round(t * (SLIDER_LEN - KNOB_W));
        int knobX = x + SLIDER_X + knobOffset;
        int knobY = y + SLIDER_Y;

        graphics.blit(TEXTURE, knobX, knobY, KNOB_U, KNOB_V, KNOB_W, KNOB_H, 128, 128);

        // ── Текст в плашке ──
        String rpmText = localRpm + " RPM";
        int textW = font.width(rpmText);
        int textX = x + LABEL_X + (LABEL_W - textW) / 2;
        int textY = y + LABEL_Y + (LABEL_H - 8) / 2;   // 8 = высота шрифта
        graphics.drawString(font, rpmText, textX, textY, 0xFFE0E0E0, false);
    }

    // ===================== ВВОД =====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverSlider(mouseX, mouseY)) {
            isDragging = true;
            updateFromMouse(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            updateFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging && button == 0) {
            isDragging = false;
            updateFromMouse(mouseX);
            // Отправляем пакет на сервер
            sendRpmPacket();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ =====================

    private boolean isOverSlider(double mouseX, double mouseY) {
        int ax = leftPos + SLIDER_X;
        int ay = topPos + SLIDER_Y;
        return mouseX >= ax && mouseX <= ax + SLIDER_LEN
                && mouseY >= ay && mouseY <= ay + SLIDER_H;
    }

    /**
     * Обновляет localRpm на основе позиции мыши вдоль дорожки ползунка.
     * Округляет до десятков и зажимает в [100, 1000].
     */
    private void updateFromMouse(double mouseX) {
        double relative = mouseX - (leftPos + SLIDER_X + KNOB_W / 2.0);
        float t = (float)(relative / (SLIDER_LEN - KNOB_W));
        t = Math.max(0f, Math.min(1f, t));

        // Линейная интерполяция 100..1000, шаг 10
        int raw = Math.round(t * (1000 - 100) + 100);
        localRpm = Math.round(raw / 10f) * 10;
        localRpm = Math.max(100, Math.min(1000, localRpm));
    }

    private void sendRpmPacket() {
        ModPacketHandler.INSTANCE.send(
                PacketDistributor.SERVER.noArg(),
                new SyncMotorRpmPacket(menu.blockEntity.getBlockPos(), localRpm)
        );
    }
}
