package com.trd.client;

import com.trd.api.fluids.system.FluidDropItem;
import com.trd.api.fluids.ModFluids;
import com.trd.api.metallurgy.system.ItemHeatColorRegistry;
import com.trd.client.gecko.entity.mobs.DepthWormBrutalRenderer;
import com.trd.item.tools.FluidIdentifierItem;
import com.trd.main.ResourceRegistry;
import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.industrial.fluids.FluidPipeBlockEntity;
import com.trd.block.entity.industrial.rotation.ShaftBlockEntity;
import com.trd.client.gecko.block.energy.MachineBatteryRenderer;
import com.trd.client.overlay.gui.*;
import com.trd.client.render.flywheel.ModModels;
import com.trd.client.render.flywheel.ShaftVisual;
import com.trd.client.renderer.*;

import com.trd.multiblock.industrial.FuelTankBlockEntity;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;


import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import com.trd.block.entity.ModBlockEntities;
import com.trd.client.config.ModConfigKeybindHandler;
//import com.trd.client.gecko.block.rotation.DrillHeadRenderer;
//import com.trd.client.gecko.block.rotation.MotorElectroRenderer;
//import com.trd.client.gecko.block.rotation.ShaftRenderer;
//import com.trd.client.gecko.block.rotation.WindGenFlugerRenderer;
import com.trd.client.gecko.block.turrets.TurretLightPlacerRenderer;
import com.trd.client.gecko.entity.bullets.TurretBulletRenderer;
import com.trd.client.gecko.entity.mobs.DepthWormRenderer;
import com.trd.client.gecko.entity.turrets.TurretLightLinkedRenderer;
import com.trd.client.gecko.entity.turrets.TurretLightRenderer;
import com.trd.client.overlay.hud.OverlayAmmoHud;
import com.trd.client.overlay.hud.TachometerOverlay;
import com.trd.entity.ModEntities;
import com.trd.item.ModItems;
import com.trd.main.MainRegistry;
import com.trd.menu.ModMenuTypes;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        MinecraftForge.EVENT_BUS.register(ClientRenderHandler.class);
        // Единый таймер кадра — обновляется 1 раз ДО всех Flywheel beginFrame()
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.RenderTickEvent renderEvent) -> {
            if (renderEvent.phase == net.minecraftforge.event.TickEvent.Phase.START) {
                com.trd.client.render.flywheel.AnimationTimer.onFrameStart();
            }
        });

        ModItems.MACHINEGUN.ifPresent(item -> {
            ItemProperties.register(item,
                    new ResourceLocation(MainRegistry.MOD_ID, "pull"),
                    (pStack, pLevel, pEntity, pSeed) -> {
                        if (pEntity != null && pEntity.isUsingItem() && pEntity.getUseItem() == pStack) {
                            return 1.0f;
                        }
                        return 0.0f;
                    });
        });
        MenuScreens.register(ModMenuTypes.SMALL_SMELTER_MENU.get(), GUISmallSmelter::new);
        MenuScreens.register(ModMenuTypes.MACHINE_BATTERY_MENU.get(), GUIMachineBattery::new);
        MenuScreens.register(ModMenuTypes.MOTOR_ELECTRO_MENU.get(), GUIMotorElectro::new);
        MenuScreens.register(ModMenuTypes.TURRET_AMMO_MENU.get(), GUITurretAmmo::new);
