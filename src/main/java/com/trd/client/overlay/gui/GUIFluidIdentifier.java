package com.trd.client.overlay.gui;

import com.trd.api.fluids.ModFluids;
import com.trd.api.fluids.system.FluidDropItem;
import com.trd.api.fluids.system.FluidPropertyHelper;
import com.trd.item.tools.FluidIdentifierItem;
import com.trd.main.MainRegistry;
import com.trd.network.ModPacketHandler;
import com.trd.network.packet.fluids.ClearFluidHistoryPacket;
import com.trd.network.packet.fluids.SelectFluidPacket;
import com.trd.network.packet.fluids.ToggleFavoriteFluidPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class GUIFluidIdentifier extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/item/fluid_identifier_gui.png");
    private final ItemStack identifierStack;

    private static final int IMAGE_WIDTH = 153;
    private static final int IMAGE_HEIGHT = 229;
    private int leftPos, topPos;

    private final List<String> recentFluids = new ArrayList<>();
    private final List<String> favorites = new ArrayList<>();
    private final List<String> displayList = new ArrayList<>();

    private float scrollAmount = 0f;
    private int timerClear = 0;
    private int timerSearch = 0;
    private int cursorTimer = 0;
    private static final int PRESS_DURATION = 10;
    private static final int COLOR_INFO = 0xAEC6CF;
    private static final int COLOR_HAZARDOUS = 0xFFFF5555;
    private static final int COLOR_RADIOACTIVE = 0xFF55FF55;

    private boolean isDraggingScrollBar = false;
    private static final int SCROLLBAR_X = 123;      // относительно leftPos
    private static final int SCROLLBAR_Y = 75;       // относительно topPos
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 141; // высота трека
    private static final int THUMB_HEIGHT = 15;
    private EditBox searchBox;

    public GUIFluidIdentifier(ItemStack stack) {
        super(Component.literal("Fluid Identifier"));
        this.identifierStack = stack;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        this.recentFluids.clear();
        this.recentFluids.addAll(FluidIdentifierItem.getRecentFluids(identifierStack));
        this.favorites.clear();
        this.favorites.addAll(FluidIdentifierItem.getFavorites(identifierStack));

        this.searchBox = new EditBox(this.font, this.leftPos + 40, this.topPos + 12, 64, 15, Component.empty());
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(16);
        this.searchBox.setTextColor(0x00FFFFFF);
        this.searchBox.setFocused(true);
        this.searchBox.setResponder(text -> updateFluidList());

        updateFluidList();
    }

    @Override
    public void tick() {
        super.tick();
        if (timerClear > 0) timerClear--;
        if (timerSearch > 0) timerSearch--;
        cursorTimer++;
    }

    private String getFluidSearchString(Fluid fluid) {
        StringBuilder sb = new StringBuilder();

        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id != null) {
            String path = id.getPath().toLowerCase();
            sb.append(id.toString().toLowerCase()).append(" ");
            sb.append(path).append(" ");

            for (String part : path.split("_")) {
                sb.append(part).append(" ");
            }
        }

        sb.append(fluid.getFluidType().getDescription().getString().toLowerCase()).append(" ");

        FluidStack stack = new FluidStack(fluid, 1000);
        int corrosivity = FluidPropertyHelper.getCorrosivity(stack);
        int temperature = FluidPropertyHelper.getTemperature(stack);

        if (corrosivity > 0) {
            sb.append("corrosive acid кислота коррозия едкий ");
            if (corrosivity >= 2) sb.append("strong сильный ");
        }
        if (temperature > 500) sb.append("hot heat горячий пар steam ");
        if (temperature < 273) sb.append("cold ice холодный лед ");

        int baseTemp = fluid.getFluidType().getTemperature();
        if (baseTemp > 1000) sb.append("lava магма magma ");
        if (fluid.getFluidType().getDensity() < 0) sb.append("gas газ пар steam ");

        return sb.toString();
    }

    private void updateFluidList() {
        displayList.clear();
        String search = searchBox.getValue().toLowerCase().trim();

        for (String fav : favorites) {
            if (fav.equals("none")) {
                displayList.add(fav);
                continue;
            }
            Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(fav));
            if (fluid == null || !fluid.defaultFluidState().isSource()) continue;
            if (search.isEmpty() || getFluidSearchString(fluid).contains(search)) {
                displayList.add(fav);
            }
        }

        boolean noneInFavorites = favorites.contains("none");
        boolean shouldAddNone = !noneInFavorites && (search.isEmpty() || "none ничего пусто empty".contains(search));
        if (shouldAddNone) {
            displayList.add("none");
        }

        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY || !fluid.defaultFluidState().isSource()) continue;
            String id = BuiltInRegistries.FLUID.getKey(fluid).toString();
            if (displayList.contains(id)) continue;
            if (search.isEmpty() || getFluidSearchString(fluid).contains(search)) {
                displayList.add(id);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int x = this.leftPos;
        int y = this.topPos;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, x, y, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        if (timerSearch > 0) graphics.blit(TEXTURE, x + 22, y + 9, 167, 80, 15, 15);
        if (timerClear > 0) graphics.blit(TEXTURE, x + 105, y + 33, 154, 80, 12, 33);

        Component recentTooltip = renderRecentFluids(graphics, x, y, mouseX, mouseY);
        renderScrollableList(graphics, x, y, mouseX, mouseY);
        renderScrollBar(graphics, x, y);

        String content = searchBox.getValue();
        boolean focused = searchBox.isFocused();
        String cursorSymbol = (focused && (cursorTimer / 10 % 2 == 0)) ? "_" : "";
        String fullText = content + cursorSymbol;
        if (this.font.width(fullText) > 60) {
            fullText = this.font.plainSubstrByWidth(fullText, 60, true);
        }
        graphics.drawString(this.font, fullText, searchBox.getX(), searchBox.getY(), COLOR_INFO, false);

        if (recentTooltip != null) {
            graphics.renderTooltip(this.font, recentTooltip, mouseX, mouseY);
        }
    }

    private Component renderRecentFluids(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        Component tooltipToRender = null;

        for (int i = 0; i < recentFluids.size(); i++) {
            if (i >= 10) break;
            String fluidId = recentFluids.get(i);
            if (fluidId == null || fluidId.isEmpty() || fluidId.equals("null")) continue;

            int drawX = x + 22 + ((i % 5) * 16);
            int drawY = y + 33 + ((i / 5) * 17);

            renderFluidDropIcon(graphics, fluidId, drawX, drawY);

            if (mouseX >= drawX && mouseX < drawX + 16 && mouseY >= drawY && mouseY < drawY + 16) {
                tooltipToRender = getFluidDisplayName(fluidId);
                if (tooltipToRender.getString().trim().isEmpty()) {
                    tooltipToRender = Component.literal("Unknown");
                }
            }
        }
        return tooltipToRender;
    }

    private Component getFluidDisplayName(String fluidId) {
        Component name;
        if (fluidId.equals("none")) {
            name = Component.translatable("tooltip.trd.no_fluid");
        } else {
            Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(fluidId));
            if (fluid != null) {
                name = fluid.getFluidType().getDescription().copy();
            } else {
                name = Component.literal(fluidId.replace("minecraft:", "").replace("trd:", ""));
            }
        }
        int color = getFluidColor(fluidId);
        TextColor textColor = TextColor.fromRgb(color);
        return name.copy().withStyle(style -> style.withColor(textColor));
    }

    private void renderScrollableList(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int listX = x + 22;
        int listY = y + 75;

        // 1. ИСПРАВЛЕНО: Теперь это список, и мы будем использовать его для отрисовки
        List<Component> tooltipToRender = null;

        graphics.enableScissor(listX, listY, listX + 99, listY + 141);

        int maxScroll = Math.max(0, (displayList.size() * 19) - 141);
        int currentOffset = (int) (scrollAmount * maxScroll);
        String selectedFluid = FluidIdentifierItem.getSelectedFluid(identifierStack);

        for (int i = 0; i < displayList.size(); i++) {
            String fluidId = displayList.get(i);
            int entryY = listY + (i * 19) - currentOffset;

            if (entryY + 19 < listY || entryY > listY + 141) continue;

            boolean isFav = favorites.contains(fluidId);
            boolean isCurrent = fluidId.equals(selectedFluid);
            int vOffset = isCurrent ? (isFav ? 60 : 40) : (isFav ? 20 : 0);

            graphics.blit(TEXTURE, listX, entryY, 154, vOffset, 99, 19);

            int iconX = listX + 3;
            int iconY = entryY + 3;
            int iconSize = 13;

            renderFluidDropIcon(graphics, fluidId, iconX, iconY);

            // 2. ИСПРАВЛЕНО: Проверка наведения
            if (mouseX >= iconX && mouseX <= iconX + iconSize && mouseY >= iconY && mouseY <= iconY + iconSize) {
                List<Component> fullTooltip = new ArrayList<>();

                if (identifierStack.getItem() instanceof FluidIdentifierItem item) {
                    fullTooltip.add(item.getFluidDisplayName(fluidId));
                    fullTooltip.add(Component.literal(""));
                }

                Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidId));
                if (fluid != null) {
                    // Вызываем наш новый статический метод из предмета капли
                    fullTooltip.addAll(FluidDropItem.getFluidPropertiesTooltip(fluid.getFluidType()));
                }

                // Записываем в локальную переменную, которую отрисуем после disableScissor
                tooltipToRender = fullTooltip;
            }

            // Рендер индикаторов опасности
            if (!fluidId.equals("none")) {
                Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(fluidId));
                if (fluid != null) {
                    FluidStack fStack = new FluidStack(fluid, 1000);
                    int cx = listX + 88;
                    if (FluidPropertyHelper.getCorrosivity(fStack) > 0) {
                        graphics.fill(cx, entryY + 4, cx + 2, entryY + 6, COLOR_HAZARDOUS);
                        cx -= 3;
                    }
                }
            }

            Component name = getFluidDisplayName(fluidId);
            graphics.drawString(this.font, name, listX + 20, entryY + 5, getFluidColor(fluidId), false);
        }

        graphics.disableScissor();

        // 3. ИСПРАВЛЕНО: Отрисовка списка компонентов
        if (tooltipToRender != null) {
            // Используем renderComponentTooltip для списков (List<Component>)
            graphics.renderComponentTooltip(this.font, tooltipToRender, mouseX, mouseY);
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x = this.leftPos;
        int y = this.topPos;

        // === ПРОВЕРКА КЛИКА ПО ПОЛЗУНКУ ===
        int trackX = x + SCROLLBAR_X;
        int trackY = y + SCROLLBAR_Y;
        int thumbY = trackY + (int) (scrollAmount * (SCROLLBAR_HEIGHT - THUMB_HEIGHT));

        // Попали в ползунок (thumb)?
        if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH &&
                mouseY >= thumbY && mouseY <= thumbY + THUMB_HEIGHT) {
            isDraggingScrollBar = true;
            return true;
        }

        // Попали в трек (клик выше/ниже ползунка — прыжок)?
        if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH &&
                mouseY >= trackY && mouseY <= trackY + SCROLLBAR_HEIGHT) {
            // Прыгаем к позиции клика
            float clickRatio = (float) ((mouseY - trackY) / (double) (SCROLLBAR_HEIGHT - THUMB_HEIGHT));
            scrollAmount = Math.max(0f, Math.min(1f, clickRatio));
            return true;
        }

        if (mouseX >= x + 105 && mouseX <= x + 117 && mouseY >= y + 33 && mouseY <= y + 66) {
            timerClear = PRESS_DURATION;
            playClickSound();
            ModPacketHandler.INSTANCE.sendToServer(new ClearFluidHistoryPacket());
            this.recentFluids.clear();
            return true;
        }

        int listX = x + 22;
        int listY = y + 75;
        if (mouseX >= listX && mouseX <= listX + 99 && mouseY >= listY && mouseY <= listY + 141) {
            int maxScroll = Math.max(0, (displayList.size() * 19) - 141);
            int currentOffset = (int) (scrollAmount * maxScroll);
            int clickedIndex = (int) ((mouseY - listY + currentOffset) / 19);

            if (clickedIndex >= 0 && clickedIndex < displayList.size()) {
                String fluid = displayList.get(clickedIndex);
                int entryY = listY + (clickedIndex * 19) - currentOffset;
                playClickSound();

                if (mouseX >= listX + 88 && mouseX <= listX + 97 && mouseY >= entryY + 4 && mouseY <= entryY + 14) {
                    toggleFavorite(fluid);
                } else {
                    selectFluid(fluid);
                }
                return true;
            }
        }

        for (int i = 0; i < recentFluids.size(); i++) {
            int drawX = x + 22 + ((i % 5) * 16);
            int drawY = y + 33 + ((i / 5) * 17);
            if (mouseX >= drawX && mouseX < drawX + 16 && mouseY >= drawY && mouseY < drawY + 16) {
                playClickSound();
                selectFluid(recentFluids.get(i));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollBar && button == 0) {
            int trackY = this.topPos + SCROLLBAR_Y;
            float dragRatio = (float) ((mouseY - trackY) / (double) (SCROLLBAR_HEIGHT - THUMB_HEIGHT));
            scrollAmount = Math.max(0f, Math.min(1f, dragRatio));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollBar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void selectFluid(String fluid) {
        ModPacketHandler.INSTANCE.sendToServer(new SelectFluidPacket(fluid));
        identifierStack.getOrCreateTag().putString("SelectedFluid", fluid);

        recentFluids.remove(fluid);
        recentFluids.add(0, fluid);
        if (recentFluids.size() > 10) recentFluids.remove(10);
    }

    private void toggleFavorite(String fluid) {
        ModPacketHandler.INSTANCE.sendToServer(new ToggleFavoriteFluidPacket(fluid));
        if (favorites.contains(fluid)) favorites.remove(fluid);
        else favorites.add(fluid);
        updateFluidList();
    }

    private void renderFluidDropIcon(GuiGraphics graphics, String id, int x, int y) {
        ItemStack dropStack;

        if (id.equals("none")) {
            // Новая серая капля
            dropStack = new ItemStack(ModFluids.FLUID_DROP_NONE.get());
        } else {
            Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(id));
            if (fluid != null) {
                Item dropItem = ModFluids.getFluidDrop(fluid.getFluidType());
                if (dropItem != null) {
                    dropStack = new ItemStack(dropItem);
                } else {
                    // Fallback – сам идентификатор с NBT (сторонняя жидкость)
                    dropStack = new ItemStack(identifierStack.getItem());
                    dropStack.getOrCreateTag().putString("SelectedFluid", id);
                }
            } else {
                // Неизвестный id – падаем на идентификатор
                dropStack = new ItemStack(identifierStack.getItem());
                dropStack.getOrCreateTag().putString("SelectedFluid", id);
            }
        }

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(13f / 16f, 13f / 16f, 1f);
        graphics.renderItem(dropStack, 0, 0);
        graphics.pose().popPose();
    }

    private int getFluidColor(String id) {
        if (id.equals("none")) return 0xFFAAAAAA;
        if (id.equals("minecraft:lava")) return 0xFFFF5500;
        Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(id));
        return (fluid != null) ? IClientFluidTypeExtensions.of(fluid).getTintColor() : 0xFFDDDDDD;
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void renderScrollBar(GuiGraphics graphics, int x, int y) {
        int trackX = x + SCROLLBAR_X;
        int trackY = y + SCROLLBAR_Y;

        // Трек (опционально, если есть в текстуре)
        // graphics.blit(TEXTURE, trackX, trackY, 215, 95, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);

        // Ползунок (thumb)
        int thumbY = trackY + (int) (scrollAmount * (SCROLLBAR_HEIGHT - THUMB_HEIGHT));
        graphics.blit(TEXTURE, trackX, thumbY, 215, 80, SCROLLBAR_WIDTH, THUMB_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        int ms = Math.max(0, (displayList.size() * 19) - 141);
        if (ms > 0) {
            scrollAmount = Math.max(0f, Math.min(1f, scrollAmount - (float) (d * 19 / ms)));
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int m) {
        return searchBox.charTyped(c, m);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        return searchBox.keyPressed(k, s, m) || super.keyPressed(k, s, m);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}