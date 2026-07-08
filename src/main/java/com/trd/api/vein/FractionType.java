package com.trd.api.vein;

import java.util.Collections;
import java.util.Map;

/**
 * Фракции конгломерата. Каждая фракция — это группа металлов в виде соединений,
 * которые требуют специфической переработки.
 */
public enum FractionType {
    LIGHT_METAL("light_metal", 0xA0C4D9, Map.of(
            "aluminum", 70,
            "titanium", 20,
            "beryllium", 10
    )),
    HEAVY_METAL("heavy_metal", 0x8B4513, Map.of(
            "iron", 35,
            "copper", 25,
            "zinc", 15,
            "lead", 12,
            "tin", 10,
            "tungsten", 3
    )),
    NOBLE_METAL("noble_metal", 0xFFD700, Map.of(
            "gold", 100
    )),
    RARE_EARTH("rare_earth", 0x9370DB, Map.of(
            "neodymium", 100
    ));

    private final String name;
    private final int color;
    private final Map<String, Integer> metalWeights;

    FractionType(String name, int color, Map<String, Integer> metalWeights) {
        this.name = name;
        this.color = color;
        this.metalWeights = metalWeights;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public Map<String, Integer> getMetalWeights() {
        return Collections.unmodifiableMap(metalWeights);
    }

    /**
     * Основной металл фракции (по весу).
     * Используется для отображения типа жилы.
     */
    public String getPrimaryMetal() {
        return metalWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
    }
}