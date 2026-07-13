package com.trd.client.overlay.gui;

;
import com.trd.menu.industrial.ConveyorBufferMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ConveyorBufferScreen extends AbstractContainerScreen<ConveyorBufferMenu> {
    private static final ResourceLocation GUI = new ResourceLocation("trd", "textures/gui/storage/buffer.png");

    public ConveyorBufferScreen(ConveyorBufferMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 173;
        this.titleLabelY = 6;
        this.inventoryLabelY = 79;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Фон GUI
        g.blit(GUI, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Светодиод: текстура на 177,0 → рендер на 11,19, размер 7×7
        if (menu.getBlockEntity() != null && menu.getBlockEntity().isConnectedToMachine()) {
            g.blit(GUI, x + 11, y + 19, 177, 0, 7, 7);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}