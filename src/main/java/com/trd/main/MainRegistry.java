package com.trd.main;

import com.trd.api.fluids.ModFluids;
import com.trd.api.fuel.ModFuels;
import com.trd.api.hive.HiveNetworkManager;
import com.trd.api.metallurgy.ModMetallurgy;
import com.trd.api.metallurgy.system.Metal;
import com.trd.api.metallurgy.system.MetalUnits2;
import com.trd.api.metallurgy.system.MetallurgyRegistry;
import com.trd.api.vein.VeinManager;
import com.trd.block.entity.conglomerate.ConglomerateBlockEntity;
import com.trd.datagen.stats.ModBlockLootTableProvider;
import com.trd.entity.mobs.depth_worm.DepthWormBrutalEntity;
import com.trd.entity.mobs.grenadier.GrenadierZombieEntity;
import com.trd.event.SlagItem;
import com.trd.worldgen.feature.ModFeatures;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import com.trd.api.hive.HiveNetworkManagerProvider;
import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.ModBlockEntities;
import com.trd.capability.ModCapabilities;
import com.trd.entity.ModEntities;
import com.trd.entity.mobs.depth_worm.DepthWormEntity;
import com.trd.entity.weapons.turrets.TurretLightEntity;
import com.trd.event.CrateBreaker;
import com.trd.item.energy.ModBatteryItem;
import com.trd.menu.ModMenuTypes;
import com.trd.network.ModPacketHandler;
import com.trd.sound.ModSounds;
import com.trd.worldgen.biome.ModSurfaceRules;
import com.trd.worldgen.biome.terrablender.ModOverworldRegion;
import com.trd.worldgen.tree.custom.ModFoliagePlacerTypes;
import com.trd.worldgen.tree.custom.ModTrunkPlacerTypes;
import software.bernie.geckolib.GeckoLib;

