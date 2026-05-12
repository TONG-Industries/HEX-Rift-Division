package com.trd.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import java.util.List;

public record SpecialVeinConfiguration(
        List<OreConfiguration.TargetBlockState> targets,
        BlockState state,
        int minSize,      // радиус на minY
        int maxSize,      // радиус на maxY
        int minY,
        int maxY,
        boolean respectAir, // true = не заменяет воздух (разрезание)
        float density,      // 0.0–1.0, плотность заполнения
        float noiseScale    // можно оставить 0.1f, позже добавишь шум
) implements FeatureConfiguration {

    public static final Codec<SpecialVeinConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            OreConfiguration.TargetBlockState.CODEC.listOf().fieldOf("targets").forGetter(SpecialVeinConfiguration::targets),
            BlockState.CODEC.fieldOf("state").forGetter(SpecialVeinConfiguration::state),
            Codec.INT.fieldOf("min_size").forGetter(SpecialVeinConfiguration::minSize),
            Codec.INT.fieldOf("max_size").forGetter(SpecialVeinConfiguration::maxSize),
            Codec.INT.fieldOf("min_y").forGetter(SpecialVeinConfiguration::minY),
            Codec.INT.fieldOf("max_y").forGetter(SpecialVeinConfiguration::maxY),
            Codec.BOOL.fieldOf("respect_air").forGetter(SpecialVeinConfiguration::respectAir),
            Codec.FLOAT.fieldOf("density").forGetter(SpecialVeinConfiguration::density),
            Codec.FLOAT.optionalFieldOf("noise_scale", 0.1f).forGetter(SpecialVeinConfiguration::noiseScale)
    ).apply(instance, SpecialVeinConfiguration::new));
}