//        MenuScreens.register(ModMenuTypes.SHAFT_PLACER_MENU.get(), GUIShaftPlacer::new);
//        MenuScreens.register(ModMenuTypes.MINING_PORT_MENU.get(), GUIMiningPort::new);
        MenuScreens.register(ModMenuTypes.FLUID_BARREL_MENU.get(), GUIFluidBarrel::new);
        MenuScreens.register(ModMenuTypes.HEATER_MENU.get(), GUIHeater::new);
        MenuScreens.register(ModMenuTypes.SMELTER_MENU.get(), GUISmelter::new);
        MenuScreens.register(ModMenuTypes.FUEL_TANK_MENU.get(), GUIFuelTank::new);
        
        event.registerBlockEntityRenderer(ModBlockEntities.MOTOR_ELECTRO_BE.get(), com.trd.client.render.flywheel.DummyFlywheelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SHAFT_BE.get(), com.trd.client.render.flywheel.DummyFlywheelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BEARING_BE.get(), com.trd.client.render.flywheel.DummyFlywheelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TACHOMETER_BE.get(), com.trd.client.render.flywheel.DummyFlywheelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FUEL_TANK_BE.get(), com.trd.client.render.flywheel.DummyFlywheelRenderer::new);

//        event.registerBlockEntityRenderer(ModBlockEntities.WIND_GEN_FLUGER_BE.get(), WindGenFlugerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TURRET_LIGHT_PLACER_BE.get(), TurretLightPlacerRenderer::new);
//        event.registerBlockEntityRenderer(ModBlockEntities.DRILL_HEAD_BE.get(), DrillHeadRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MACHINE_BATTERY_BE.get(), MachineBatteryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CONNECTOR_BE.get(), ConnectorRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADIER_ZOMBIE.get(), ZombieRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CASTING_DESCENT.get(), CastingDescentRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CASTING_POT.get(), com.trd.client.renderer.CastingPotRenderer::new);

        ModEntities.GRENADE_NUC_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_FIRE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_SLIME_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_HE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADEHE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADEFIRE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADESMART_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADESLIME_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
        ModEntities.GRENADE_IF_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new));
    }

    // ВАЖНО: Добавляем (priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onRegisterAdditionalModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        // ModModels.init(); <--- УДАЛИ ЭТУ СТРОКУ ОТСЮДА! Она теперь в главном классе.

        // 2. Статика
        event.register(new net.minecraft.resources.ResourceLocation("trd", "block/half_shaft"));
        event.register(new net.minecraft.resources.ResourceLocation("trd", "block/electro_motor"));
        event.register(new net.minecraft.resources.ResourceLocation("trd", "block/bearing_shaft"));
        event.register(new net.minecraft.resources.ResourceLocation("trd", "block/tachometr"));
        event.register(new ResourceLocation("trd", "block/fuel_tank_big"));
        event.register(new ResourceLocation("trd", "block/flywheel_light"));

        // 3. Динамические модели
        for (String name : ModModels.GEAR_MODELS.keySet()) {
            event.register(new net.minecraft.resources.ResourceLocation("trd", "block/" + name));
        }

        for (String name : ModModels.SHAFT_MODELS.keySet()) {
            event.register(new net.minecraft.resources.ResourceLocation("trd", "block/" + name));
        }
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Устанавливаем RenderType для технических блоков балок, чтобы Sodium не применял к ним агрессивное отсечение
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.BEAM_COLLISION.get(), net.minecraft.client.renderer.RenderType.cutout());

        // 1. Инициализируем загрузку кастомной 3D модели для Flywheel
        event.enqueueWork(() -> {
            VisualizerRegistry.setVisualizer(ModBlockEntities.SHAFT_BE.get(), new dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer<com.trd.block.entity.industrial.rotation.ShaftBlockEntity>() {

                @Override
                public BlockEntityVisual<? super ShaftBlockEntity> createVisual(VisualizationContext ctx, ShaftBlockEntity be, float partialTick) {
                    // Возвращаем наш визуал, передавая все нужные параметры
                    return new ShaftVisual(ctx, be, partialTick);
                }

                @Override
                public boolean skipVanillaRender(ShaftBlockEntity be) {
                    // Обязательно true! Отключаем ванильный рендер для максимального FPS
                    return true;
                }
            });

            VisualizerRegistry.setVisualizer(ModBlockEntities.MOTOR_ELECTRO_BE.get(), new dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer<com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity>() {

                @Override
                public dev.engine_room.flywheel.api.visual.BlockEntityVisual<? super com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity> createVisual(dev.engine_room.flywheel.api.visualization.VisualizationContext ctx, com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity be, float partialTick) {
                    // Возвращаем визуал мотора
                    return new com.trd.client.render.flywheel.MotorVisual(ctx, be, partialTick);
                }

                @Override
                public boolean skipVanillaRender(com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity be) {
                    // Отключаем ванильный рендер, чтобы Flywheel взял всё на себя
                    return true;
                }
            });

            VisualizerRegistry.setVisualizer(ModBlockEntities.BEARING_BE.get(), new dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer<com.trd.block.entity.industrial.rotation.BearingBlockEntity>() {

                @Override
                public dev.engine_room.flywheel.api.visual.BlockEntityVisual<? super com.trd.block.entity.industrial.rotation.BearingBlockEntity> createVisual(dev.engine_room.flywheel.api.visualization.VisualizationContext ctx, com.trd.block.entity.industrial.rotation.BearingBlockEntity be, float partialTick) {
                    // Возвращаем инстанс подшипника (внутреннее кольцо и вставленный вал)
                    return new com.trd.client.render.flywheel.BearingVisual(ctx, be, partialTick);
                }

                @Override
                public boolean skipVanillaRender(com.trd.block.entity.industrial.rotation.BearingBlockEntity be) {
                    // Отключаем ванильный BlockEntityRenderer
                    return true;
                }
            });

            VisualizerRegistry.setVisualizer(ModBlockEntities.TACHOMETER_BE.get(), new dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer<com.trd.block.entity.industrial.rotation.TachometerBlockEntity>() {

                @Override
                public dev.engine_room.flywheel.api.visual.BlockEntityVisual<? super com.trd.block.entity.industrial.rotation.TachometerBlockEntity> createVisual(dev.engine_room.flywheel.api.visualization.VisualizationContext ctx, com.trd.block.entity.industrial.rotation.TachometerBlockEntity be, float partialTick) {
                    return new com.trd.client.render.flywheel.TachometerVisual(ctx, be, partialTick);
                }

                @Override
                public boolean skipVanillaRender(com.trd.block.entity.industrial.rotation.TachometerBlockEntity be) {
                    return true;
                }
            });
        });


        // === ЦИСТЕРНА (Flywheel рендер) ===
        VisualizerRegistry.setVisualizer(ModBlockEntities.FUEL_TANK_BE.get(),
                new dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer<FuelTankBlockEntity>() {

                    @Override
                    public BlockEntityVisual<? super FuelTankBlockEntity> createVisual(
                            VisualizationContext ctx, FuelTankBlockEntity be, float partialTick) {
                        return new com.trd.client.render.flywheel.FuelTankVisual(ctx, be, partialTick);
                    }

                    @Override
                    public boolean skipVanillaRender(FuelTankBlockEntity be) {
                        return true; // Отключаем ванильный рендер полностью
                    }
                });

    }
    //================================================================================================
    //================================================================================================

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        EntityRenderers.register(ModEntities.TURRET_BULLET.get(), TurretBulletRenderer::new);
        EntityRenderers.register(ModEntities.DEPTH_WORM.get(), DepthWormRenderer::new);
        EntityRenderers.register(ModEntities.DEPTH_WORM_BRUTAL.get(), DepthWormBrutalRenderer::new);
        event.registerEntityRenderer(ModEntities.TURRET_LIGHT.get(), TurretLightRenderer::new);
        EntityRenderers.register(ModEntities.TURRET_LIGHT_LINKED.get(), TurretLightLinkedRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        MinecraftForge.EVENT_BUS.register(ModConfigKeybindHandler.class);

    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "ammo_hud", OverlayAmmoHud.HUD_AMMO);
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "tachometer_hud", TachometerOverlay.HUD_TACHOMETER);
    }


    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // Перечисляем все наши трубы
        Block[] pipes = {
                ModBlocks.BRONZE_FLUID_PIPE.get(),
                ModBlocks.STEEL_FLUID_PIPE.get(),
                ModBlocks.LEAD_FLUID_PIPE.get(),
                ModBlocks.TUNGSTEN_FLUID_PIPE.get()
                // добавь остальные
        };

        for (Block pipe : pipes) {
            for (BlockState state : pipe.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                BakedModel original = event.getModels().get(location);
                if (original != null) {
                    // Оборачиваем оригинальную модель в наш хардкорный движок
                    event.getModels().put(location, new PipeBakedModel(original));
                }
            }
        }

        // Подмена модели для невидимых блоков коллизии балок
        ModelResourceLocation beamBaseLocation = BlockModelShaper.stateToModelLocation(ModBlocks.BEAM_BLOCK.get().defaultBlockState());
        BakedModel beamBaseModel = event.getModels().get(beamBaseLocation);
        if (beamBaseModel != null) {
            for (BlockState state : ModBlocks.BEAM_COLLISION.get().getStateDefinition().getPossibleStates()) {
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                event.getModels().put(location, new DynamicBeamModel(beamBaseModel));
            }
        }
    }

    // 2. АППАРАТНАЯ РАСКРАСКА
    @SubscribeEvent
    public static void registerBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            // tintIndex == 1 мы прописали в PipeBakedModel
            if (tintIndex == 1 && level != null && pos != null) {
                if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
                    net.minecraft.world.level.material.Fluid fluid = be.getFilterFluid();

                    if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {

                        // --- ИСКЛЮЧЕНИЕ ДЛЯ ВАНИЛЬНОЙ ЛАВЫ ---
                        if (fluid == net.minecraft.world.level.material.Fluids.LAVA || fluid == net.minecraft.world.level.material.Fluids.FLOWING_LAVA) {
                            return 0xFF5500; // Красивый огненно-оранжевый цвет
                        }
                        // --- ИСКЛЮЧЕНИЕ ДЛЯ ВАНИЛЬНОЙ ВОДЫ (на всякий случай) ---
                        if (fluid == net.minecraft.world.level.material.Fluids.WATER || fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER) {
                            return 0x3F76E4; // Стандартный синий цвет воды
                        }

                        // Для всех твоих кастомных жидкостей берем их родной цвет
                        return net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid).getTintColor();
                    }
                }
            }
            return -1; // -1 значит "не перекрашивать"
        }, ModBlocks.BRONZE_FLUID_PIPE.get(), ModBlocks.STEEL_FLUID_PIPE.get(), ModBlocks.LEAD_FLUID_PIPE.get(), ModBlocks.TUNGSTEN_FLUID_PIPE.get() /* Добавь остальные трубы */);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {

        for (RegistryObject<Item> dropObj : ModFluids.getAllFluidDrops().values()) {
            Item item = dropObj.get();
            if (item instanceof FluidDropItem fluidDrop) {
                event.register((stack, tintIndex) -> {
                    // tintIndex 0 — основной цвет предмета
                    if (tintIndex == 0) {
                        return fluidDrop.getFluidTintColor();
                    }
                    return 0xFFFFFFFF;
                }, item);
            }
        }

        event.register((stack, tintIndex) -> 0x717070, ModFluids.FLUID_DROP_NONE.get());
        event.register((stack, tintIndex) -> 0xe64306, ModFluids.FLUID_DROP_LAVA.get());
        event.register((stack, tintIndex) -> 0x4487ff, ModFluids.FLUID_DROP_WATER.get());


        event.getItemColors().register((stack, tintIndex) -> {
            if (tintIndex == 1 && stack.getItem() instanceof FluidIdentifierItem) {
                String fluidName = FluidIdentifierItem.getSelectedFluid(stack);
                if (!fluidName.equals("none")) {

                    if (fluidName.contains("lava")) return 0xe64306;
                    if (fluidName.contains("none")) return 0x717070;

                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidName));
                    if (fluid != null) {
                        return net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluidType())
                                .getTintColor(new FluidStack(fluid, 1000)) | 0xFF000000;
                    }
                }
            }
            return 0xFFFFFFFF;
        }, ModItems.FLUID_IDENTIFIER.get());



        // === КРАСНОЕ СВЕЧЕНИЕ ===
        ItemHeatColorRegistry.registerMixed(ItemHeatColorRegistry.HeatGradient.RED_TO_WHITE,
                Items.NETHERITE_INGOT,
                Items.NETHERITE_BLOCK,
                Items.NETHERITE_PICKAXE,
                Items.NETHERITE_AXE,
                Items.NETHERITE_SHOVEL,
                Items.NETHERITE_HOE,
                Items.NETHERITE_SWORD,
                Items.NETHERITE_HELMET,
                Items.NETHERITE_CHESTPLATE,
                Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS
        );

        // === ЖЁЛТОЕ СВЕЧЕНИЕ ===
        ItemHeatColorRegistry.registerMixed(ItemHeatColorRegistry.HeatGradient.YELLOW_TO_WHITE

        );

        // === ОРАНЖЕВОЕ СВЕЧЕНИЕ ===
        ItemHeatColorRegistry.registerMixed(ItemHeatColorRegistry.HeatGradient.ORANGE_TO_WHITE,
                Items.COPPER_INGOT,
                Items.COPPER_BLOCK,
                Items.RAW_COPPER,
                Items.RAW_COPPER_BLOCK,
                Items.COPPER_ORE,
                Items.DEEPSLATE_COPPER_ORE,
                Items.LIGHTNING_ROD,
                Items.IRON_INGOT,
                Items.IRON_NUGGET,
                Items.IRON_BLOCK,
                Items.RAW_IRON,
                Items.RAW_IRON_BLOCK,
                Items.IRON_ORE,
                Items.DEEPSLATE_IRON_ORE,
                Items.IRON_PICKAXE,
                Items.IRON_AXE,
                Items.IRON_SHOVEL,
                Items.IRON_HOE,
                Items.IRON_SWORD,
                Items.IRON_HELMET,
                Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS,
                Items.IRON_BOOTS,
                Items.GOLD_INGOT,
                Items.GOLD_NUGGET,
                Items.GOLD_BLOCK,
                Items.RAW_GOLD,
                Items.RAW_GOLD_BLOCK,
                Items.GOLD_ORE,
                Items.DEEPSLATE_GOLD_ORE,
                Items.GOLDEN_PICKAXE,
                Items.GOLDEN_AXE,
                Items.GOLDEN_SHOVEL,
                Items.GOLDEN_HOE,
                Items.GOLDEN_SWORD,
                Items.GOLDEN_HELMET,
                Items.GOLDEN_CHESTPLATE,
                Items.GOLDEN_LEGGINGS,
                Items.GOLDEN_BOOTS,
                ModItems.CAST_PICKAXE_STEEL_BASE.get(),
                ModItems.CAST_PICKAXE_IRON_BASE.get(),
                ResourceRegistry.getMainUnit("steel"),
                ResourceRegistry.getSmallUnit("steel"),
                ResourceRegistry.getBlock("steel"),
                ResourceRegistry.getMainUnit("aluminum"),
                ResourceRegistry.getSmallUnit("aluminum"),
                ResourceRegistry.getBlock("aluminum"),
                ResourceRegistry.getMainUnit("bronze"),
                ResourceRegistry.getSmallUnit("bronze"),
                ResourceRegistry.getBlock("bronze"),
                ResourceRegistry.getMainUnit("tin"),
                ResourceRegistry.getSmallUnit("tin"),
                ResourceRegistry.getBlock("tin"),
                ResourceRegistry.getMainUnit("titanium"),
                ResourceRegistry.getSmallUnit("titanium"),
                ResourceRegistry.getBlock("titanium"),
                ResourceRegistry.getBlock("industrial_copper"),
                ResourceRegistry.getMainUnit("industrial_copper"),
                ResourceRegistry.getSmallUnit("industrial_copper"),
                ResourceRegistry.getBlock("titanium"),
                ResourceRegistry.getMainUnit("zinc"),
                ResourceRegistry.getSmallUnit("zinc"),
                ResourceRegistry.getBlock("zinc")
        );

        // === СИНИЕ СВЕЧЕНИЕ ===
        ItemHeatColorRegistry.registerMixed(ItemHeatColorRegistry.HeatGradient.BLUE_TO_WHITE

        );


        // === РЕГИСТРАЦИЯ ОБРАБОТЧИКОВ ЦВЕТА ===
        // Железо и сталь
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                Items.IRON_INGOT, Items.IRON_NUGGET,
                net.minecraft.world.level.block.Blocks.IRON_BLOCK.asItem(),
                Items.RAW_IRON, Items.RAW_IRON_BLOCK,
                Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE,
                Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                ResourceRegistry.getMainUnit("steel"),
                ResourceRegistry.getSmallUnit("steel"),
                ModItems.CAST_PICKAXE_STEEL_BASE.get(),
                ModItems.CAST_PICKAXE_IRON_BASE.get(),
                ResourceRegistry.getBlock("steel").asItem()
        );

        // Золото
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                Items.GOLD_INGOT, Items.GOLD_NUGGET,
                net.minecraft.world.level.block.Blocks.GOLD_BLOCK.asItem(),
                Items.RAW_GOLD, Items.RAW_GOLD_BLOCK,
                Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE,
                Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE, Items.GOLDEN_SWORD,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS
        );

        // Медь
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                Items.COPPER_INGOT,
                net.minecraft.world.level.block.Blocks.COPPER_BLOCK.asItem(),
                Items.RAW_COPPER, Items.RAW_COPPER_BLOCK,
                Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE,
                Items.LIGHTNING_ROD
        );

        // Незерит
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                Items.NETHERITE_INGOT,
                net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK.asItem(),
                Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_SWORD,
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS
        );

        // Алюминий
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                ResourceRegistry.getMainUnit("aluminum"),
                ResourceRegistry.getSmallUnit("aluminum"),
                ResourceRegistry.getBlock("aluminum").asItem()
        );

        // Бронза
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                ResourceRegistry.getMainUnit("bronze"),
                ResourceRegistry.getSmallUnit("bronze"),
                ResourceRegistry.getBlock("bronze").asItem()
        );

        // Олово
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                ResourceRegistry.getMainUnit("tin"),
                ResourceRegistry.getSmallUnit("tin"),
                ResourceRegistry.getBlock("tin").asItem()
        );

        // Цинк
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                ResourceRegistry.getMainUnit("zinc"),
                ResourceRegistry.getSmallUnit("zinc"),
                ResourceRegistry.getBlock("zinc").asItem()
        );

        // Титан
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                ResourceRegistry.getMainUnit("titanium"),
                ResourceRegistry.getSmallUnit("titanium"),
                ResourceRegistry.getBlock("titanium").asItem()
        );

        // Промышленная медь
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getHeatColor(stack, tintIndex),
                ResourceRegistry.getMainUnit("industrial_copper"),
                ResourceRegistry.getSmallUnit("industrial_copper"),
                ResourceRegistry.getBlock("industrial_copper").asItem()
        );

        // Спец обработчик для шлака
        event.register((stack, tintIndex) -> ItemHeatColorRegistry.getSlagHeatColor(stack, tintIndex),
                ModItems.SLAG.get()
        );

        event.register((stack, tintIndex) -> {
            if (tintIndex == 0 && stack.hasTag() && stack.getTag().contains("MetalColor")) {
                return stack.getTag().getInt("MetalColor");
            }
            return 0xFFFFFF;
        }, ModItems.LIQUID_METAL.get());
    }
}
