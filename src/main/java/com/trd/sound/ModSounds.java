package com.trd.sound;

import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MainRegistry.MOD_ID);

    public static final RegistryObject<SoundEvent> CRATE_BREAK = registerSoundEvents("crate_break");

    public static final RegistryObject<SoundEvent> BULLET_GROUND = registerSoundEvents("bullet_ground");
    public static final RegistryObject<SoundEvent> BULLET_IMPACT = registerSoundEvents("bullet_impact");

    public static final RegistryObject<SoundEvent> TURRET_FIRE = registerSoundEvents("turret_fire");
    public static final RegistryObject<SoundEvent> TURRET_LOCK = registerSoundEvents("turret_lock");

    public static final RegistryObject<SoundEvent> DRY_FIRE = registerSoundEvents("dry_fire");
    public static final RegistryObject<SoundEvent> GUNPULL = registerSoundEvents("gunpull");
    public static final RegistryObject<SoundEvent> HEAVY_GUNCLICK = registerSoundEvents("heavy_gunclick");
    public static final RegistryObject<SoundEvent> GUNCLICK = registerSoundEvents("gunclick");

    public static final RegistryObject<SoundEvent> BUTTON1 = registerSoundEvents("button1");

    public static final RegistryObject<SoundEvent> LEVER1 = registerSoundEvents("lever1");
    public static final RegistryObject<SoundEvent> LEVER2 = registerSoundEvents("lever2");

    public static final RegistryObject<SoundEvent> TOOL_TECH_BLEEP = registerSoundEvents("techbleep");
    public static final RegistryObject<SoundEvent> TOOL_TECH_BOOP = registerSoundEvents("techboop");
    public static final RegistryObject<SoundEvent> PICKAXE_HIT = registerSoundEvents("pickaxe_hit");
    public static final RegistryObject<SoundEvent> MUKE_EXPLOSION = registerSoundEvents("mukeexplosion");
    public static final RegistryObject<SoundEvent> BOUNCE_RANDOM = registerSoundEvents("item.bounce_random");
    // Вспомогательный метод
    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        // В 1.20.1 используем обычный конструктор ResourceLocation
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainRegistry.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
