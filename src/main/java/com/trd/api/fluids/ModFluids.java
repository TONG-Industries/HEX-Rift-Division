package com.trd.api.fluids;

import com.trd.api.fluids.system.BaseFluidType;
import com.trd.api.fluids.system.FluidDropItem;
import com.trd.api.fluids.system.UnbucketableLiquidBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "trd");
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.Keys.FLUIDS, "trd");
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.Keys.BLOCKS, "trd");
    public static final DeferredRegister<Item> FLUID_DROP_ITEMS = DeferredRegister.create(ForgeRegistries.Keys.ITEMS, "trd");

    private static final ResourceLocation WATER_STILL = new ResourceLocation("block/water_still");
    private static final ResourceLocation WATER_FLOW = new ResourceLocation("block/water_flow");
    private static final ResourceLocation DEFAULT_GUI_TEXTURE = new ResourceLocation("trd", "textures/gui/fluid/fluid_base.png");
    private static final Map<String, RegistryObject<Item>> FLUID_DROPS = new HashMap<>();

    public static final RegistryObject<FluidType> HYDROGEN_PEROXIDE_TYPE = FLUID_TYPES.register("hydrogen_peroxide",
            () -> new BaseFluidType(FluidType.Properties.create().density(1450).viscosity(1100).temperature(300),
                    WATER_STILL, WATER_FLOW,
                    new ResourceLocation("trd", "textures/gui/fluid/hydrogen_peroxide.png"),
                    0xc2b590, 20, 40));

    public static final RegistryObject<FlowingFluid> HYDROGEN_PEROXIDE_SOURCE = FLUIDS.register("hydrogen_peroxide",
            () -> new ForgeFlowingFluid.Source(ModFluids.HYDROGEN_PEROXIDE_PROPS));
    public static final RegistryObject<FlowingFluid> HYDROGEN_PEROXIDE_FLOWING = FLUIDS.register("flowing_hydrogen_peroxide",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.HYDROGEN_PEROXIDE_PROPS));
    public static final RegistryObject<LiquidBlock> HYDROGEN_PEROXIDE_BLOCK = BLOCKS.register("hydrogen_peroxide_block",
            () -> new UnbucketableLiquidBlock(HYDROGEN_PEROXIDE_SOURCE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).replaceable().noCollission().strength(100.0F).noLootTable()));
    private static final ForgeFlowingFluid.Properties HYDROGEN_PEROXIDE_PROPS = new ForgeFlowingFluid.Properties(
            HYDROGEN_PEROXIDE_TYPE, HYDROGEN_PEROXIDE_SOURCE, HYDROGEN_PEROXIDE_FLOWING).block(HYDROGEN_PEROXIDE_BLOCK);

    public static final RegistryObject<FluidType> SULFURIC_ACID_TYPE = FLUID_TYPES.register("sulfuric_acid",
            () -> new BaseFluidType(FluidType.Properties.create().density(1830).viscosity(2000).temperature(300),
                    WATER_STILL, WATER_FLOW,
                    new ResourceLocation("trd", "textures/gui/fluid/sulfuric_acid.png"),
                    0xbcc13f, 20, 80));

    public static final RegistryObject<FlowingFluid> SULFURIC_ACID_SOURCE = FLUIDS.register("sulfuric_acid",
            () -> new ForgeFlowingFluid.Source(ModFluids.SULFURIC_ACID_PROPS));
    public static final RegistryObject<FlowingFluid> SULFURIC_ACID_FLOWING = FLUIDS.register("flowing_sulfuric_acid",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.SULFURIC_ACID_PROPS));
    public static final RegistryObject<LiquidBlock> SULFURIC_ACID_BLOCK = BLOCKS.register("sulfuric_acid_block",
            () -> new UnbucketableLiquidBlock(SULFURIC_ACID_SOURCE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).replaceable().noCollission().strength(100.0F).noLootTable()));
    private static final ForgeFlowingFluid.Properties SULFURIC_ACID_PROPS = new ForgeFlowingFluid.Properties(
            SULFURIC_ACID_TYPE, SULFURIC_ACID_SOURCE, SULFURIC_ACID_FLOWING).block(SULFURIC_ACID_BLOCK);

    public static final RegistryObject<FluidType> NATURAL_GAS_TYPE = FLUID_TYPES.register("natural_gas",
            () -> new BaseFluidType(FluidType.Properties.create().density(-800).viscosity(500).temperature(300),
                    WATER_STILL, WATER_FLOW,
                    new ResourceLocation("trd", "textures/gui/fluid/natural_gas.png"),
                    0xa3b8c4, 20, 0));

    public static final RegistryObject<FlowingFluid> NATURAL_GAS_SOURCE = FLUIDS.register("natural_gas",
            () -> new ForgeFlowingFluid.Source(ModFluids.NATURAL_GAS_PROPS));
    public static final RegistryObject<FlowingFluid> NATURAL_GAS_FLOWING = FLUIDS.register("flowing_natural_gas",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.NATURAL_GAS_PROPS));
    private static final ForgeFlowingFluid.Properties NATURAL_GAS_PROPS = new ForgeFlowingFluid.Properties(
            NATURAL_GAS_TYPE, NATURAL_GAS_SOURCE, NATURAL_GAS_FLOWING);

    public static final RegistryObject<FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
            () -> new BaseFluidType(FluidType.Properties.create().density(-1000).viscosity(200).temperature(373),
                    WATER_STILL, WATER_FLOW,
                    new ResourceLocation("trd", "textures/gui/fluid/steam.png"),
                    0x88FFFFFF, 100, 0));

    public static final RegistryObject<FlowingFluid> STEAM_SOURCE = FLUIDS.register("steam",
            () -> new ForgeFlowingFluid.Source(ModFluids.STEAM_PROPS));
    public static final RegistryObject<FlowingFluid> STEAM_FLOWING = FLUIDS.register("flowing_steam",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.STEAM_PROPS));
    private static final ForgeFlowingFluid.Properties STEAM_PROPS = new ForgeFlowingFluid.Properties(
            STEAM_TYPE, STEAM_SOURCE, STEAM_FLOWING);


    public static final RegistryObject<FluidType> LOW_PRESSURE_STEAM_TYPE = FLUID_TYPES.register("low_pressure_steam",
            () -> new BaseFluidType(FluidType.Properties.create().density(-1000).viscosity(200).temperature(300),
                    WATER_STILL, WATER_FLOW,
                    new ResourceLocation("trd", "textures/gui/fluid/low_pressure_steam.png"),
                    0x636a7c, 20, 0));

    public static final RegistryObject<FlowingFluid> LOW_PRESSURE_STEAM_SOURCE = FLUIDS.register("low_pressure_steam",
            () -> new ForgeFlowingFluid.Source(ModFluids.LOW_PRESSURE_STEAM_PROPS));
    public static final RegistryObject<FlowingFluid> LOW_PRESSURE_STEAM_FLOWING = FLUIDS.register("flowing_low_pressure_steam",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.LOW_PRESSURE_STEAM_PROPS));
    private static final ForgeFlowingFluid.Properties LOW_PRESSURE_STEAM_PROPS = new ForgeFlowingFluid.Properties(
            LOW_PRESSURE_STEAM_TYPE, LOW_PRESSURE_STEAM_SOURCE, LOW_PRESSURE_STEAM_FLOWING);

    public static final RegistryObject<FluidType> SMOKE_TYPE = FLUID_TYPES.register("smoke",
            () -> new BaseFluidType(FluidType.Properties.create().density(-1200).viscosity(400).temperature(400),
                    WATER_STILL, WATER_FLOW,
                    new ResourceLocation("trd", "textures/gui/fluid/smoke.png"),
                    0xFF3A3A3A, 20, 0));

    public static final RegistryObject<FlowingFluid> SMOKE_SOURCE = FLUIDS.register("smoke",
            () -> new ForgeFlowingFluid.Source(ModFluids.SMOKE_PROPS));
    public static final RegistryObject<FlowingFluid> SMOKE_FLOWING = FLUIDS.register("flowing_smoke",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.SMOKE_PROPS));
    private static final ForgeFlowingFluid.Properties SMOKE_PROPS = new ForgeFlowingFluid.Properties(
            SMOKE_TYPE, SMOKE_SOURCE, SMOKE_FLOWING);

    public static final RegistryObject<Item> FLUID_DROP_NONE = FLUID_DROP_ITEMS.register("fluid_drop_none", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FLUID_DROP_WATER = FLUID_DROP_ITEMS.register("fluid_drop_water",
            () -> new FluidDropItem(() -> ForgeRegistries.FLUID_TYPES.get().getValue(new ResourceLocation("water")), new Item.Properties()));
    public static final RegistryObject<Item> FLUID_DROP_LAVA = FLUID_DROP_ITEMS.register("fluid_drop_lava",
            () -> new FluidDropItem(() -> ForgeRegistries.FLUID_TYPES.get().getValue(new ResourceLocation("lava")), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        BLOCKS.register(eventBus);
        FLUID_DROPS.put("water", FLUID_DROP_WATER);
        FLUID_DROPS.put("lava", FLUID_DROP_LAVA);
        registerFluidDrops();
        FLUID_DROP_ITEMS.register(eventBus);
    }

    private static void registerFluidDrops() {
        registerDrop("hydrogen_peroxide", HYDROGEN_PEROXIDE_TYPE);
        registerDrop("sulfuric_acid", SULFURIC_ACID_TYPE);
        registerDrop("natural_gas", NATURAL_GAS_TYPE);
        registerDrop("steam", STEAM_TYPE);
        registerDrop("low_pressure_steam", LOW_PRESSURE_STEAM_TYPE);
        registerDrop("smoke", SMOKE_TYPE);
    }

    private static void registerDrop(String name, RegistryObject<FluidType> fluidTypeObj) {
        RegistryObject<Item> dropItem = FLUID_DROP_ITEMS.register("fluid_drop_" + name,
                () -> new FluidDropItem(fluidTypeObj::get, new Item.Properties()));
        FLUID_DROPS.put(name, dropItem);
    }

    public static ResourceLocation getGuiTexture(Fluid fluid) {
        FluidType type = fluid.getFluidType();
        if (type instanceof BaseFluidType base) return base.getGuiTexture();
        if (type == ForgeMod.WATER_TYPE.get()) return new ResourceLocation("trd", "textures/item/water.png");
        if (type == ForgeMod.LAVA_TYPE.get()) return new ResourceLocation("trd", "textures/item/lava.png");
        return DEFAULT_GUI_TEXTURE;
    }

    public static Item getFluidDrop(FluidType fluidType) {
        for (RegistryObject<Item> dropObj : FLUID_DROPS.values()) {
            Item item = dropObj.get();
            if (item instanceof FluidDropItem drop && drop.getFluidType() == fluidType) return item;
        }
        return null;
    }

    public static Map<String, RegistryObject<Item>> getAllFluidDrops() {
        return new HashMap<>(FLUID_DROPS);
    }
}