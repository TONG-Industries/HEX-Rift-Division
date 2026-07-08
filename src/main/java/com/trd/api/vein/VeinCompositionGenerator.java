package com.trd.api.vein;

import net.minecraft.util.RandomSource;

import java.util.*;

public class VeinCompositionGenerator {

    private static final List<FractionEntry> FRACTIONS = List.of(
            // Легкометаллическая — везде, но преобладает наверху
            new FractionEntry(FractionType.LIGHT_METAL, 0.0f, 1.0f, 30),
            // Тяжелометаллическая — везде, основная масса
            new FractionEntry(FractionType.HEAVY_METAL, 0.0f, 1.0f, 40),
            // Благородная — только ниже Y=40 (глубже 25% от диапазона)
            new FractionEntry(FractionType.NOBLE_METAL, 0.25f, 1.0f, 12),
            // Редкоземельная — глубоко (ниже Y=0)
            new FractionEntry(FractionType.RARE_EARTH, 0.35f, 1.0f, 8)
    );

    public static VeinComposition generate(int y, RandomSource random) {
        // depthFactor: 0.0 на Y=320, 1.0 на Y=-64
        float depthFactor = 1.0f - ((y + 64.0f) / 384.0f);
        depthFactor = Math.max(0.0f, Math.min(1.0f, depthFactor));

        List<FractionEntry> available = new ArrayList<>();
        for (FractionEntry entry : FRACTIONS) {
            if (depthFactor >= entry.minDepth && depthFactor <= entry.maxDepth) {
                available.add(entry);
            }
        }

        if (available.isEmpty()) {
            available.add(FRACTIONS.get(1)); // fallback heavy_metal
        }

        // Выбираем 2-4 фракции
        int count = 2 + random.nextInt(3);
        Collections.shuffle(available, new java.util.Random(random.nextLong()));
        List<FractionEntry> selected = new ArrayList<>(available.subList(0, Math.min(count, available.size())));

        // Распределяем проценты
        int totalWeight = selected.stream().mapToInt(e -> e.weight).sum();
        Map<FractionType, Integer> composition = new LinkedHashMap<>();
        int remaining = 100;

        for (int i = 0; i < selected.size(); i++) {
            FractionEntry entry = selected.get(i);
            int percent;
            if (i == selected.size() - 1) {
                percent = remaining;
            } else {
                percent = (entry.weight * 90) / totalWeight;
                int variation = Math.max(2, percent / 4);
                percent = percent - variation + random.nextInt(variation * 2 + 1);
                percent = Math.max(5, Math.min(remaining - 5 * (selected.size() - i - 1), percent));
            }
            composition.put(entry.fraction, percent);
            remaining -= percent;
        }

        return new VeinComposition(composition);
    }

    private record FractionEntry(FractionType fraction, float minDepth, float maxDepth, int weight) {}
}