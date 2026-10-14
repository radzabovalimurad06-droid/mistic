package com.example.mistic.registry;

import com.example.mistic.MisticMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final Identifier HEARTBEAT_ID = Identifier.of(MisticMod.MOD_ID, "mistic_heartbeat");
    public static final SoundEvent MISTIC_HEARTBEAT = SoundEvent.of(HEARTBEAT_ID);

    public static final Identifier GROWL_ID = Identifier.of(MisticMod.MOD_ID, "mistic_growl");
    public static final SoundEvent MISTIC_GROWL = SoundEvent.of(GROWL_ID);

    public static final Identifier STEP_ID = Identifier.of(MisticMod.MOD_ID, "mistic_step");
    public static final SoundEvent MISTIC_STEP = SoundEvent.of(STEP_ID);

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, HEARTBEAT_ID, MISTIC_HEARTBEAT);
        Registry.register(Registries.SOUND_EVENT, GROWL_ID, MISTIC_GROWL);
        Registry.register(Registries.SOUND_EVENT, STEP_ID, MISTIC_STEP);
        MisticMod.LOGGER.info("Registering sounds for " + MisticMod.MOD_ID);
    }
}
