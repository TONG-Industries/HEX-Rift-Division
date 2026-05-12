package com.trd.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public class OculusCompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("compat.irisflw.mixin")) {
            // Проверка на конфликты ПРЯМО ТУТ
            boolean hasConflict = Thread.currentThread().getContextClassLoader().getResource("irisflw.mixins.json") != null;

            if (hasConflict) {
                // Леон или кто-то другой уже здесь — выключаем наши патчи
                return false;
            }

            // Проверка на наличие самого Окулуса (чтобы не упасть без него)
            return Thread.currentThread().getContextClassLoader().getResource("net/irisshaders/iris/Iris.class") != null;
        }
        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}