package com.trd.client.overlay.gui;

import com.trd.menu.industrial.FireboxMenu;
import com.trd.multiblock.industrial.FireboxBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран топки. Кнопки отправляют clickMenuButton через handleInventoryButtonClick.
 * Текстура: assets/trd/textures/gui/firebox_gui.png (256x256), окно 200x192.
 */
public class GUIFirebox extends AbstractContainerScreen<FireboxMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("trd", "textures/gui/machine/firebox_gui.png");

    // бары {x,y,w,h,u,v}
    private static final int[] ENERGY = {10, 20, 16, 64, 0, 192};
    private static final int[] TEMP   = {60, 20, 14, 64, 18, 192};
    private static final int[] PRESS  = {80, 20, 14, 64, 34, 192};
    private static final int[] WATER  = {108, 20, 16, 64, 50, 192};
    private static final int[] STEAM  = {128, 20, 16, 64, 68, 192};
    private static final int[] SMOKE  = {148, 20, 16, 64, 86, 192};
    // кнопки {x,y,w,h}
    private static final int[] HEAT_BTN = {172, 20, 18, 18};
    private static final int[] COAL_MINUS = {146, 86, 10, 10};
    private static final int[] COAL_PLUS  = {172, 86, 10, 10};
    private static final int[] WATER_MINUS = {146, 98, 10, 10};
    private static final int[] WATER_PLUS  = {172, 98, 10, 10};
    // uv спрайтов кнопок
    private static final int BTN_RED_U = 202, BTN_RED_V = 0;
    private static final int BTN_GREEN_U = 202, BTN_GREEN_V = 20;
    private static final int MINUS_U = 202, MINUS_V = 40;
    private static final int PLUS_U = 202, PLUS_V = 52;

    public GUIFirebox(FireboxMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 200;
        this.imageHeight = 192;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 20;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        drawBar(g, ENERGY, frac(menu.getEnergy(), menu.getMaxEnergy()));
        drawBar(g, TEMP, frac((long) (menu.getTemperature() * 100), (long) (FireboxBlockEntity.MAX_TEMP * 100)));
        drawBar(g, PRESS, frac(menu.getPressure(), menu.getMaxPressure()));
        drawBar(g, WATER, frac(menu.getWater(), FireboxBlockEntity.WATER_CAPACITY));
        drawBar(g, STEAM, frac(menu.getSteam(), FireboxBlockEntity.STEAM_CAPACITY));
        drawBar(g, SMOKE, frac(menu.getSmoke(), FireboxBlockEntity.SMOKE_CAPACITY));

        if (menu.isHeaterOn())
            g.blit(TEXTURE, leftPos + HEAT_BTN[0], topPos + HEAT_BTN[1], BTN_GREEN_U, BTN_GREEN_V, HEAT_BTN[2], HEAT_BTN[3]);
        else
            g.blit(TEXTURE, leftPos + HEAT_BTN[0], topPos + HEAT_BTN[1], BTN_RED_U, BTN_RED_V, HEAT_BTN[2], HEAT_BTN[3]);

        blitBtn(g, COAL_MINUS, MINUS_U, MINUS_V);
        blitBtn(g, COAL_PLUS, PLUS_U, PLUS_V);
        blitBtn(g, WATER_MINUS, MINUS_U, MINUS_V);
        blitBtn(g, WATER_PLUS, PLUS_U, PLUS_V);
    }

    private void blitBtn(GuiGraphics g, int[] r, int u, int v) {
        g.blit(TEXTURE, leftPos + r[0], topPos + r[1], u, v, r[2], r[3]);
    }

    private void drawBar(GuiGraphics g, int[] b, float frac) {
        frac = Math.max(0f, Math.min(1f, frac));
        int fh = (int) (b[3] * frac);
        if (fh <= 0) return;
        int off = b[3] - fh;
        g.blit(TEXTURE, leftPos + b[0], topPos + b[1] + off, b[4], b[5] + off, b[2], fh);
    }

    private static float frac(long v, long max) { return max <= 0 ? 0f : (float) v / (float) max; }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        int c = 0x404040;
        g.drawString(font, "Уголь: " + menu.getCoalFeed(), 100, 88, c, false);
        g.drawString(font, "Вода: " + menu.getWaterFeed(), 100, 100, c, false);
        g.drawString(font, menu.isHeaterOn() ? "ВКЛ" : "ВЫКЛ", 150, 40, menu.isHeaterOn() ? 0x2ecc40 : 0xd83c3c, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);
        renderBarTooltips(g, mx, my);
    }

    private void renderBarTooltips(GuiGraphics g, int mx, int my) {
        int lx = mx - leftPos, ly = my - topPos;
        if (in(lx, ly, ENERGY)) g.renderTooltip(font, Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " FE"), mx, my);
        else if (in(lx, ly, TEMP)) g.renderTooltip(font, Component.literal(String.format("%.0f / %.0f C", menu.getTemperature(), FireboxBlockEntity.MAX_TEMP)), mx, my);
        else if (in(lx, ly, PRESS)) g.renderTooltip(font, Component.literal(menu.getPressure() + " / " + menu.getMaxPressure()), mx, my);
        else if (in(lx, ly, WATER)) g.renderTooltip(font, Component.literal("Вода: " + menu.getWater() + " mB"), mx, my);
        else if (in(lx, ly, STEAM)) g.renderTooltip(font, Component.literal("Пар: " + menu.getSteam() + " mB"), mx, my);
        else if (in(lx, ly, SMOKE)) g.renderTooltip(font, Component.literal("Дым: " + menu.getSmoke() + " mB"), mx, my);
    }

    private boolean in(int lx, int ly, int[] r) {
        return lx >= r[0] && lx < r[0] + r[2] && ly >= r[1] && ly < r[1] + r[3];
    }

    @Override
    public boolean mouseClicked(double mxD, double myD, int btn) {
        int lx = (int) mxD - leftPos, ly = (int) myD - topPos;
        int id = -1;
        if (in(lx, ly, HEAT_BTN)) id = 0;
        else if (in(lx, ly, COAL_MINUS)) id = 1;
        else if (in(lx, ly, COAL_PLUS)) id = 2;
        else if (in(lx, ly, WATER_MINUS)) id = 3;
        else if (in(lx, ly, WATER_PLUS)) id = 4;
        if (id >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mxD, myD, btn);
    }
}
