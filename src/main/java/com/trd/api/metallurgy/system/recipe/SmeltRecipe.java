package com.trd.api.metallurgy.system.recipe;

import com.trd.api.metallurgy.system.Metal;

/**
 * @param input Входной предмет
 * @param output Выходной металл
 * @param outputUnits Выход в единицах металла (не в UNITS_PER_INGOT * n, а точное значение!)
 * @param minTemp Минимальная температура для начала плавки
 * @param heatConsumption Потребление температуры за тик (градусы/тик)
 * @param smeltTimeTicks Время плавки в тиках
 */
public record SmeltRecipe(
        net.minecraft.world.item.Item input,
        Metal output,
        int outputUnits,
        int minTemp,
        float heatConsumption,
        int smeltTimeTicks
) {

    /**
     * Общее потребление температуры = heatConsumption * smeltTimeTicks
     */
    public float getTotalHeatConsumption() {
        return heatConsumption * smeltTimeTicks;
    }
}