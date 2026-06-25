package com.trd.client.overlay.gui;

import com.trd.item.ModItems;
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
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

public class GUIHeater extends AbstractContainerScreen<HeaterMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MainRegistry.MOD_ID, "textures/gui/machine/heater_gui.png");

    private static final List<ItemStack>[] TIER_ITEMS = new List[6];

    static {
        // ========== ТИР 0: Дешёвое деревянное топливо ==========
        TIER_ITEMS[0] = Arrays.asList(
                new ItemStack(Items.STICK), new ItemStack(Items.SCAFFOLDING),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.SPRUCE_PLANKS),
                new ItemStack(Items.BIRCH_PLANKS), new ItemStack(Items.JUNGLE_PLANKS),
                new ItemStack(Items.ACACIA_PLANKS), new ItemStack(Items.DARK_OAK_PLANKS),
                new ItemStack(Items.MANGROVE_PLANKS), new ItemStack(Items.CHERRY_PLANKS),
                new ItemStack(Items.BAMBOO_PLANKS), new ItemStack(Items.BAMBOO_MOSAIC),
                new ItemStack(Items.OAK_SLAB), new ItemStack(Items.SPRUCE_SLAB),
                new ItemStack(Items.BIRCH_SLAB), new ItemStack(Items.JUNGLE_SLAB),
                new ItemStack(Items.ACACIA_SLAB), new ItemStack(Items.DARK_OAK_SLAB),
                new ItemStack(Items.MANGROVE_SLAB), new ItemStack(Items.CHERRY_SLAB),
                new ItemStack(Items.BAMBOO_SLAB), new ItemStack(Items.BAMBOO_MOSAIC_SLAB),
                new ItemStack(Items.OAK_STAIRS), new ItemStack(Items.SPRUCE_STAIRS),
                new ItemStack(Items.BIRCH_STAIRS), new ItemStack(Items.JUNGLE_STAIRS),
                new ItemStack(Items.ACACIA_STAIRS), new ItemStack(Items.DARK_OAK_STAIRS),
                new ItemStack(Items.MANGROVE_STAIRS), new ItemStack(Items.CHERRY_STAIRS),
                new ItemStack(Items.BAMBOO_STAIRS), new ItemStack(Items.BAMBOO_MOSAIC_STAIRS),
                new ItemStack(Items.OAK_FENCE), new ItemStack(Items.SPRUCE_FENCE),
                new ItemStack(Items.BIRCH_FENCE), new ItemStack(Items.JUNGLE_FENCE),
                new ItemStack(Items.ACACIA_FENCE), new ItemStack(Items.DARK_OAK_FENCE),
                new ItemStack(Items.MANGROVE_FENCE), new ItemStack(Items.CHERRY_FENCE),
                new ItemStack(Items.BAMBOO_FENCE),
                new ItemStack(Items.OAK_FENCE_GATE), new ItemStack(Items.SPRUCE_FENCE_GATE),
                new ItemStack(Items.BIRCH_FENCE_GATE), new ItemStack(Items.JUNGLE_FENCE_GATE),
                new ItemStack(Items.ACACIA_FENCE_GATE), new ItemStack(Items.DARK_OAK_FENCE_GATE),
                new ItemStack(Items.MANGROVE_FENCE_GATE), new ItemStack(Items.CHERRY_FENCE_GATE),
                new ItemStack(Items.BAMBOO_FENCE_GATE),
                new ItemStack(Items.OAK_DOOR), new ItemStack(Items.SPRUCE_DOOR),
                new ItemStack(Items.BIRCH_DOOR), new ItemStack(Items.JUNGLE_DOOR),
                new ItemStack(Items.ACACIA_DOOR), new ItemStack(Items.DARK_OAK_DOOR),
                new ItemStack(Items.MANGROVE_DOOR), new ItemStack(Items.CHERRY_DOOR),
                new ItemStack(Items.BAMBOO_DOOR),
                new ItemStack(Items.OAK_TRAPDOOR), new ItemStack(Items.SPRUCE_TRAPDOOR),
                new ItemStack(Items.BIRCH_TRAPDOOR), new ItemStack(Items.JUNGLE_TRAPDOOR),
                new ItemStack(Items.ACACIA_TRAPDOOR), new ItemStack(Items.DARK_OAK_TRAPDOOR),
                new ItemStack(Items.MANGROVE_TRAPDOOR), new ItemStack(Items.CHERRY_TRAPDOOR),
                new ItemStack(Items.BAMBOO_TRAPDOOR),
                new ItemStack(Items.OAK_BUTTON), new ItemStack(Items.SPRUCE_BUTTON),
                new ItemStack(Items.BIRCH_BUTTON), new ItemStack(Items.JUNGLE_BUTTON),
                new ItemStack(Items.ACACIA_BUTTON), new ItemStack(Items.DARK_OAK_BUTTON),
                new ItemStack(Items.MANGROVE_BUTTON), new ItemStack(Items.CHERRY_BUTTON),
                new ItemStack(Items.BAMBOO_BUTTON),
                new ItemStack(Items.OAK_PRESSURE_PLATE), new ItemStack(Items.SPRUCE_PRESSURE_PLATE),
                new ItemStack(Items.BIRCH_PRESSURE_PLATE), new ItemStack(Items.JUNGLE_PRESSURE_PLATE),
                new ItemStack(Items.ACACIA_PRESSURE_PLATE), new ItemStack(Items.DARK_OAK_PRESSURE_PLATE),
                new ItemStack(Items.MANGROVE_PRESSURE_PLATE), new ItemStack(Items.CHERRY_PRESSURE_PLATE),
                new ItemStack(Items.BAMBOO_PRESSURE_PLATE),
                new ItemStack(Items.OAK_SIGN), new ItemStack(Items.SPRUCE_SIGN),
                new ItemStack(Items.BIRCH_SIGN), new ItemStack(Items.JUNGLE_SIGN),
                new ItemStack(Items.ACACIA_SIGN), new ItemStack(Items.DARK_OAK_SIGN),
                new ItemStack(Items.MANGROVE_SIGN), new ItemStack(Items.CHERRY_SIGN),
                new ItemStack(Items.BAMBOO_SIGN), new ItemStack(Items.OAK_HANGING_SIGN),
                new ItemStack(Items.SPRUCE_HANGING_SIGN), new ItemStack(Items.BIRCH_HANGING_SIGN),
                new ItemStack(Items.JUNGLE_HANGING_SIGN), new ItemStack(Items.ACACIA_HANGING_SIGN),
                new ItemStack(Items.DARK_OAK_HANGING_SIGN), new ItemStack(Items.MANGROVE_HANGING_SIGN),
                new ItemStack(Items.CHERRY_HANGING_SIGN), new ItemStack(Items.BAMBOO_HANGING_SIGN),
                new ItemStack(Items.OAK_LOG), new ItemStack(Items.SPRUCE_LOG),
                new ItemStack(Items.BIRCH_LOG), new ItemStack(Items.JUNGLE_LOG),
                new ItemStack(Items.ACACIA_LOG), new ItemStack(Items.DARK_OAK_LOG),
                new ItemStack(Items.MANGROVE_LOG), new ItemStack(Items.CHERRY_LOG),
                new ItemStack(Items.BAMBOO_BLOCK), new ItemStack(Items.STRIPPED_BAMBOO_BLOCK),
                new ItemStack(Items.STRIPPED_OAK_LOG), new ItemStack(Items.STRIPPED_SPRUCE_LOG),
                new ItemStack(Items.STRIPPED_BIRCH_LOG), new ItemStack(Items.STRIPPED_JUNGLE_LOG),
                new ItemStack(Items.STRIPPED_ACACIA_LOG), new ItemStack(Items.STRIPPED_DARK_OAK_LOG),
                new ItemStack(Items.STRIPPED_MANGROVE_LOG), new ItemStack(Items.STRIPPED_CHERRY_LOG),
                new ItemStack(Items.OAK_WOOD), new ItemStack(Items.SPRUCE_WOOD),
                new ItemStack(Items.BIRCH_WOOD), new ItemStack(Items.JUNGLE_WOOD),
                new ItemStack(Items.ACACIA_WOOD), new ItemStack(Items.DARK_OAK_WOOD),
                new ItemStack(Items.MANGROVE_WOOD), new ItemStack(Items.CHERRY_WOOD),
                new ItemStack(Items.STRIPPED_OAK_WOOD), new ItemStack(Items.STRIPPED_SPRUCE_WOOD),
                new ItemStack(Items.STRIPPED_BIRCH_WOOD), new ItemStack(Items.STRIPPED_JUNGLE_WOOD),
                new ItemStack(Items.STRIPPED_ACACIA_WOOD), new ItemStack(Items.STRIPPED_DARK_OAK_WOOD),
                new ItemStack(Items.STRIPPED_MANGROVE_WOOD), new ItemStack(Items.STRIPPED_CHERRY_WOOD),
                new ItemStack(Items.BOWL), new ItemStack(Items.NOTE_BLOCK),
                new ItemStack(Items.JUKEBOX), new ItemStack(Items.BOOKSHELF),
                new ItemStack(Items.CHISELED_BOOKSHELF), new ItemStack(Items.COMPOSTER),
                new ItemStack(Items.BARREL), new ItemStack(Items.CRAFTING_TABLE),
                new ItemStack(Items.CHEST), new ItemStack(Items.TRAPPED_CHEST),
                new ItemStack(Items.OAK_BOAT), new ItemStack(Items.SPRUCE_BOAT),
                new ItemStack(Items.BIRCH_BOAT), new ItemStack(Items.JUNGLE_BOAT),
                new ItemStack(Items.ACACIA_BOAT), new ItemStack(Items.DARK_OAK_BOAT),
                new ItemStack(Items.MANGROVE_BOAT), new ItemStack(Items.CHERRY_BOAT),
                new ItemStack(Items.BAMBOO_RAFT), new ItemStack(Items.OAK_CHEST_BOAT),
                new ItemStack(Items.SPRUCE_CHEST_BOAT), new ItemStack(Items.BIRCH_CHEST_BOAT),
                new ItemStack(Items.JUNGLE_CHEST_BOAT), new ItemStack(Items.ACACIA_CHEST_BOAT),
                new ItemStack(Items.DARK_OAK_CHEST_BOAT), new ItemStack(Items.MANGROVE_CHEST_BOAT),
                new ItemStack(Items.CHERRY_CHEST_BOAT), new ItemStack(Items.BAMBOO_CHEST_RAFT),
                new ItemStack(Items.FLETCHING_TABLE), new ItemStack(Items.SMITHING_TABLE),
                new ItemStack(Items.CARTOGRAPHY_TABLE), new ItemStack(Items.LOOM),
                new ItemStack(Items.ITEM_FRAME), new ItemStack(Items.GLOW_ITEM_FRAME),
                new ItemStack(Items.PAINTING),
                new ItemStack(Items.WHITE_BED), new ItemStack(Items.ORANGE_BED),
                new ItemStack(Items.MAGENTA_BED), new ItemStack(Items.LIGHT_BLUE_BED),
                new ItemStack(Items.YELLOW_BED), new ItemStack(Items.LIME_BED),
                new ItemStack(Items.PINK_BED), new ItemStack(Items.GRAY_BED),
                new ItemStack(Items.LIGHT_GRAY_BED), new ItemStack(Items.CYAN_BED),
                new ItemStack(Items.PURPLE_BED), new ItemStack(Items.BLUE_BED),
                new ItemStack(Items.BROWN_BED), new ItemStack(Items.GREEN_BED),
                new ItemStack(Items.RED_BED), new ItemStack(Items.BLACK_BED),
                new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.WOODEN_PICKAXE),
                new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_SHOVEL),
                new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.SHIELD),
                new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW),
                new ItemStack(Items.FISHING_ROD),
                new ItemStack(Items.CAMPFIRE), new ItemStack(Items.SOUL_CAMPFIRE),
                new ItemStack(Items.TORCH), new ItemStack(Items.SOUL_TORCH),
                new ItemStack(Items.REDSTONE_TORCH)
        );

        // ========== ТИР 1: Обычное топливо ==========
        TIER_ITEMS[1] = Arrays.asList(
                new ItemStack(Items.COAL),
                new ItemStack(Items.CHARCOAL),
                new ItemStack(Items.BLAZE_POWDER)
        );

        // ========== ТИР 2: Blaze rod и прочее ==========
        TIER_ITEMS[2] = Arrays.asList(
                new ItemStack(Items.BLAZE_ROD),
                new ItemStack(Items.MAGMA_CREAM),
                new ItemStack(Items.PORKCHOP)
        );

        // ========== ТИР 3: Блок угля ==========
        TIER_ITEMS[3] = Arrays.asList(
                new ItemStack(Items.COAL_BLOCK)
        );

        // ========== ТИР 4: Лава ==========
        TIER_ITEMS[4] = Arrays.asList(
                new ItemStack(Items.LAVA_BUCKET)
        );

        // ========== ТИР 5: Специальное ==========
        TIER_ITEMS[5] = Arrays.asList(
                new ItemStack(ModItems.MORY_LAH.get()),
                new ItemStack(Items.DRAGON_BREATH)
        );
    }

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

            // === ОКРУГЛЕНИЕ ДО ЦЕЛЫХ ===
            Component tempText = Component.literal(String.format("%.0f / %.0f °C", temp, maxTemp))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));

            guiGraphics.renderTooltip(this.font, tempText, mouseX, mouseY);
        }

        // Тултип индикатора работы — секунды без десятых
        if (this.isHovering(104, 25, 18, 18, mouseX, mouseY)) {
            if (menu.isBurning()) {
                int seconds = menu.getBurnTime() / 20;
                int totalSeconds = menu.getTotalBurnTime() / 20;

                // === БЕЗ ДЕСЯТЫХ ===
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
        String[] lines = {
                "§6§lТопливные тиры:",
                "§8Тир 0: §f1°C, §f6.25§7с.",
                "§8Тир 1: §f2°C, §f12.5§7с.",
                "§8Тир 2: §f3°C, §f25§7с.",
                "§8Тир 3: §f4°C, §f40§7с.",
                "§8Тир 4: §f6°C, §f60§7с.",
                "§8Тир 5: §f8°C, §f120§7с."
        };


        int lineHeight = 11;
        int padding = 4;
        int iconSize = 12;
        int iconTextGap = 2;

        int maxTextWidth = 0;
        for (String line : lines) {
            maxTextWidth = Math.max(maxTextWidth, this.font.width(line));
        }

        int tooltipWidth = padding + iconSize + iconTextGap + maxTextWidth + padding;
        int tooltipHeight = lines.length * lineHeight + padding * 2;

        int tooltipX = mouseX + 8;
        int tooltipY = mouseY - tooltipHeight / 2;

        if (tooltipX + tooltipWidth > this.width) tooltipX = mouseX - tooltipWidth - 8;
        if (tooltipY < 4) tooltipY = 4;
        if (tooltipY + tooltipHeight > this.height) tooltipY = this.height - tooltipHeight - 4;

        guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF0100010);
        guiGraphics.fill(tooltipX + 1, tooltipY, tooltipX + tooltipWidth - 1, tooltipY + 1, 0xF0500070);
        guiGraphics.fill(tooltipX + 1, tooltipY + tooltipHeight - 1, tooltipX + tooltipWidth - 1, tooltipY + tooltipHeight, 0xF0500070);
        guiGraphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + tooltipHeight, 0xF0500070);
        guiGraphics.fill(tooltipX + tooltipWidth - 1, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF0500070);

        long currentSecond = System.currentTimeMillis() / 1000;

        for (int i = 0; i < lines.length; i++) {
            int lineY = tooltipY + padding + i * lineHeight;

            if (i == 0) {
                guiGraphics.drawString(this.font, lines[i], tooltipX + padding, lineY + 2, 0xFFFFFF, true);
            } else {
                int tier = i - 1;

                List<ItemStack> items = TIER_ITEMS[tier];
                if (items != null && !items.isEmpty()) {
                    int itemIndex = (int)((currentSecond + tier) % items.size());
                    ItemStack stack = items.get(itemIndex);

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(tooltipX + padding, lineY, 100);
                    guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
                    guiGraphics.renderItem(stack, 0, 0);
                    guiGraphics.renderItemDecorations(this.font, stack, 0, 0);
                    guiGraphics.pose().popPose();
                }

                int textX = tooltipX + padding + iconSize + iconTextGap;
                guiGraphics.drawString(this.font, lines[i], textX, lineY + 2, 0xFFFFFF, true);
            }
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