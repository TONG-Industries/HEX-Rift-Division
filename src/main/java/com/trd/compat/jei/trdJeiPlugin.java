package com.trd.compat.jei;

import com.trd.api.metallurgy.system.Metal;
import com.trd.api.metallurgy.system.MetalUnits2;
import com.trd.api.metallurgy.system.MetallurgyRegistry;
import com.trd.api.metallurgy.system.recipe.AlloyRecipe;
import com.trd.api.metallurgy.system.recipe.AlloySlot;
import com.trd.api.metallurgy.system.recipe.MoldRecipe;
import com.trd.api.metallurgy.system.recipe.MoldRecipeRegistry;
import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.industrial.MillstoneBlockEntity;
import com.trd.event.SlagItem;
import com.trd.item.ModItems;
import com.trd.main.MainRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JeiPlugin
public class trdJeiPlugin implements IModPlugin {

    public static final ResourceLocation UID = new ResourceLocation(MainRegistry.MOD_ID, "jei_plugin");

    public static final RecipeType<SmeltingWrapper> SMELTING_TYPE =
            RecipeType.create(MainRegistry.MOD_ID, "smelting", SmeltingWrapper.class);
    public static final RecipeType<CastingWrapper> CASTING_TYPE =
            RecipeType.create(MainRegistry.MOD_ID, "casting", CastingWrapper.class);
    public static final RecipeType<AlloyingWrapper> ALLOYING_TYPE =
            RecipeType.create(MainRegistry.MOD_ID, "alloying", AlloyingWrapper.class);
    public static final RecipeType<MillstoneWrapper> MILLSTONE_TYPE =
            RecipeType.create(MainRegistry.MOD_ID, "millstone", MillstoneWrapper.class);

