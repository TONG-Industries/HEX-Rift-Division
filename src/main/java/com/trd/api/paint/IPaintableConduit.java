package com.trd.api.paint;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Общий интерфейс для окрашиваемых труб/проводов.
 * BlockEntity возвращает BlockState блока, чью текстуру надо имитировать (или null).
 * Один PaintableConduitRenderer обслуживает и трубу, и провод.
 */

public interface IPaintableConduit {
    BlockState getMimicState();

// import net.minecraft.resources.ResourceLocation;

default net.minecraft.resources.ResourceLocation getCoreTexture() {
    return new net.minecraft.resources.ResourceLocation("trd", "block/conduit_core");
}}