import com.trd.item.ModItems;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod(MainRegistry.MOD_ID)
public class MainRegistry {
    public static final String MOD_ID = "trd";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MainRegistry() {
        LOGGER.info("Initializing Crustal Incursion...");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModCreativeTabs.register(modEventBus);
        GeckoLib.initialize();
        this.registerCapabilities(modEventBus);
        ResourceRegistry.init();
        ModBlocks.register(modEventBus); // 1. Сначала блоки
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new CrateBreaker());
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        modEventBus.addListener(this::entityAttributeEvent);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        ModTrunkPlacerTypes.register(modEventBus);
        ModFoliagePlacerTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        MinecraftForge.EVENT_BUS.addListener(this::onFuelBurnTime);
        MinecraftForge.EVENT_BUS.register(new HiveEventHandler());
        // Проверяем, есть ли Окулус
        // Проверяем наличие Окулуса
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            // 1. Проверяем наличие Окулуса
            if (net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById("oculus") != null) {

                // 2. УНИВЕРСАЛЬНЫЙ ЩИТ: Ищем признаки любого стороннего фикса Iris-Flywheel
                // Мы ищем по файлу миксинов, так как ID мода у всех может быть разным
                boolean hasThirdPartyFix = Thread.currentThread().getContextClassLoader()
                        .getResource("irisflw.mixins.json") != null ||
                        net.minecraftforge.fml.loading.FMLLoader.getLoadingModList()
                                .getModFileById("oculusflywheelcompat") != null
                        ||
                        net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById("irisflw") != null;

                if (!hasThirdPartyFix) {
                    // Если чисто — зажигаем!
                    com.trd.compat.irisflw.IrisFlw.init();
                    LOGGER.info("🔥 [trd] Движок Flywheel-Oculus успешно запущен!");
                } else {
                    // Если кто-то уже чинит — вежливо отходим
                    LOGGER.warn(
                            "🛡️ [trd] Обнаружен сторонний графический фикс. Встроенная оптимизация trd отключена для стабильности.");
                }
            }
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.trd.client.render.flywheel.ModModels.init();
        }

    }

    private void registerCapabilities(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::register);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModMetallurgy.init(); // <-- регистрация металлов и рецептов
            ModPacketHandler.register();
            Regions.register(new ModOverworldRegion(new ResourceLocation(MOD_ID, "overworld"), 5));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, "trd",
                    ModSurfaceRules.makeRules());
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Логгирование для отладки
        LOGGER.info("Building creative tab contents for: " + event.getTabKey());
        ResourceRegistry.addCreative(event);
        if (event.getTab() == ModCreativeTabs.trd_BUILD_TAB.get()) {

            event.accept(ModBlocks.CONCRETE.get());
            event.accept(ModBlocks.CONCRETE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_HAZARD_NEW.get());
            event.accept(ModBlocks.CONCRETE_HAZARD_NEW_SLAB.get());
            event.accept(ModBlocks.CONCRETE_HAZARD_NEW_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_HAZARD_OLD.get());
            event.accept(ModBlocks.CONCRETE_HAZARD_OLD_SLAB.get());
            event.accept(ModBlocks.CONCRETE_HAZARD_OLD_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_TILE.get());
            event.accept(ModBlocks.CONCRETE_TILE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_TILE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_TILE_ALT.get());
            event.accept(ModBlocks.CONCRETE_TILE_ALT_SLAB.get());
            event.accept(ModBlocks.CONCRETE_TILE_ALT_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_TILE_ALT_BLUE.get());
            event.accept(ModBlocks.CONCRETE_TILE_ALT_BLUE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_TILE_ALT_BLUE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_STRIPPED.get());
            event.accept(ModBlocks.CONCRETE_STRIPPED_SLAB.get());
            event.accept(ModBlocks.CONCRETE_STRIPPED_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_REINFORCED.get());
            event.accept(ModBlocks.CONCRETE_REINFORCED_SLAB.get());
            event.accept(ModBlocks.CONCRETE_REINFORCED_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_REINFORCED_HEAVY.get());
            event.accept(ModBlocks.CONCRETE_REINFORCED_HEAVY_SLAB.get());
            event.accept(ModBlocks.CONCRETE_REINFORCED_HEAVY_STAIRS.get());
            event.accept(ModBlocks.FIREBRICK_BLOCK.get());
            event.accept(ModBlocks.FIREBRICK_SLAB.get());
            event.accept(ModBlocks.FIREBRICK_STAIRS.get());
            event.accept(ModBlocks.REINFORCEDBRICK_BLOCK.get());
            event.accept(ModBlocks.REINFORCEDBRICK_SLAB.get());
            event.accept(ModBlocks.REINFORCEDBRICK_STAIRS.get());
            event.accept(ModBlocks.SEQUOIA_PLANKS.get());
            event.accept(ModBlocks.SEQUOIA_SLAB.get());
            event.accept(ModBlocks.SEQUOIA_STAIRS.get());

            event.accept(ModBlocks.MORY_BLOCK);
            event.accept(ModBlocks.ANTON_CHIGUR);
            event.accept(ModBlocks.MINERAL_BLOCK2.get());
            event.accept(ModBlocks.MINERAL_TILE.get());
            event.accept(ModBlocks.DOLOMITE_TILE.get());
            event.accept(ModBlocks.TILE_LIGHT.get());
            event.accept(ModBlocks.SULFUR_TILE.get());
            event.accept(ModBlocks.SULFUR_BRICKS.get());
            event.accept(ModBlocks.CONCRETE_NET.get());
            event.accept(ModBlocks.DECO_STEEL.get());
            event.accept(ModBlocks.DECO_STEEL_DARK.get());
            event.accept(ModBlocks.DECO_STEEL_SMOG.get());
            event.accept(ModBlocks.DECO_LEAD.get());
            event.accept(ModBlocks.DECO_BEAM.get());
            event.accept(ModBlocks.BEAM_BLOCK.get());
            event.accept(ModBlocks.STEEL_PROPS.get());
            event.accept(ModBlocks.ROUND_LAMP.get());

        }

        if (event.getTab() == ModCreativeTabs.trd_TECH_TAB.get()) {


            //ИНСТРУМЕНТЫ
            event.accept(ModItems.BEAM_PLACER.get());
            event.accept(ModItems.SCREWDRIVER.get());
            event.accept(ModItems.POKER.get());

            event.accept(ModItems.BELT.get());
            event.accept(ModItems.WIRE_COIL);

            event.accept(ModItems.INFINITE_FLUID_BARREL);
            event.accept(ModItems.FLUID_IDENTIFIER.get());

            event.accept(ModBlocks.STEEL_STORAGE.get());


            //КИНЕТИКА
            event.accept(ModBlocks.SHAFT_LIGHT_IRON);
            event.accept(ModBlocks.SHAFT_MEDIUM_IRON);

            event.accept(ModBlocks.SHAFT_LIGHT_DURALUMIN);
            event.accept(ModBlocks.SHAFT_MEDIUM_DURALUMIN);

            event.accept(ModBlocks.SHAFT_LIGHT_STEEL);
            event.accept(ModBlocks.SHAFT_MEDIUM_STEEL);

            event.accept(ModBlocks.SHAFT_LIGHT_TITANIUM);
            event.accept(ModBlocks.SHAFT_MEDIUM_TITANIUM);

            event.accept(ModBlocks.SHAFT_LIGHT_TUNGSTEN_CARBIDE);
            event.accept(ModBlocks.SHAFT_MEDIUM_TUNGSTEN_CARBIDE);

            event.accept(ModItems.BEVEL_GEAR.get());
            event.accept(ModItems.GEAR1_STEEL.get());
            event.accept(ModItems.GEAR2_STEEL.get());

            event.accept(ModItems.PULLEY.get());
            event.accept(ModItems.FLYWHEEL_LIGHT.get());
            event.accept(ModItems.COPPER_ROTOR.get());


            event.accept(ModBlocks.BEARING_BLOCK);
            event.accept(ModBlocks.MOTOR_ELECTRO);
            event.accept(ModBlocks.TACHOMETER);
            event.accept(ModItems.STEAM_ENGINE_ITEM);
            event.accept(ModBlocks.DROBITEL);
            event.accept(ModBlocks.STATOR_BLOCK);
            event.accept(ModItems.COPPER_COIL.get());



            //ЭНЕРГОСЕТЬ
            event.accept(ModBlocks.MACHINE_BATTERY);
            event.accept(ModItems.ENERGY_CELL_BASIC);

            event.accept(ModBlocks.WIRE_COATED);
            event.accept(ModBlocks.PAINTABLE_WIRE);
            event.accept(ModBlocks.CONNECTOR);
            event.accept(ModBlocks.MEDIUM_CONNECTOR);
            event.accept(ModBlocks.LARGE_CONNECTOR);

            event.accept(ModItems.CREATIVE_BATTERY);
            List<RegistryObject<Item>> batteriesToAdd = List.of(
                    ModItems.BATTERY,
                    ModItems.BATTERY_ADVANCED,
                    ModItems.BATTERY_LITHIUM,
                    ModItems.BATTERY_TRIXITE);
            for (RegistryObject<Item> batteryRegObj : batteriesToAdd) {
                Item item = batteryRegObj.get();
                if (item instanceof ModBatteryItem batteryItem) {
                    ItemStack emptyStack = new ItemStack(batteryItem);
                    event.accept(emptyStack);
                    ItemStack chargedStack = new ItemStack(batteryItem);
                    ModBatteryItem.setEnergy(chargedStack, batteryItem.getCapacity());
                    event.accept(chargedStack);
                }
            }

            event.accept(ModBlocks.SWITCH);
            event.accept(ModBlocks.CONVERTER_BLOCK);

            event.accept(ModBlocks.ELECTRO_FURNACE);




            //ЖИДКОСТИ
            event.accept(ModItems.CORRUPTED_BARREL_ITEM);
            event.accept(ModItems.LEAKING_BARREL_ITEM);
            event.accept(ModItems.IRON_BARREL_ITEM);
            event.accept(ModItems.STEEL_BARREL_ITEM);
            event.accept(ModItems.LEAD_BARREL_ITEM);
            event.accept(ModBlocks.FUEL_TANK_SMALL);
            event.accept(ModBlocks.FUEL_TANK_BIG);
            event.accept(ModBlocks.PAINTABLE_PIPE);
            event.accept(ModBlocks.PAINTABLE_WIRE);

          event.accept(ModItems.BOILER_ITEM);
        
          event.accept(ModBlocks.LOW_PRESSURE_STEAM_CONDENSER.get());
            event.accept(ModItems.PROTECTOR_STEEL);
            event.accept(ModItems.PROTECTOR_LEAD);
            event.accept(ModItems.PROTECTOR_TUNGSTEN);

            event.accept(ModBlocks.BRONZE_FLUID_PIPE);
            event.accept(ModBlocks.STEEL_FLUID_PIPE);
            event.accept(ModBlocks.LEAD_FLUID_PIPE);
            event.accept(ModBlocks.TUNGSTEN_FLUID_PIPE);
            event.accept(ModBlocks.PAINTABLE_PIPE);

            event.accept(ModItems.BOILER_ITEM);

            for (var entry : com.trd.api.fluids.ModFluids.getAllFluidDrops().values()) {
                event.accept(entry.get());
            }




            //ПЛАВИЛЬНИ
            event.accept(ModBlocks.JERNOVA);
            event.accept(ModBlocks.SMALL_SMELTER);

            event.accept(ModItems.HEATER_ITEM);
            event.accept(ModBlocks.SMELTER);
            event.accept(ModBlocks.CASTING_POT);
            event.accept(ModBlocks.CASTING_DESCENT);
            event.accept(ModItems.MOLD_EMPTY.get());
            event.accept(ModItems.MOLD_NUGGET.get());
            event.accept(ModItems.MOLD_PLATE.get());
            event.accept(ModItems.MOLD_INGOT.get());
            event.accept(ModItems.MOLD_BLOCK.get());
            event.accept(ModItems.MOLD_PICKAXE.get());
        }

        if (event.getTab() == ModCreativeTabs.trd_WEAPONS_TAB.get()) {
            event.accept(ModItems.CAST_PICKAXE_IRON);
            event.accept(ModItems.CAST_PICKAXE_STEEL);

            event.accept(ModItems.GRENADIER_GOGGLES);
            event.accept(ModBlocks.DET_MINER);
            event.accept(ModItems.DETONATOR);
            event.accept(ModItems.MULTI_DETONATOR);
            event.accept(ModItems.RANGE_DETONATOR);
            event.accept(ModItems.MORY_LAH);
            event.accept(ModItems.GRENADE);
            event.accept(ModItems.GRENADEHE);
            event.accept(ModItems.GRENADEFIRE);
            event.accept(ModItems.GRENADESMART);
            event.accept(ModItems.GRENADESLIME);
            event.accept(ModItems.GRENADE_IF);
            event.accept(ModItems.GRENADE_IF_HE);
            event.accept(ModItems.GRENADE_IF_SLIME);
            event.accept(ModItems.GRENADE_IF_FIRE);
            event.accept(ModItems.GRAVITY_GRENADE.get());
            event.accept(ModItems.GRENADE_NUC);
            event.accept(ModItems.TURRET_CHIP);
            event.accept(ModItems.TURRET_LIGHT_PORTATIVE_PLACER);
            event.accept(ModItems.MACHINEGUN);
            event.accept(ModBlocks.TURRET_LIGHT_PLACER);
            event.accept(ModBlocks.TROMBONE);
            event.accept(ModItems.AMMO_TURRET);
            event.accept(ModItems.AMMO_TURRET_HOLLOW);
            event.accept(ModItems.AMMO_TURRET_PIERCING);
            event.accept(ModItems.AMMO_TURRET_FIRE);
            event.accept(ModItems.AMMO_TURRET_RADIO);
            event.accept(ModItems.MISSILE_100MM);
            event.accept(ModItems.MISSILE_100MM_HE);
            event.accept(ModItems.MISSILE_100MM_FIRE);

        }

        if (event.getTab() == ModCreativeTabs.trd_RECOURSES_TAB.get()) {

            event.accept(ModItems.IRON_PLATE.get());
            event.accept(ModItems.TITANIUM_PLATE.get());
            event.accept(ModItems.STEEL_PLATE.get());
            event.accept(ModItems.TUNGSTEN_PLATE.get());
            event.accept(ModItems.LEAD_PLATE.get());
            event.accept(ModItems.ALUMINUM_PLATE.get());
            event.accept(ModItems.INDUSTRIAL_COPPER_PLATE.get());
            event.accept(ModItems.GOLD_PLATE.get());

            event.accept(ModItems.CAST_PICKAXE_IRON_BASE.get());
            event.accept(ModItems.CAST_PICKAXE_STEEL_BASE.get());

            for (Metal metal : MetallurgyRegistry.getAllMetals()) {
                ItemStack slagStack = SlagItem.createSlag(metal, MetalUnits2.UNITS_PER_INGOT);
                event.accept(slagStack);
            }
            event.accept(ModItems.ROPE.get());
            event.accept(ModItems.WOODEN_HANDLE.get());

            event.accept(ModItems.FIRE_SMES.get());
            event.accept(ModItems.DOLOMITE_SMES.get());
            event.accept(ModItems.FIREBRICK.get());
            event.accept(ModItems.REINFORCEDBRICK.get());

            event.accept(ModItems.CONGLOMERATE_CHUNK);
            event.accept(ModItems.HARD_ROCK);
            event.accept(ModItems.DOLOMITE_CHUNK);
            event.accept(ModItems.LIMESTONE_CHUNK);
            event.accept(ModItems.BAUXITE_CHUNK);
            event.accept(ModItems.ASBESTOS);
            event.accept(ModItems.CINNABAR);
            event.accept(ModItems.LIGNITE);
            event.accept(ModItems.FLUORITE);
            event.accept(ModItems.SEQUESTRUM);
            event.accept(ModItems.SULFUR);

            event.accept(ModItems.CONGLOMERATE_POWDER);
            event.accept(ModItems.DOLOMITE_POWDER);
            event.accept(ModItems.LIMESTONE_POWDER);
            event.accept(ModItems.BAUXITE_POWDER);

            event.accept(ModItems.FUEL_ASH.get());
            event.accept(ModItems.TRASH);

            event.accept(ModBlocks.LIGNITE_BLOCK);

        }

        if (event.getTab() == ModCreativeTabs.trd_NATURE_TAB.get()) {

            event.accept(ModBlocks.ASBESOTS_ORE.get());
            event.accept(ModBlocks.LIGNITE_ORE.get());
            event.accept(ModBlocks.CINNABAR_ORE.get());
            event.accept(ModBlocks.CINNABAR_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.FLUORITE_ORE.get());
            event.accept(ModBlocks.FLUORITE_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.SEQUESTRUM_ORE.get());
            event.accept(ModBlocks.SEQUESTRUM_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.SULFUR_ORE.get());
            event.accept(ModBlocks.SULFUR_ORE_DEEPSLATE.get());

            event.accept(ModBlocks.CONGLOMERATE.get());
            event.accept(ModBlocks.DEPLETED_CONGLOMERATE.get());

            event.accept(ModBlocks.DOLOMITE.get());
            event.accept(ModBlocks.LIMESTONE.get());
            event.accept(ModBlocks.SULFUR_CLUSTER.get());
            event.accept(ModBlocks.BAUXITE.get());
            event.accept(ModBlocks.MINERAL1.get());
            event.accept(ModBlocks.MINERAL3.get());
            event.accept(ModBlocks.SEQUOIA_BARK.get());
            event.accept(ModBlocks.SEQUOIA_HEARTWOOD.get());
            event.accept(ModBlocks.SEQUOIA_LEAVES.get());
            event.accept(ModBlocks.SEQUOIA_BIOME_MOSS.get());
            event.accept(ModBlocks.BASALT_ROUGH.get());
            event.accept(ModItems.DEPTH_WORM_SPAWN_EGG);
            event.accept(ModItems.DEPTH_WORM_BRUTAL_SPAWN_EGG);
            event.accept(ModBlocks.DEPTH_WORM_NEST);
            event.accept(ModBlocks.HIVE_SOIL);
            event.accept(ModBlocks.HIVE_ROOTS.get()); // Обычная версия
            event.accept(ModItems.GRENADIER_ZOMBIE_SPAWN_EGG.get());
        }

    }

    // Метод регистрации атрибутов (здоровье, урон и т.д.)
    private void entityAttributeEvent(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.DEPTH_WORM.get(), DepthWormEntity.createAttributes().build());
        event.put(ModEntities.TURRET_LIGHT.get(), TurretLightEntity.createAttributes().build());
        event.put(ModEntities.TURRET_LIGHT_LINKED.get(), TurretLightEntity.createAttributes().build());
        event.put(ModEntities.GRENADIER_ZOMBIE.get(), GrenadierZombieEntity.createAttributes().build());
        event.put(ModEntities.DEPTH_WORM_BRUTAL.get(), DepthWormBrutalEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onEntitySpawn(MobSpawnEvent.FinalizeSpawn event) {
        Level level = (Level) event.getLevel();
        // Если мы в Некрозе
        if (level.dimension().location().getPath().equals("necrosis")) {
            double spawnY = event.getY();
            Player nearestPlayer = level.getNearestPlayer(event.getX(), spawnY, event.getZ(), 128, false);

            // Если игрок далеко по вертикали (больше 50 блоков) - отменяем спавн
            if (nearestPlayer != null && Math.abs(nearestPlayer.getY() - spawnY) > 50) {
                event.setSpawnCancelled(true);
            }
        }
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Level> event) {
        if (!event.getObject().isClientSide) {
            event.addCapability(new ResourceLocation("trd", "hive_network_manager"),
                    new HiveNetworkManagerProvider());
            System.out.println("DEBUG: Capability Attached to Level!");
        }
    }

    @SubscribeEvent
    public void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        int burnTime = ModFuels.getBurnTimeForItem(event.getItemStack().getItem());
        if (burnTime >= 0) {
            event.setBurnTime(burnTime);
        }
    }

    @Mod.EventBusSubscriber(modid = "trd", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public class HiveEventHandler {
        @SubscribeEvent
        public static void onWorldTick(TickEvent.LevelTickEvent event) {
            // Обязательно проверяем сторону (Server) и фазу (END)
            if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
                HiveNetworkManager manager = HiveNetworkManager.get(serverLevel);
                if (manager != null) {
                    manager.tick(serverLevel);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide)
            return;
        if (event.getItemStack().is(Items.STICK)) {
            ServerLevel level = (ServerLevel) event.getLevel();
            BlockPos origin = event.getPos().above();

            RandomSource rand = level.random;
            int radius = 5 + rand.nextInt(4);
            int height = 5 + rand.nextInt(4);

            Set<BlockPos> veinBlocks = new HashSet<>();

            for (int x = -radius; x <= radius; x++) {
                for (int y = -height / 2; y < height / 2 + height % 2; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        double halfHeight = height / 2.0;
                        double yOffset = y;
                        double dist = (x * x) / (double) (radius * radius) +
                                (yOffset * yOffset) / (halfHeight * halfHeight) +
                                (z * z) / (double) (radius * radius);
                        if (dist > 1.0)
                            continue;

                        BlockPos pos = origin.offset(x, y, z);
                        BlockState existing = level.getBlockState(pos);
                        if (!existing.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                            veinBlocks.add(pos.immutable());
                        }
                    }
                }
            }

            if (veinBlocks.size() < 30) {
                event.getEntity().displayClientMessage(Component.literal("§cСлишком мало места для жилы!"), true);
                return;
            }

            var composition = com.trd.api.vein.VeinCompositionGenerator.generate(origin.getY(), rand);
            UUID veinId = VeinManager.get(level).registerVein(veinBlocks, composition, origin.getY());

            for (BlockPos pos : veinBlocks) {
                level.setBlock(pos, ModBlocks.CONGLOMERATE.get().defaultBlockState(), 2);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ConglomerateBlockEntity conglomerateBe) {
                    conglomerateBe.setVeinId(veinId);
                }
            }

            event.getEntity().displayClientMessage(
                    Component.literal("§aЖила: " + veinBlocks.size() + " блоков, основной металл: "
                            + composition.getPrimaryMetal()),
                    false);
        }
    }

    // ═══════════════════════════════════════════════════════
    // ОПЫТ ПРИ ДОБЫЧЕ РУД
    // ═══════════════════════════════════════════════════════

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Block block = event.getState().getBlock();

        // Проверяем, есть ли этот блок в списке руд с опытом
        for (ModBlockLootTableProvider.OreXpConfig config : ModBlockLootTableProvider.ORES_WITH_EXPERIENCE) {
            if (config.block().get() == block) {
                // Проверяем отсутствие Silk Touch
                ItemStack tool = event.getPlayer().getMainHandItem();
                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                    // Добавляем опыт к событию
                    int xp = event.getLevel().getRandom().nextIntBetweenInclusive(config.minXp(), config.maxXp());
                    event.setExpToDrop(event.getExpToDrop() + xp);
                }
                break; // нашли, дальше не ищем
            }
        }
    }

    // ═══════════════════════════════════════════════════════
}