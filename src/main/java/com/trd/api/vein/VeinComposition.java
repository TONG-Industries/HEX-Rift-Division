package com.trd.api.vein;

import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VeinComposition {
    private final Map<FractionType, Integer> fractions; // fraction -> percent
    private final int slagPercent;

    public VeinComposition(Map<FractionType, Integer> fractions) {
        this.fractions = new HashMap<>(fractions);
        int total = this.fractions.values().stream().mapToInt(Integer::intValue).sum();
        this.slagPercent = Math.max(0, 100 - total);
    }

    public Map<FractionType, Integer> getFractions() {
        return Collections.unmodifiableMap(fractions);
    }

    public int getSlagPercent() {
        return slagPercent;
    }

    public Map<FractionType, Integer> getFullComposition() {
        return Collections.unmodifiableMap(fractions);
    }

    public FractionType getPrimaryFraction() {
        return fractions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(FractionType.HEAVY_METAL);
    }

    /**
     * Для обратной совместимости: возвращает основной металл основной фракции.
     */
    public String getPrimaryMetal() {
        return getPrimaryFraction().getPrimaryMetal();
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        fractions.forEach((fraction, percent) -> tag.putInt(fraction.name(), percent));
        return tag;
    }

    public static VeinComposition deserialize(CompoundTag tag) {
        Map<FractionType, Integer> fractions = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            try {
                FractionType fraction = FractionType.valueOf(key);
                fractions.put(fraction, tag.getInt(key));
            } catch (IllegalArgumentException e) {
                // Миграция со старого формата: металлы маппим на фракции
                FractionType mapped = mapOldMetalToFraction(key);
                if (mapped != null) {
                    fractions.merge(mapped, tag.getInt(key), Integer::sum);
                }
            }
        }
        return new VeinComposition(fractions);
    }

    private static FractionType mapOldMetalToFraction(String metal) {
        return switch (metal) {
            case "aluminum", "titanium", "beryllium" -> FractionType.LIGHT_METAL;
            case "iron", "copper", "zinc", "lead", "tin", "tungsten", "nickel" -> FractionType.HEAVY_METAL;
            case "gold", "silver" -> FractionType.NOBLE_METAL;
            case "uranium", "rare_earth" -> FractionType.RARE_EARTH;
            default -> null;
        };
    }
}