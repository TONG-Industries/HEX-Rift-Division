package com.trd.main;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import com.trd.block.basic.ModBlocks;
import com.trd.item.ModItems;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MainRegistry.MOD_ID);

    // Первая вкладка BUILD (без withTabsBefore)
    public static final RegistryObject<CreativeModeTab> trd_BUILD_TAB = CREATIVE_MODE_TABS.register("trd_build_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MainRegistry.MOD_ID + ".trd_build_tab"))
                    .icon(() -> new ItemStack(ModBlocks.CONCRETE.get()))
                    .build());

    // Вкладка TECH – должна быть после BUILD
    public static final RegistryObject<CreativeModeTab> trd_TECH_TAB = CREATIVE_MODE_TABS.register("trd_tech_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MainRegistry.MOD_ID + ".trd_tech_tab"))
                    .icon(() -> new ItemStack(ModBlocks.MACHINE_BATTERY.get()))
                    .withTabsBefore(new ResourceLocation(MainRegistry.MOD_ID, "trd_build_tab"))
                    .build());

    public static final RegistryObject<CreativeModeTab> trd_WEAPONS_TAB = CREATIVE_MODE_TABS.register("trd_weapons_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MainRegistry.MOD_ID + ".trd_weapons_tab"))
                    .icon(() -> new ItemStack(ModItems.GRENADE_NUC.get()))
                    .withTabsBefore(new ResourceLocation(MainRegistry.MOD_ID, "trd_tech_tab"))
                    .build());

    public static final RegistryObject<CreativeModeTab> trd_RECOURSES_TAB = CREATIVE_MODE_TABS.register("trd_recourses_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MainRegistry.MOD_ID + ".trd_recourses_tab"))
                    .icon(() -> new ItemStack(ModItems.FIREBRICK.get()))
                    .withTabsBefore(new ResourceLocation(MainRegistry.MOD_ID, "trd_weapons_tab"))
                    .build());

    public static final RegistryObject<CreativeModeTab> trd_NATURE_TAB = CREATIVE_MODE_TABS.register("trd_nature_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MainRegistry.MOD_ID + ".trd_nature_tab"))
                    .icon(() -> new ItemStack(ModItems.DEPTH_WORM_SPAWN_EGG.get()))
                    .withTabsBefore(new ResourceLocation(MainRegistry.MOD_ID, "trd_recourses_tab"))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}