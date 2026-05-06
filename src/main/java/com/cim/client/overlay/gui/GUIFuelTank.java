package com.cim.client.overlay.gui;

import com.cim.api.fluids.ModFluids;
import com.cim.main.CrustalIncursionMod;
import com.cim.menu.FuelTankMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class GUIFuelTank extends AbstractContainerScreen<FuelTankMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CrustalIncursionMod.MOD_ID, "textures/gui/storage/fluid_tank_gui.png");
    // Используем ту же текстуру, что и у бочки (там есть кнопка и фон)

    private static final int TANK_X = 62;
    private static final int TANK_Y = 8;
    private static final int TANK_W = 34;
    private static final int TANK_H = 52;
    private static final int MODE_X = 41;
    private static final int MODE_Y = 45;
    private static final int MODE_SIZE = 15;

    public GUIFuelTank(FuelTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 148; // как у бочки
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -9999;
        this.inventoryLabelX = -9999;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderCustomTooltips(graphics, mouseX, mouseY);
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;

        if (relX >= TANK_X && relX < TANK_X + TANK_W && relY >= TANK_Y && relY < TANK_Y + TANK_H) {
            List<Component> tooltip = new ArrayList<>();
            FluidStack fluid = menu.getFluid();
            if (fluid.isEmpty()) {
                tooltip.add(Component.literal("Пусто").withStyle(ChatFormatting.GRAY));
            } else {
                MutableComponent fluidName = fluid.getDisplayName().copy();
                int tint = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor() | 0xFF000000;
                fluidName.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(tint)));
                tooltip.add(fluidName);
                tooltip.add(Component.literal(fluid.getAmount() + " / " + menu.getCapacity() + " mB")
                        .withStyle(ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        if (relX >= MODE_X && relX < MODE_X + MODE_SIZE && relY >= MODE_Y && relY < MODE_Y + MODE_SIZE) {
            List<Component> tooltip = List.of(
                    Component.literal("Режим:"),
                    Component.literal(switch (menu.getMode()) {
                        case 0 -> "§aВход / Выход (Оба)";
                        case 1 -> "§bТолько Вход";
                        case 2 -> "§6Только Выход";
                        default -> "§cОтключено";
                    })
            );
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Кнопка режима (такая же как в бочке)
        int mode = menu.getMode();
        graphics.blit(TEXTURE, x + MODE_X, y + MODE_Y, 177, mode * 16, MODE_SIZE, MODE_SIZE);

        renderFluid(graphics, x + TANK_X, y + TANK_Y, TANK_W, TANK_H);
    }

    private void renderFluid(GuiGraphics gui, int x, int y, int width, int height) {
        FluidStack fluid = menu.getFluid();
        if (fluid.isEmpty()) return;

        int capacity = menu.getCapacity();
        int fluidHeight = (int) (height * ((float) fluid.getAmount() / capacity));
        if (fluidHeight <= 0) return;

        ResourceLocation guiTexture = ModFluids.getGuiTexture(fluid.getFluid());
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int currentY = y + height - fluidHeight;
        for (int j = 0; j < fluidHeight; j += 16) {
            int segHeight = Math.min(fluidHeight - j, 16);
            int drawY = currentY + j;
            for (int i = 0; i < width; i += 16) {
                int segWidth = Math.min(width - i, 16);
                int drawX = x + i;
                gui.blit(guiTexture, drawX, drawY, 0, 0, segWidth, segHeight, 16, 16);
            }
        }
        // Линия поверхности
        int surfaceY = y + height - fluidHeight;
        gui.fill(x, surfaceY, x + width, surfaceY + 1, 0x40FFFFFF);
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY, MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE)) {
            playSound();
            com.cim.network.ModPacketHandler.INSTANCE.sendToServer(
                    new com.cim.network.packet.fluids.UpdateBarrelModeC2SPacket(menu.blockEntity.getBlockPos())
            );
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return (mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + w &&
                mouseY >= this.topPos + y && mouseY <= this.topPos + y + h);
    }

    private void playSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}