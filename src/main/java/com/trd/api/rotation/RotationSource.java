package com.trd.api.rotation;

/**
 * Компактный контейнер для передачи пары Скорость + Крутящий момент.
 */
public record RotationSource(long speed, long torque) {
    public static final RotationSource EMPTY = new RotationSource(0, 0);
}