package com.trd.api.rotation;

import net.minecraft.core.Direction;

/**
 * Базовый интерфейс для всего, что вращается.
 * Используется KineticNetwork для управления скоростью.
 */
public interface Rotational {

    /**
     * Роль блока в кинетической сети.
     *
     * PASSIVE   — валы, подшипники, тахометры: просто передают вращение, вклад в момент = 0.
     * GENERATOR — моторы, ветряки: производят момент. Их getTorque() суммируется в пул сети.
     * CONSUMER  — статоры, жернова: потребляют момент из пула сети через getConsumedTorque().
     *             Если суммарное потребление превышает пул генераторов — сеть перегружается и останавливается.
     */
    enum NodeRole { PASSIVE, GENERATOR, CONSUMER }

    /** Роль этого блока в кинетической сети. По умолчанию — пассивный узел. */
    default NodeRole getNodeRole() {
        // Обратная совместимость: если блок объявляет isSource(), используем его
        if (isSource()) return NodeRole.GENERATOR;
        if (getConsumedTorque() > 0) return NodeRole.CONSUMER;
        return NodeRole.PASSIVE;
    }
    // Получение текущих данных (для расчетов в сети)
    long getSpeed();
    long getTorque();

    // Установка данных (вызывается сетью после recalculate)
    void setSpeed(long speed);

    // Лимиты (нужны для проверки поломки/перегрузки)
    long getMaxSpeed();
    long getMaxTorque();

    // --- ДАННЫЕ ДЛЯ ФИЗИКИ (ЭТАП 1: Шаг 3) ---
    double getInertiaContribution(); // Масса блока (сопротивление разгону) [cite: 34]
    default float getBearingFrictionCoefficient() { return 0.0f; } // Коэффициент трения (для подшипников)
    long getMaxTorqueTolerance();

    default long getConsumedTorque() { return 0; }
    default float getFrictionMultiplier() { return 1.0f; }

    default long getGeneratedSpeed() {
        return 0; // По умолчанию обычные блоки ничего не генерируют
    }// Предел прочности до скручивания вала [cite: 36]
    // Метод специально для визуализатора Flywheel
    default long getVisualSpeed() {
        return getSpeed();
    }
    /**
     * Возвращает true, если этот блок производит энергию (мотор, ветряк).
     * @deprecated Используй getNodeRole() == NodeRole.GENERATOR для явной типизации.
     */
    @Deprecated
    default boolean isSource() {
        return false;
    }

    default Direction[] getPropagationDirections() {
        return Direction.values(); // По умолчанию — все стороны
    }

    /**
     * Вызывается сетью перед соединением блоков.
     * Позволяет блокам отказать в соединении (например, из-за разных диаметров валов).
     */
    // БЫЛО: default boolean canConnectMechanically(Direction direction, Rotational neighbor)
// СТАЛО:
    default boolean canConnectMechanically(net.minecraft.core.BlockPos myPos, net.minecraft.core.BlockPos neighborPos, Rotational neighbor) {
        return true;
    }

    default void setNetworkScale(float scale) {}
    default float getNetworkScale() { return 1.0f; }

    // НОВЫЙ МЕТОД: возвращает список позиций, с которыми блок МОЖЕТ соединиться
    default java.util.List<net.minecraft.core.BlockPos> getPotentialConnections(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos myPos) {
        return java.util.Collections.emptyList();
    }

    // НОВЫЙ МЕТОД: рассчитывает коэффициент передачи на соседа
    default float calculateTransmissionRatio(net.minecraft.core.BlockPos myPos, net.minecraft.core.BlockPos neighborPos, Rotational neighbor) {
        return 1.0f; // По умолчанию передаем 1 к 1
    }

    default void forceSyncVisuals(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (!level.isClientSide && this instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
        }
    }
}