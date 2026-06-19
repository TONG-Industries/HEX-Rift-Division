package com.trd.item;

import com.trd.api.rotation.ShaftMaterial;
import com.trd.entity.ModEntities;
import com.trd.entity.weapons.grenades.GrenadeIfType;
import com.trd.entity.weapons.grenades.GrenadeType;
import com.trd.event.LiquidMetalItem;
import com.trd.item.armor.GrenadierArmorMaterial;
import com.trd.item.armor.GrenadierGogglesItem;
import com.trd.item.conglomerates.ConglomerateItem;
import com.trd.item.energy.EnergyCellItem;
import com.trd.item.food.FoodZamaz;
import com.trd.event.SlagItem;
import com.trd.item.mobs.DepthWormBrutalSpawnEggItem;
import com.trd.item.mobs.MoryLahItem;
import com.trd.item.energy.StatorCoilItem;
import com.trd.item.energy.WireCoilItem;
import com.trd.item.rotation.GearItem;
import com.trd.item.tools.*;
import com.trd.item.tools.cast_pickaxes.materials.CastPickaxeIronItem;
import com.trd.item.tools.cast_pickaxes.materials.CastPickaxeSteelItem;
import com.trd.item.weapons.grenades.GravityGrenadeItem;
import com.trd.item.weapons.grenades.GrenadeIfItem;
import com.trd.item.weapons.grenades.GrenadeItem;
import com.trd.item.weapons.grenades.GrenadeNucItem;
import com.trd.item.weapons.missiles.MissileItem;
import com.trd.multiblock.system.MultiblockBlockItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.trd.block.basic.ModBlocks;
import com.trd.item.activators.DetonatorItem;
import com.trd.item.activators.MultiDetonatorItem;
import com.trd.item.activators.RangeDetonatorItem;
import com.trd.item.energy.ItemCreativeBattery;
import com.trd.item.energy.ModBatteryItem;
import com.trd.item.guns.MachineGunItem;
import com.trd.item.mobs.DepthWormSpawnEggItem;
import com.trd.item.weapons.ammo.AmmoTurretItem;
import com.trd.item.weapons.turrets.TurretChipItem;
import com.trd.item.weapons.turrets.TurretLightPortativePlacer;
import com.trd.item.weapons.turrets.TurretLightPlacerBlockItem;
import com.trd.main.MainRegistry;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MainRegistry.MOD_ID);


    public static final RegistryObject<Item> CORRUPTED_BARREL_ITEM = ITEMS.register("corrupted_barrel",
            () -> new BlockItem(ModBlocks.CORRUPTED_BARREL.get(), new Item.Properties()));

    public static final RegistryObject<Item> LEAKING_BARREL_ITEM = ITEMS.register("leaking_barrel",
            () -> new BlockItem(ModBlocks.LEAKING_BARREL.get(), new Item.Properties()));
    public static final RegistryObject<Item> IRON_BARREL_ITEM = ITEMS.register("iron_barrel",
            () -> new BlockItem(ModBlocks.IRON_BARREL.get(), new Item.Properties()));
    public static final RegistryObject<Item> STEEL_BARREL_ITEM = ITEMS.register("steel_barrel",
            () -> new BlockItem(ModBlocks.STEEL_BARREL.get(), new Item.Properties()));
    public static final RegistryObject<Item> LEAD_BARREL_ITEM = ITEMS.register("lead_barrel",
            () -> new BlockItem(ModBlocks.LEAD_BARREL.get(), new Item.Properties()));

    public static final RegistryObject<Item> PROTECTOR_STEEL = ITEMS.register("protector_steel",
            () -> new ProtectorItem(720, 40, 5, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PROTECTOR_LEAD = ITEMS.register("protector_lead",
            () -> new ProtectorItem(-500, 225, 25, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PROTECTOR_TUNGSTEN = ITEMS.register("protector_tungsten",
            () -> new ProtectorItem(1700, 270, 30, new Item.Properties().stacksTo(1)));

    //ОБЫЧНЫЕ ПРЕДМЕТЫ
    public static final RegistryObject<Item> DEPTH_WORM_SPAWN_EGG = ITEMS.register("depth_worm_spawn_egg",
            () -> new DepthWormSpawnEggItem(new Item.Properties()));
    public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ALUMINUM_PLATE = ITEMS.register("aluminum_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STEEL_PLATE = ITEMS.register("steel_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INDUSTRIAL_COPPER_PLATE = ITEMS.register("industrial_copper_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TUNGSTEN_PLATE = ITEMS.register("tungsten_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LEAD_PLATE = ITEMS.register("lead_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_PLATE = ITEMS.register("gold_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_PLATE = ITEMS.register("titanium_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LIGNITE = ITEMS.register("lignite",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SULFUR = ITEMS.register("sulfur",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SEQUESTRUM = ITEMS.register("sequestrum",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FLUORITE = ITEMS.register("fluorite",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GRAVITY_GRENADE = ITEMS.register("gravity_grenade",
            () -> new GravityGrenadeItem(new Item.Properties().stacksTo(16), ModEntities.GRAVITY_GRENADE_PROJECTILE));

    public static final RegistryObject<Item> MISSILE_100MM = ITEMS.register("missile_100mm",
            () -> new MissileItem(new Item.Properties(), "standard", 25.0f, 1.67f));

    public static final RegistryObject<Item> MISSILE_100MM_HE = ITEMS.register("missile_100mm_he",
            () -> new MissileItem(new Item.Properties(), "he", 40.0f, 1.67f));

    public static final RegistryObject<Item> MISSILE_100MM_FIRE = ITEMS.register("missile_100mm_fire",
            () -> new MissileItem(new Item.Properties(), "fire", 20.0f, 1.67f));
    public static final RegistryObject<Item> DEPTH_WORM_BRUTAL_SPAWN_EGG = ITEMS.register("depth_worm_brutal_spawn_egg",
            () -> new DepthWormBrutalSpawnEggItem(new Item.Properties()));

    public static final RegistryObject<Item> SLAG = ITEMS.register("slag",
            () -> new SlagItem(new Item.Properties()));
    public static final RegistryObject<Item> POKER = ITEMS.register("poker",
            () -> new PokerItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(128)));
    // ИНСТРУМЕНТЫ
    public static final RegistryObject<Item> BELT = ITEMS.register("belt",
            () -> new com.trd.item.rotation.BeltItem(new Item.Properties()));

    // Базовый предмет-капля (без привязки к жидкости, используется как "none" / заглушка)
    public static final RegistryObject<Item> FLUID_IDENTIFIER_DROP = ITEMS.register("fluid_identifier_drop",
            () -> new Item(new Item.Properties()));

    // ШКИВЫ
    public static final RegistryObject<Item> PULLEY = ITEMS.register("pulley",
            () -> new com.trd.item.rotation.PulleyItem(new Item.Properties(),
                    1, // Размер для BlockState (PULLEY_SIZE = 1)
                    12, // Физический диаметр в пикселях (для передаточного числа)
                    com.trd.api.rotation.ShaftMaterial.IRON,
                    com.trd.api.rotation.ShaftDiameter.LIGHT,
                    com.trd.api.rotation.ShaftDiameter.MEDIUM));

    public static final RegistryObject<Item> FLYWHEEL_LIGHT = ITEMS.register("flywheel_light",
            () -> new com.trd.item.rotation.FlywheelItem(new Item.Properties(),
                    com.trd.api.rotation.ShaftDiameter.LIGHT,
                    com.trd.api.rotation.ShaftDiameter.MEDIUM));

    public static final RegistryObject<Item> SCREWDRIVER = ITEMS.register("screwdriver",
            () -> new ScrewdriverItem(new Item.Properties().stacksTo(1).durability(256)));

    public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
            () -> new Item(new Item.Properties().stacksTo(1).durability(256)));
    // Прочность как у железных инструментов
    public static final RegistryObject<Item> RANGE_DETONATOR = ITEMS.register("range_detonator",
            () -> new RangeDetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> MULTI_DETONATOR = ITEMS.register("multi_detonator",
            () -> new MultiDetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> DETONATOR = ITEMS.register("detonator",
            () -> new DetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> FIREBRICK = ITEMS.register("firebrick",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REINFORCEDBRICK = ITEMS.register("reinforcedbrick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CAST_PICKAXE_IRON_BASE = ITEMS.register("cast_pickaxe_iron_base",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CAST_PICKAXE_STEEL_BASE = ITEMS.register("cast_pickaxe_steel_base",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WOODEN_HANDLE = ITEMS.register("wooden_handle",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ROPE = ITEMS.register("rope",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LIQUID_METAL = ITEMS.register("liquid_metal",
            () -> new LiquidMetalItem(new Item.Properties()));

    public static final RegistryObject<Item> CAST_PICKAXE_IRON = ITEMS.register("cast_pickaxe_iron",
            () -> new CastPickaxeIronItem(new Item.Properties()));

    public static final RegistryObject<Item> CAST_PICKAXE_STEEL = ITEMS.register("cast_pickaxe_steel",
            () -> new CastPickaxeSteelItem(new Item.Properties()));

    public static final RegistryObject<Item> MOLD_INGOT = ITEMS.register("mold_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOLD_PLATE = ITEMS.register("mold_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOLD_PICKAXE = ITEMS.register("mold_pickaxe",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOLD_EMPTY= ITEMS.register("mold_empty",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<GrenadierGogglesItem> GRENADIER_GOGGLES = ITEMS.register("grenadier_goggles",
            () -> new GrenadierGogglesItem(GrenadierArmorMaterial.GRENADIER, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<ForgeSpawnEggItem> GRENADIER_ZOMBIE_SPAWN_EGG = ITEMS.register("grenadier_zombie_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GRENADIER_ZOMBIE, 0x4C7F52, 0x8B0000,
                    new Item.Properties()));

    public static final RegistryObject<Item> MOLD_NUGGET = ITEMS.register("mold_nugget",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOLD_BLOCK= ITEMS.register("mold_block",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WIRE_COIL = ITEMS.register("wire_coil",
            () -> new WireCoilItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> COPPER_COIL = ITEMS.register("copper_coil",
            () -> new StatorCoilItem(new Item.Properties().stacksTo(64), "copper", 1, 500, 20, 3.0f));

    public static final RegistryObject<Item> BEAM_PLACER = ITEMS.register("beam_placer",
            () -> new BeamPlacerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FLUID_IDENTIFIER = ITEMS.register("fluid_identifier",
            () -> new FluidIdentifierItem(new Item.Properties()));

    public static final RegistryObject<Item> FUEL_ASH = ITEMS.register("fuel_ash",
            () -> new Item(new Item.Properties()));


    public static final RegistryObject<Item> INFINITE_FLUID_BARREL = ITEMS.register("infinite_fluid_barrel",
            () -> new com.trd.item.tools.InfiniteFluidBarrelItem(new Item.Properties()));



    //ОРУЖИЕ
    public static final RegistryObject<Item> MACHINEGUN = ITEMS.register("machinegun",
            () -> new MachineGunItem(new Item.Properties()));

    public static final RegistryObject<Item> TURRET_CHIP = ITEMS.register("turret_chip",
            () -> new TurretChipItem(new Item.Properties()));

    public static final RegistryObject<Item> TURRET_LIGHT_PORTATIVE_PLACER = ITEMS.register("turret_light_portative_placer",
            () -> new TurretLightPortativePlacer(new Item.Properties().stacksTo(1)));


    // Conglomerate items
    public static final RegistryObject<Item> CONGLOMERATE_CHUNK = ITEMS.register("conglomerate_chunk",
            () -> new ConglomerateItem(new Item.Properties()));

    public static final RegistryObject<Item> HARD_ROCK = ITEMS.register("hard_rock",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DOLOMITE_SMES = ITEMS.register("dolomite_smes",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIRE_SMES = ITEMS.register("fire_smes",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LIMESTONE_CHUNK = ITEMS.register("limestone_chunk",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LIMESTONE_POWDER = ITEMS.register("limestone_powder",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BAUXITE_CHUNK = ITEMS.register("bauxite_chunk",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BAUXITE_POWDER = ITEMS.register("bauxite_powder",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DOLOMITE_CHUNK = ITEMS.register("dolomite_chunk",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DOLOMITE_POWDER= ITEMS.register("dolomite_powder",
            () -> new Item(new Item.Properties()));

    // BlockItems
    public static final RegistryObject<Item> CONGLOMERATE_BLOCK_ITEM = ITEMS.register("conglomerate",
            () -> new BlockItem(ModBlocks.CONGLOMERATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEPLETED_CONGLOMERATE_BLOCK_ITEM = ITEMS.register("depleted_conglomerate",
            () -> new BlockItem(ModBlocks.DEPLETED_CONGLOMERATE.get(), new Item.Properties()));


    //БЛОК-АЙТЕМЫ
//    public static final RegistryObject<Item> MOTOR_ELECTRO_ITEM = ITEMS.register("motor_electro",
//            () -> new MotorElectroBlockItem(ModBlocks.MOTOR_ELECTRO.get(), new Item.Properties()));
//
//    public static final RegistryObject<Item> WIND_GEN_FLUGER = ITEMS.register("wind_gen_fluger",
//            () -> new WindGenFlugerBlockItem(ModBlocks.WIND_GEN_FLUGER.get(), new Item.Properties()));
//
//    public static final RegistryObject<Item> SHAFT_IRON_ITEM = ITEMS.register("shaft_iron",
//            () -> new ShaftBlockItem(ModBlocks.SHAFT_IRON.get(), new Item.Properties()));
//
//    public static final RegistryObject<Item> SHAFT_WOODEN_ITEM = ITEMS.register("shaft_wooden",
//            () -> new ShaftBlockItem(ModBlocks.SHAFT_WOODEN.get(), new Item.Properties()));
//
  public static final RegistryObject<Item> TURRET_LIGHT_PLACER_ITEM = ITEMS.register("turret_light_placer",
           () -> new TurretLightPlacerBlockItem(ModBlocks.TURRET_LIGHT_PLACER.get(), new Item.Properties()));
//
//    public static final RegistryObject<Item> DRILL_HEAD_ITEM = ITEMS.register("drill_head_item",
//            () -> new DrillHeadItem(ModBlocks.DRILL_HEAD.get(), new Item.Properties()));

    public static final RegistryObject<Item> HEATER_ITEM = ITEMS.register("heater",
            () -> new MultiblockBlockItem(ModBlocks.HEATER.get(), new Item.Properties()));
    public static final RegistryObject<Item> BOILER_ITEM = ITEMS.register("boiler",
            () -> new MultiblockBlockItem(ModBlocks.BOILER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SMELTER_ITEM = ITEMS.register("smelter",
            () -> new MultiblockBlockItem(ModBlocks.SMELTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> FUEL_TANK_BIG_ITEM = ITEMS.register("fuel_tank_big",
            () -> new MultiblockBlockItem(ModBlocks.FUEL_TANK_BIG.get(), new Item.Properties()));

    public static final RegistryObject<Item> SMELTER_ITEM = ITEMS.register("smelter",
            () -> new MultiblockBlockItem(ModBlocks.SMELTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> FUEL_TANK_BIG_ITEM = ITEMS.register("fuel_tank_big",
            () -> new MultiblockBlockItem(ModBlocks.FUEL_TANK_BIG.get(), new Item.Properties()));

    public static final RegistryObject<Item> STATOR_ITEM = ITEMS.register("stator",
            () -> new com.trd.item.StatorMultiblockItem(ModBlocks.STATOR_BLOCK.get(), new Item.Properties()));




    //ПАТРОНЫ
    public static final RegistryObject<Item> AMMO_TURRET = ITEMS.register("ammo_turret",
            () -> new AmmoTurretItem(new Item.Properties(), 8.0f, 3.0f, false));

    public static final RegistryObject<Item> AMMO_TURRET_PIERCING = ITEMS.register("ammo_turret_piercing",
            () -> new AmmoTurretItem(new Item.Properties(), 12.0f, 3.0f, true));

    public static final RegistryObject<Item> AMMO_TURRET_HOLLOW = ITEMS.register("ammo_turret_hollow",
            () -> new AmmoTurretItem(new Item.Properties(), 8.0f, 3.0f, false));

    public static final RegistryObject<Item> AMMO_TURRET_FIRE = ITEMS.register("ammo_turret_fire",
            () -> new AmmoTurretItem(new Item.Properties(), 6.0f, 3.0f, false));

    public static final RegistryObject<Item> AMMO_TURRET_RADIO = ITEMS.register("ammo_turret_radio",
            () -> new AmmoTurretItem(new Item.Properties(), 9.0f, 3.0f, false));




    //ГРАНАТЫ
    public static final RegistryObject<Item> GRENADE = ITEMS.register("grenade",
            () -> new GrenadeItem(new Item.Properties().stacksTo(16), GrenadeType.STANDARD, ModEntities.GRENADE_PROJECTILE));

    public static final RegistryObject<Item> GRENADEHE = ITEMS.register("grenadehe",
            () -> new GrenadeItem(new Item.Properties().stacksTo(16), GrenadeType.HE, ModEntities.GRENADEHE_PROJECTILE));

    public static final RegistryObject<Item> GRENADEFIRE = ITEMS.register("grenadefire",
            () -> new GrenadeItem(new Item.Properties().stacksTo(16), GrenadeType.FIRE, ModEntities.GRENADEFIRE_PROJECTILE));

    public static final RegistryObject<Item> GRENADESLIME = ITEMS.register("grenadeslime",
            () -> new GrenadeItem(new Item.Properties().stacksTo(16), GrenadeType.SLIME, ModEntities.GRENADESLIME_PROJECTILE));

    public static final RegistryObject<Item> GRENADESMART = ITEMS.register("grenadesmart",
            () -> new GrenadeItem(new Item.Properties().stacksTo(16), GrenadeType.SMART, ModEntities.GRENADESMART_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF = ITEMS.register("grenade_if",
            () -> new GrenadeIfItem(new Item.Properties().stacksTo(16), GrenadeIfType.GRENADE_IF, ModEntities.GRENADE_IF_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_HE = ITEMS.register("grenade_if_he",
            () -> new GrenadeIfItem(new Item.Properties().stacksTo(16), GrenadeIfType.GRENADE_IF_HE, ModEntities.GRENADE_IF_HE_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_SLIME = ITEMS.register("grenade_if_slime",
            () -> new GrenadeIfItem(new Item.Properties().stacksTo(16), GrenadeIfType.GRENADE_IF_SLIME, ModEntities.GRENADE_IF_SLIME_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_FIRE = ITEMS.register("grenade_if_fire",
            () -> new GrenadeIfItem(new Item.Properties().stacksTo(16), GrenadeIfType.GRENADE_IF_FIRE, ModEntities.GRENADE_IF_FIRE_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_NUC = ITEMS.register("grenade_nuc",
            () -> new GrenadeNucItem(new Item.Properties().stacksTo(16), ModEntities.GRENADE_NUC_PROJECTILE));

    public static final RegistryObject<Item> MORY_LAH = ITEMS.register("mory_lah",
            () -> new MoryLahItem(new Item.Properties()));

    public static final RegistryObject<Item> TEXTURE_TEST = ITEMS.register("texture_test",
            () -> new Item(new Item.Properties()));


    //ЕДА
    public static final RegistryObject<Item> MORY_FOOD = ITEMS.register("mory_food",
            () -> new FoodZamaz(new FoodZamaz.Builder()
                    .nutrition(4)
                    .saturation(2.0F)));
    public static final RegistryObject<Item> COFFEE = ITEMS.register("coffee",
            () -> new FoodZamaz(new FoodZamaz.Builder()
                    .nutrition(2)
                    .saturation(0.5F)
                    .alwaysEat()
                    .eatDuration(16)
                    .effect(MobEffects.MOVEMENT_SPEED, 30, 1)));





    //БАТАРЕИ
    public static final RegistryObject<Item> ENERGY_CELL_BASIC = ITEMS.register("energy_cell_basic",
            () -> new EnergyCellItem(new Item.Properties().stacksTo(1),
                    1_000_000L,     // capacity
                    5_000L,         // chargingSpeed
                    5_000L));       // unchargingSpeed

    public static final RegistryObject<Item> CREATIVE_BATTERY = ITEMS.register("battery_creative",
            () -> new ItemCreativeBattery(new Item.Properties()));

    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new ModBatteryItem(new Item.Properties(), 5000, 100, 100));

    public static final RegistryObject<Item> BATTERY_ADVANCED = ITEMS.register("battery_advanced",
            () -> new ModBatteryItem(new Item.Properties(), 20000, 500, 500));

    public static final RegistryObject<Item> BATTERY_LITHIUM = ITEMS.register("battery_lithium",
            () -> new ModBatteryItem(new Item.Properties(), 250000, 1000, 1000));

    public static final RegistryObject<Item> BATTERY_TRIXITE = ITEMS.register("battery_trixite",
            () -> new ModBatteryItem(new Item.Properties(), 5000000, 40000, 200000));

    //ШЕСТЕРНИ

    // =========================================
    // КИНЕТИЧЕСКИЕ ШЕСТЕРНИ (Размер 1 - Малые)
    // =========================================
//    public static final RegistryObject<Item> GEAR1_IRON = ITEMS.register("gear1_iron",
//            () -> new GearItem(new Item.Properties(), 1, ShaftMaterial.IRON));
//
//    public static final RegistryObject<Item> GEAR1_DURALUMIN = ITEMS.register("gear1_duralumin",
//            () -> new GearItem(new Item.Properties(), 1, ShaftMaterial.DURALUMIN));

    public static final RegistryObject<Item> GEAR1_STEEL = ITEMS.register("gear1_steel",
            () -> new GearItem(new Item.Properties(), 1, ShaftMaterial.STEEL));

    public static final RegistryObject<Item> BEVEL_GEAR = ITEMS.register("bevel_gear",
            () -> new com.trd.item.rotation.BevelGearItem(new Item.Properties(), ShaftMaterial.STEEL));

    public static final RegistryObject<Item> COPPER_ROTOR = ITEMS.register("copper_rotor",
            () -> new com.trd.item.rotation.RotorItem(new Item.Properties()));

//    public static final RegistryObject<Item> GEAR1_TITANIUM = ITEMS.register("gear1_titanium",
//            () -> new GearItem(new Item.Properties(), 1, ShaftMaterial.TITANIUM));
//
//    public static final RegistryObject<Item> GEAR1_TUNGSTEN_CARBIDE = ITEMS.register("gear1_tungsten_carbide",
//            () -> new GearItem(new Item.Properties(), 1, ShaftMaterial.TUNGSTEN_CARBIDE));

    // =========================================
    // КИНЕТИЧЕСКИЕ ШЕСТЕРНИ (Размер 2 - Средние)
    // =========================================
//    public static final RegistryObject<Item> GEAR2_IRON = ITEMS.register("gear2_iron",
//            () -> new GearItem(new Item.Properties(), 2, ShaftMaterial.IRON));
//
//    public static final RegistryObject<Item> GEAR2_DURALUMIN = ITEMS.register("gear2_duralumin",
//            () -> new GearItem(new Item.Properties(), 2, ShaftMaterial.DURALUMIN));
//
    public static final RegistryObject<Item> GEAR2_STEEL = ITEMS.register("gear2_steel",
            () -> new GearItem(new Item.Properties(), 2, ShaftMaterial.STEEL));
//
//    public static final RegistryObject<Item> GEAR2_TITANIUM = ITEMS.register("gear2_titanium",
//            () -> new GearItem(new Item.Properties(), 2, ShaftMaterial.TITANIUM));
//
//    public static final RegistryObject<Item> GEAR2_TUNGSTEN_CARBIDE = ITEMS.register("gear2_tungsten_carbide",
//            () -> new GearItem(new Item.Properties(), 2, ShaftMaterial.TUNGSTEN_CARBIDE));
//
//    // =========================================
//    // КИНЕТИЧЕСКИЕ ШЕСТЕРНИ (Размер 3 - Большие)
//    // =========================================
//    public static final RegistryObject<Item> GEAR3_IRON = ITEMS.register("gear3_iron",
//            () -> new GearItem(new Item.Properties(), 3, ShaftMaterial.IRON));
//
//    public static final RegistryObject<Item> GEAR3_DURALUMIN = ITEMS.register("gear3_duralumin",
//            () -> new GearItem(new Item.Properties(), 3, ShaftMaterial.DURALUMIN));
//
//    public static final RegistryObject<Item> GEAR3_STEEL = ITEMS.register("gear3_steel",
//            () -> new GearItem(new Item.Properties(), 3, ShaftMaterial.STEEL));
//
//    public static final RegistryObject<Item> GEAR3_TITANIUM = ITEMS.register("gear3_titanium",
//            () -> new GearItem(new Item.Properties(), 3, ShaftMaterial.TITANIUM));
//
//    public static final RegistryObject<Item> GEAR3_TUNGSTEN_CARBIDE = ITEMS.register("gear3_tungsten_carbide",
//            () -> new GearItem(new Item.Properties(), 3, ShaftMaterial.TUNGSTEN_CARBIDE));
}