package com.trd.client.render.flywheel;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ModModels {
    // Теперь мы используем String ключи (например "gear1_steel" или "shaft_iron")
    public static final Map<String, PartialModel> GEAR_MODELS = new HashMap<>();
    public static final Map<String, PartialModel> SHAFT_MODELS = new HashMap<>();

    public static final PartialModel MOTOR_BASE = PartialModel.of(new ResourceLocation("trd", "block/electro_motor"));
    public static final PartialModel HALF_SHAFT = PartialModel.of(new ResourceLocation("trd", "block/half_shaft"));
    public static final PartialModel BEARING_INNER_RING = PartialModel.of(new ResourceLocation("trd", "block/bearing_shaft"));
    public static final PartialModel BEARING = PartialModel.of(new ResourceLocation("trd", "block/bearing"));
    public static final PartialModel BEVEL_GEAR = PartialModel.of(new ResourceLocation("trd", "block/bevel_gear"));
    public static final PartialModel TACHOMETER = PartialModel.of(new ResourceLocation("trd", "block/tachometr"));
    public static final Map<String, PartialModel> PULLEY_MODELS = new HashMap<>();
    public static final PartialModel BELT_SEGMENT = PartialModel.of(new ResourceLocation("trd", "block/belt_segment"));

    public static final PartialModel COPPER_ROTOR = PartialModel.of(new ResourceLocation("trd", "block/rotor"));
    public static final PartialModel FLYWHEEL = PartialModel.of(new ResourceLocation("trd", "block/flywheel_light"));

    public static final PartialModel STATOR = PartialModel.of(new ResourceLocation("trd", "block/stator"));
    public static final Map<String, PartialModel> STATOR_COILS = new HashMap<>();


    public static final PartialModel FUEL_TANK_BIG = PartialModel.of(new ResourceLocation("trd", "block/fuel_tank_big"));
    public static final PartialModel BOILER = PartialModel.of(new ResourceLocation("trd", "block/boiler"));

    // Статический блок вызывается самым первым, как только Java видит этот класс!
    // Flywheel 100% получит эти модели вовремя.
    static {
        String[] materials = {"iron", "duralumin", "steel", "titanium", "tungsten_carbide"};

        // 1. Загружаем все 15 шестерней
        int[] gearSizes = {1, 2, 3};
        for (int size : gearSizes) {
            for (String mat : materials) {
                String name = "gear" + size + "_" + mat; // Получится "gear1_steel"
                GEAR_MODELS.put(name, PartialModel.of(new ResourceLocation("trd", "block/" + name)));
            }
        }

        // 2. Загружаем все валы
        // ВНИМАНИЕ: Проверь, как именно у тебя называются ID валов в регистрации.
        // Если они называются "shaft_light_iron", оставь так. Если "iron_shaft" - поменяй логику склейки строки.
        String[] diameters = {"light", "medium", "heavy"};
        for (String dia : diameters) {
            for (String mat : materials) {
                String name = "shaft_" + dia + "_" + mat; // Проверь правильность названия!
                SHAFT_MODELS.put(name, PartialModel.of(new ResourceLocation("trd", "block/" + name)));
            }
        }

        PULLEY_MODELS.put("pulley", PartialModel.of(new ResourceLocation("trd", "block/pulley")));
        STATOR_COILS.put("copper", PartialModel.of(new ResourceLocation("trd", "block/stator_coil_copper")));
    }

    // Оставляем пустым. Этот метод просто служит "спусковым крючком",
    // чтобы заставить игру прочитать класс ModModels при запуске.
    public static void init() {}
}