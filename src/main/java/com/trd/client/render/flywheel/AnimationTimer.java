package com.trd.client.render.flywheel;

/**
 * Единый таймер анимации для всех Flywheel визуализаторов.
 * Обновляется ОДИН РАЗ за кадр через TickEvent.RenderTickEvent,
 * гарантируя что все блоки используют идентичное время.
 *
 * Это решает проблему рассинхронизации при использовании
 * System.currentTimeMillis() — каждый визуализатор получал
 * чуть разное время из-за микрозадержек между вызовами beginFrame.
 */
public final class AnimationTimer {

    private static long frameTimeMs = System.currentTimeMillis();
    private static float frameDeltaSeconds = 0f;
    private static long lastFrameTimeMs = System.currentTimeMillis();

    private AnimationTimer() {}

    /**
     * Вызывается ОДИН РАЗ в начале каждого render tick из ClientModEvents.
     * Все визуализаторы, вызванные после этого, увидят одинаковые значения.
     */
    public static void onFrameStart() {
        long now = System.currentTimeMillis();
        frameDeltaSeconds = (now - lastFrameTimeMs) / 1000f;
        // Защита от слишком больших дельт (пауза/лаг/первый кадр)
        if (frameDeltaSeconds > 0.25f) frameDeltaSeconds = 0.016f;
        lastFrameTimeMs = now;
        frameTimeMs = now;
    }

    /**
     * @return Единое время текущего кадра в миллисекундах.
     *         Одинаковое для ВСЕХ визуализаторов в одном кадре.
     */
    public static long getFrameTimeMs() {
        return frameTimeMs;
    }

    /**
     * @return Время между текущим и предыдущим кадром в секундах.
     *         Одинаковое для ВСЕХ визуализаторов в одном кадре.
     */
    public static float getFrameDeltaSeconds() {
        return frameDeltaSeconds;
    }

    /**
     * @return Монотонное время кадра в секундах (для globalAngle).
     *         Используется как основа для детерминированного вычисления фазы.
     */
    public static float getFrameTimeSeconds() {
        return (float) (frameTimeMs % 100_000_000L) / 1000f;
    }
}
