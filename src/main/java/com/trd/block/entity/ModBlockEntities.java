package com.trd.block.entity;

import com.trd.block.entity.conglomerate.ConglomerateBlockEntity;
import com.trd.block.entity.deco.BeamCollisionBlockEntity;
import com.trd.block.entity.industrial.ElectricFurnaceBlockEntity;
import com.trd.block.entity.industrial.fluids.FluidBarrelBlockEntity;

import com.trd.block.entity.industrial.fluids.FluidPipeBlockEntity;

import com.trd.block.entity.industrial.MillstoneBlockEntity;
import com.trd.block.entity.industrial.casting.SmallSmelterBlockEntity;
import com.trd.block.entity.industrial.fluids.LowPressureSteamCondenserBlockEntity;
import com.trd.block.entity.industrial.rotation.BearingBlockEntity;
import com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity;
import com.trd.block.entity.industrial.rotation.ShaftBlockEntity;
import com.trd.block.entity.weapons.MissileTurretBlockEntity;
import com.trd.multiblock.industrial.*;

import com.trd.block.entity.industrial.casting.CastingDescentBlockEntity;
import com.trd.block.entity.industrial.casting.CastingPotBlockEntity;
import com.trd.block.entity.industrial.energy.*;

import com.trd.multiblock.system.MultiblockPartEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.trd.block.entity.hive.DepthWormNestBlockEntity;
import com.trd.block.entity.hive.HiveSoilBlockEntity;
import com.trd.block.entity.weapons.TurretLightPlacerBlockEntity;
import com.trd.main.MainRegistry;
import com.trd.block.basic.ModBlocks;

