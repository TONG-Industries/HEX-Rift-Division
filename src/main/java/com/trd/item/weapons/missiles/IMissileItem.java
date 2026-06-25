package com.trd.item.weapons.missiles;

/**
 * Интерфейс для предметов-ракет, используемых в ракетнице Тромбон.
 */
public interface IMissileItem {

    /**
     * Возвращает тип ракеты для определения эффекта взрыва.
     * @return тип ракеты: "standard", "he", "fire"
     */
    String getMissileType();

    /**
     * Возвращает урон ракеты.
     */
    float getMissileDamage();

    /**
     * Возвращает скорость полёта ракеты.
     */
    float getMissileSpeed();
}