    public record SmeltingWrapper(ItemStack input, Metal metal, int outputUnits, int temp, float heatConsumption, int timeTicks) {}
    public record CastingWrapper(MoldRecipe mold, Metal metal, ItemStack output, int requiredUnits) {}
    public record AlloyingWrapper(AlloyRecipe recipe) {}
    public record MillstoneWrapper(Item input, List<ItemStack> outputs, int grindsRequired) {}

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new SmeltingCategory(guiHelper));
        registration.addRecipeCategories(new CastingCategory(guiHelper));
        registration.addRecipeCategories(new AlloyingCategory(guiHelper));
        registration.addRecipeCategories(new MillstoneCategory(guiHelper));
    }



    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.LIQUID_METAL.get(),
                (stack, context) -> {
                    if (stack.hasTag() && stack.getTag().contains("MetalId")) {
                        return stack.getTag().getString("MetalId");
                    }
                    return IIngredientSubtypeInterpreter.NONE;
                });
    }


    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<SmeltingWrapper> smeltingRecipes = new ArrayList<>();

        for (var recipe : MetallurgyRegistry.getAllSmeltRecipes()) {
            smeltingRecipes.add(new SmeltingWrapper(
                    new ItemStack(recipe.input()),
                    recipe.output(),
                    recipe.outputUnits(),
                    recipe.minTemp(),
                    recipe.heatConsumption(),
                    recipe.smeltTimeTicks()
            ));
        }

        for (Metal metal : MetallurgyRegistry.getAllMetals()) {
            ItemStack slag = SlagItem.createSlag(metal, MetalUnits2.UNITS_PER_INGOT);
            smeltingRecipes.add(new SmeltingWrapper(
                    slag,
                    metal,
                    MetalUnits2.UNITS_PER_INGOT,
                    metal.getMeltingPoint(),
                    metal.getHeatConsumptionPerTick(),
                    metal.calculateSmeltTimeForUnits(MetalUnits2.UNITS_PER_INGOT)
            ));
        }

        registration.addRecipes(SMELTING_TYPE, smeltingRecipes);

        List<CastingWrapper> castingRecipes = new ArrayList<>();
        for (MoldRecipe mold : MoldRecipeRegistry.getAllRecipes()) {
            for (Metal metal : MetallurgyRegistry.getAllMetals()) {
                ItemStack output = mold.createOutput(metal);
                if (!output.isEmpty()) {
                    castingRecipes.add(new CastingWrapper(mold, metal, output, mold.getRequiredUnits()));
                }
            }
        }
        registration.addRecipes(CASTING_TYPE, castingRecipes);

        List<AlloyingWrapper> alloyingRecipes = new ArrayList<>();
        for (AlloyRecipe recipe : MetallurgyRegistry.getAllAlloyRecipes()) {
            alloyingRecipes.add(new AlloyingWrapper(recipe));
        }
        registration.addRecipes(ALLOYING_TYPE, alloyingRecipes);

        // === ЖЕРНОВА ===
        List<MillstoneWrapper> millstoneRecipes = new ArrayList<>();
        for (Map.Entry<Item, MillstoneBlockEntity.GrindRecipe> entry : MillstoneBlockEntity.RECIPES.entrySet()) {
            // Копируем стаки, чтобы JEI не мог их испортить при индексации
            List<ItemStack> outputs = entry.getValue().outputs().stream()
                    .map(ItemStack::copy)
                    .toList();
            millstoneRecipes.add(new MillstoneWrapper(
                    entry.getKey(),
                    outputs,
                    entry.getValue().grindsRequired()
            ));
        }
        registration.addRecipes(MILLSTONE_TYPE, millstoneRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMELTER.get()), SMELTING_TYPE, ALLOYING_TYPE, CASTING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMALL_SMELTER.get()), SMELTING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(com.trd.block.basic.ModBlocks.JERNOVA.get()), MILLSTONE_TYPE);
    }

    private static ItemStack createLiquidMetalStack(Metal metal, int amount) {
        ItemStack stack = new ItemStack(ModItems.LIQUID_METAL.get());
        stack.getOrCreateTag().putString("MetalId", metal.getId().toString());
        stack.getTag().putInt("Amount", amount);
        stack.getTag().putInt("MetalColor", metal.getColor());
        return stack;
    }








    public static class MillstoneCategory implements IRecipeCategory<MillstoneWrapper> {
        private final IDrawable background;
        private final IDrawable icon;
        private final Component title;

        public MillstoneCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createDrawable(
                    new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/jei/jei_universal_gui.png"),
                    0, 0, 140, 62);
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                    new ItemStack(com.trd.block.basic.ModBlocks.JERNOVA.get()));
            this.title = Component.translatable("jei.category.trd.millstone");
        }

        @Override public RecipeType<MillstoneWrapper> getRecipeType() { return MILLSTONE_TYPE; }
        @Override public Component getTitle() { return title; }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MillstoneWrapper recipe, IFocusGroup focuses) {
            // Только реальный входной слот (без пустых заглушек слева)
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 5)
                    .addItemStack(new ItemStack(recipe.input()));

            // Выходные слоты 3×3 справа — только реальные предметы, без пустых заглушек
            int[][] rightSlots = {
                    {83, 5}, {101, 5}, {119, 5},
                    {83, 23}, {101, 23}, {119, 23},
                    {83, 41}, {101, 41}, {119, 41}
            };
            List<ItemStack> outputs = recipe.outputs();
            for (int i = 0; i < outputs.size() && i < rightSlots.length; i++) {
                ItemStack stack = outputs.get(i);
                if (!stack.isEmpty()) {
                    builder.addSlot(RecipeIngredientRole.OUTPUT, rightSlots[i][0], rightSlots[i][1])
                            .addItemStack(stack.copy());
                }
            }
        }

        @Override
        public void draw(MillstoneWrapper recipe, IRecipeSlotsView view, GuiGraphics gg, double mx, double my) {
            var font = Minecraft.getInstance().font;
            String grindsText = recipe.grindsRequired() + " об";
            gg.drawString(font, grindsText, 60, 32, 0xFF555555, false);
        }
    }

    public static class SmeltingCategory implements IRecipeCategory<SmeltingWrapper> {
        private final IDrawable background;
        private final IDrawable icon;
        private final Component title;
        private final List<ItemStack> machines;

        public SmeltingCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createDrawable(
                    new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/jei/jei_cast_gui.png"),
                    0, 0, 120, 60);
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                    new ItemStack(ModBlocks.CASTING_DESCENT.get()));
            this.title = Component.translatable("jei.category.trd.smelting");
            this.machines = Arrays.asList(
                    new ItemStack(ModBlocks.SMELTER.get()),
                    new ItemStack(ModBlocks.SMALL_SMELTER.get())
            );
        }

        @Override public RecipeType<SmeltingWrapper> getRecipeType() { return SMELTING_TYPE; }
        @Override public Component getTitle() { return title; }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SmeltingWrapper recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 13).addItemStack(recipe.input());
            builder.addSlot(RecipeIngredientRole.INPUT, 23, 13);
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 31);
            builder.addSlot(RecipeIngredientRole.INPUT, 23, 31);

            ItemStack liquidMetal = createLiquidMetalStack(recipe.metal(), recipe.outputUnits());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 81, 13).addItemStack(liquidMetal);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 13);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 81, 31);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 31);
        }

        @Override
        public void draw(SmeltingWrapper recipe, IRecipeSlotsView view, GuiGraphics gg, double mx, double my) {
            long sec = System.currentTimeMillis() / 1000;
            ItemStack machine = machines.get((int) (sec % machines.size()));
            gg.renderItem(machine, 52, 13);
            gg.renderItemDecorations(Minecraft.getInstance().font, machine, 52, 13);

            var font = Minecraft.getInstance().font;
            gg.drawString(font, recipe.temp() + "°C", 42, 41, 0xFF555555, false);
            gg.drawString(font, String.format("%.1fs", recipe.timeTicks() / 20f), 42, 51, 0xFF555555, false);
        }
    }

    public static class CastingCategory implements IRecipeCategory<CastingWrapper> {
        private final IDrawable background;
        private final IDrawable icon;
        private final Component title;
        private final List<ItemStack> machines;
        public CastingCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createDrawable(
                    new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/jei/jei_cast_gui.png"),
                    0, 0, 120, 60);
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                    new ItemStack(ModBlocks.CASTING_POT.get()));
            this.title = Component.translatable("jei.category.trd.casting");
            this.machines = Arrays.asList(
                    new ItemStack(ModBlocks.SMELTER.get()),
                    new ItemStack(ModBlocks.SMALL_SMELTER.get())
            );
        }

        @Override public RecipeType<CastingWrapper> getRecipeType() { return CASTING_TYPE; }
        @Override public Component getTitle() { return title; }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, CastingWrapper recipe, IFocusGroup focuses) {
            ItemStack liquidMetal = createLiquidMetalStack(recipe.metal(), recipe.requiredUnits());
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 13).addItemStack(liquidMetal);
            builder.addSlot(RecipeIngredientRole.INPUT, 23, 13);
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 31);
            builder.addSlot(RecipeIngredientRole.INPUT, 23, 31);

            builder.addSlot(RecipeIngredientRole.INPUT, 52, 13)
                    .addItemStack(new ItemStack(recipe.mold().getMoldItem()));

            builder.addSlot(RecipeIngredientRole.OUTPUT, 81, 13).addItemStack(recipe.output());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 13);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 81, 31);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 31);
        }
    }

    public static class AlloyingCategory implements IRecipeCategory<AlloyingWrapper> {
        private final IDrawable background;
        private final IDrawable icon;
        private final Component title;

        public AlloyingCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createDrawable(
                    new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/jei/jei_alloy_gui.png"),
                    0, 0, 120, 60);
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                    new ItemStack(ModBlocks.SMELTER.get()));
            this.title = Component.translatable("jei.category.trd.alloying");
        }

        @Override public RecipeType<AlloyingWrapper> getRecipeType() { return ALLOYING_TYPE; }
        @Override public Component getTitle() { return title; }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, AlloyingWrapper wrapper, IFocusGroup focuses) {
            AlloyRecipe recipe = wrapper.recipe();
            AlloySlot[] slots = recipe.getSlots();
            int[] xs = {5, 23, 41, 59};

            for (int i = 0; i < 4; i++) {
                if (slots[i].item() != null && slots[i].count() > 0) {
                    builder.addSlot(RecipeIngredientRole.INPUT, xs[i], 22)
                            .addItemStack(new ItemStack(slots[i].item(), slots[i].count()));
                } else {
                    builder.addSlot(RecipeIngredientRole.INPUT, xs[i], 22);
                }
            }

            ItemStack liquidMetal = createLiquidMetalStack(recipe.getOutputMetal(), recipe.getOutputUnits());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 22).addItemStack(liquidMetal);
        }

        @Override
        public void draw(AlloyingWrapper wrapper, IRecipeSlotsView view, GuiGraphics gg, double mx, double my) {
            AlloyRecipe recipe = wrapper.recipe();
            var font = Minecraft.getInstance().font;
            gg.drawString(font, recipe.getOutputMetal().getMeltingPoint() + "°C", 4, 42, 0xFF555555, false);
            gg.drawString(font, String.format("%.1fs", recipe.getSmeltTimeTicks() / 20f), 4, 52, 0xFF555555, false);
        }
    }
}