import static com.trd.block.basic.ModBlocks.*;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MainRegistry.MOD_ID);


    public static final RegistryObject<BlockEntityType<DepthWormNestBlockEntity>> DEPTH_WORM_NEST =
            BLOCK_ENTITIES.register("depth_worm_nest",
                    () -> BlockEntityType.Builder.of(DepthWormNestBlockEntity::new,
                            ModBlocks.DEPTH_WORM_NEST.get()).build(null));

    public static final RegistryObject<BlockEntityType<HiveSoilBlockEntity>> HIVE_SOIL =
            BLOCK_ENTITIES.register("hive_soil", () ->
                    BlockEntityType.Builder.of(HiveSoilBlockEntity::new, ModBlocks.HIVE_SOIL.get())
                            .build(null)
            );
    public static final RegistryObject<BlockEntityType<FuelTankBlockEntity>> FUEL_TANK_BE =
            BLOCK_ENTITIES.register("fuel_tank_big",
                    () -> BlockEntityType.Builder.of(FuelTankBlockEntity::new,
                            ModBlocks.FUEL_TANK_BIG.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.trd.multiblock.industrial.FuelTankSmallBlockEntity>> FUEL_TANK_SMALL_BE =
            BLOCK_ENTITIES.register("fuel_tank_small",
                    () -> BlockEntityType.Builder.of(com.trd.multiblock.industrial.FuelTankSmallBlockEntity::new,
                            ModBlocks.FUEL_TANK_SMALL.get()).build(null));

    public static final RegistryObject<BlockEntityType<MillstoneBlockEntity>> MILLSTONE =
            BLOCK_ENTITIES.register("millstone",
                    () -> BlockEntityType.Builder.of(MillstoneBlockEntity::new, ModBlocks.JERNOVA.get()).build(null));

    public static final RegistryObject<BlockEntityType<SmallSmelterBlockEntity>> SMALL_SMELTER_BE =
            BLOCK_ENTITIES.register("small_smelter",
                    () -> BlockEntityType.Builder.of(SmallSmelterBlockEntity::new, ModBlocks.SMALL_SMELTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ConglomerateBlockEntity>> CONGLOMERATE =
            BLOCK_ENTITIES.register("conglomerate",
                    () -> BlockEntityType.Builder.of(ConglomerateBlockEntity::new,
                            ModBlocks.CONGLOMERATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<SteelStorageBlockEntity>> STEEL_STORAGE_BE =
            BLOCK_ENTITIES.register("steel_storage",
                    () -> BlockEntityType.Builder.of(SteelStorageBlockEntity::new, ModBlocks.STEEL_STORAGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
            BLOCK_ENTITIES.register("electric_furnace",
                    () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRO_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<TurretLightPlacerBlockEntity>> TURRET_LIGHT_PLACER_BE =
            BLOCK_ENTITIES.register("turret_light_placer",
                    () -> BlockEntityType.Builder.of(TurretLightPlacerBlockEntity::new, ModBlocks.TURRET_LIGHT_PLACER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MissileTurretBlockEntity>> MISSILE_TURRET_BE =
            BLOCK_ENTITIES.register("missile_turret",
                    () -> BlockEntityType.Builder.of(MissileTurretBlockEntity::new,
                            TROMBONE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineBatteryBlockEntity>> MACHINE_BATTERY_BE =
            BLOCK_ENTITIES.register("machine_battery_be", () -> {
                // Превращаем список RegistryObject в массив Block[]
                Block[] validBlocks = ModBlocks.BATTERY_BLOCKS.stream()
                        .map(RegistryObject::get)
                        .toArray(Block[]::new);

                return BlockEntityType.Builder.<MachineBatteryBlockEntity>of(MachineBatteryBlockEntity::new, validBlocks)
                        .build(null);
            });

    public static final RegistryObject<BlockEntityType<WireBlockEntity>> WIRE_BE =
            BLOCK_ENTITIES.register("wire_be", () ->
                    BlockEntityType.Builder.<WireBlockEntity>of(WireBlockEntity::new, ModBlocks.WIRE_COATED.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<ConverterBlockEntity>> CONVERTER_BE =
            BLOCK_ENTITIES.register("converter_be",
                    () -> BlockEntityType.Builder.of(ConverterBlockEntity::new, ModBlocks.CONVERTER_BLOCK.get()).build(null));



    // 1. BlockEntity для части мультиблока
    public static final RegistryObject<BlockEntityType<MultiblockPartEntity>> MULTIBLOCK_PART = BLOCK_ENTITIES.register("multiblock_part",
            () -> BlockEntityType.Builder.of(MultiblockPartEntity::new, ModBlocks.MULTIBLOCK_PART.get()).build(null));

    // 2. BlockEntity для Нагревателя
    public static final RegistryObject<BlockEntityType<HeaterBlockEntity>> HEATER_BE = BLOCK_ENTITIES.register("heater_be",
            () -> BlockEntityType.Builder.of(HeaterBlockEntity::new, ModBlocks.HEATER.get()).build(null));
    public static final RegistryObject<BlockEntityType<BoilerBlockEntity>> BOILER_BE = BLOCK_ENTITIES.register("boiler_be",
            () -> BlockEntityType.Builder.of(BoilerBlockEntity::new, ModBlocks.BOILER.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.trd.multiblock.industrial.SteamEngineBlockEntity>> STEAM_ENGINE_BE = BLOCK_ENTITIES.register("steam_engine_be",
            () -> BlockEntityType.Builder.of(com.trd.multiblock.industrial.SteamEngineBlockEntity::new, ModBlocks.STEAM_ENGINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<SmelterBlockEntity>> SMELTER_BE = BLOCK_ENTITIES.register("smelter_be",
            () -> BlockEntityType.Builder.of(SmelterBlockEntity::new, ModBlocks.SMELTER.get()).build(null));


    public static final RegistryObject<BlockEntityType<SwitchBlockEntity>> SWITCH_BE =
            BLOCK_ENTITIES.register("switch_be", () ->
                    BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.SWITCH.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<CastingDescentBlockEntity>> CASTING_DESCENT = BLOCK_ENTITIES.register(
            "casting_descent",
            () -> BlockEntityType.Builder.of(CastingDescentBlockEntity::new, ModBlocks.CASTING_DESCENT.get()).build(null)
    );

    public static final RegistryObject<BlockEntityType<CastingPotBlockEntity>> CASTING_POT =
            BLOCK_ENTITIES.register("casting_pot",
                    () -> BlockEntityType.Builder.of(CastingPotBlockEntity::new,
                            com.trd.block.basic.ModBlocks.CASTING_POT.get()).build(null));

    public static final RegistryObject<BlockEntityType<ShaftBlockEntity>> SHAFT_BE = BLOCK_ENTITIES.register("shaft_be",
            () -> BlockEntityType.Builder.of(ShaftBlockEntity::new,
                    ModBlocks.SHAFT_LIGHT_IRON.get(),
                    ModBlocks.SHAFT_MEDIUM_IRON.get(),
                    ModBlocks.SHAFT_HEAVY_IRON.get(),
                    ModBlocks.SHAFT_LIGHT_DURALUMIN.get(),
                    ModBlocks.SHAFT_MEDIUM_DURALUMIN.get(),
                    ModBlocks.SHAFT_HEAVY_DURALUMIN.get(),
                    ModBlocks.SHAFT_LIGHT_STEEL.get(),
                    ModBlocks.SHAFT_MEDIUM_STEEL.get(),
                    ModBlocks.SHAFT_HEAVY_STEEL.get(),
                    ModBlocks.SHAFT_LIGHT_TITANIUM.get(),
                    ModBlocks.SHAFT_MEDIUM_TITANIUM.get(),
                    ModBlocks.SHAFT_HEAVY_TITANIUM.get(),
                    ModBlocks.SHAFT_LIGHT_TUNGSTEN_CARBIDE.get(),
                    ModBlocks.SHAFT_MEDIUM_TUNGSTEN_CARBIDE.get(),
                    ModBlocks.SHAFT_HEAVY_TUNGSTEN_CARBIDE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<BearingBlockEntity>> BEARING_BE = BLOCK_ENTITIES.register("bearing_be",
            () -> BlockEntityType.Builder.of(BearingBlockEntity::new, ModBlocks.BEARING_BLOCK.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<com.trd.block.entity.industrial.rotation.TachometerBlockEntity>> TACHOMETER_BE =
            BLOCK_ENTITIES.register("tachometer_be",
                    () -> BlockEntityType.Builder.of(com.trd.block.entity.industrial.rotation.TachometerBlockEntity::new, ModBlocks.TACHOMETER.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<MotorElectroBlockEntity>> MOTOR_ELECTRO_BE =
            BLOCK_ENTITIES.register("motor_electro",
                    () -> BlockEntityType.Builder.of(MotorElectroBlockEntity::new, ModBlocks.MOTOR_ELECTRO.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.trd.block.entity.industrial.rotation.StatorBlockEntity>> STATOR_BE =
            BLOCK_ENTITIES.register("stator",
                    () -> BlockEntityType.Builder.of(com.trd.block.entity.industrial.rotation.StatorBlockEntity::new, ModBlocks.STATOR_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE_BE =
            BLOCK_ENTITIES.register("fluid_pipe_be",
                    () -> BlockEntityType.Builder.of(FluidPipeBlockEntity::new,
                            // Просто перечисляем все наши трубы через запятую:
                            ModBlocks.BRONZE_FLUID_PIPE.get(),
                            ModBlocks.STEEL_FLUID_PIPE.get(),
                            ModBlocks.LEAD_FLUID_PIPE.get(),
                            ModBlocks.TUNGSTEN_FLUID_PIPE.get()
                            // ... и любые другие трубы, которые ты добавишь в будущем
                    ).build(null));


    public static final RegistryObject<BlockEntityType<BeamCollisionBlockEntity>> BEAM_COLLISION_BE =
            BLOCK_ENTITIES.register("beam_collision_be", () ->
                    BlockEntityType.Builder.of(BeamCollisionBlockEntity::new,
                            ModBlocks.BEAM_COLLISION.get()).build(null));

//    public static final RegistryObject<BlockEntityType<ShaftBlockEntity>> SHAFT_BLOCK_BE =
//            BLOCK_ENTITIES.register("shaft",
//                    () -> BlockEntityType.Builder.of(ShaftBlockEntity::new,
//                            ModBlocks.SHAFT_IRON.get(),
//                            ModBlocks.SHAFT_WOODEN.get() // и все другие валы
//                    ).build(null));

    public static final RegistryObject<BlockEntityType<ConnectorBlockEntity>> CONNECTOR_BE =
            BLOCK_ENTITIES.register("connector", () ->
                    BlockEntityType.Builder.of(ConnectorBlockEntity::new,
                            ModBlocks.CONNECTOR.get(),
                            ModBlocks.MEDIUM_CONNECTOR.get(),
                            ModBlocks.LARGE_CONNECTOR.get()
                    ).build(null));


    public static final RegistryObject<BlockEntityType<FluidBarrelBlockEntity>> FLUID_BARREL_BE =
            BLOCK_ENTITIES.register("fluid_barrel",
                    () -> BlockEntityType.Builder.of(FluidBarrelBlockEntity::new,
                            CORRUPTED_BARREL.get(), LEAKING_BARREL.get(), IRON_BARREL.get(),
                            STEEL_BARREL.get(), LEAD_BARREL.get()).build(null));

    public static final RegistryObject<BlockEntityType<LowPressureSteamCondenserBlockEntity>> LOW_PRESSURE_STEAM_CONDENSER_BE =
            BLOCK_ENTITIES.register("low_pressure_steam_condenser",
                    () -> BlockEntityType.Builder.of(
                            LowPressureSteamCondenserBlockEntity::new,
                            ModBlocks.LOW_PRESSURE_STEAM_CONDENSER.get()
                    ).build(null));
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}