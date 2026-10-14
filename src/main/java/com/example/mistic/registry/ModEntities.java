package com.example.mistic.registry;

import com.example.mistic.MisticMod;
import com.example.mistic.entity.MisticEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<MisticEntity> MISTIC = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MisticMod.MOD_ID, "mistic"),
            EntityType.Builder.<MisticEntity>create(MisticEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 2.0f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(3)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE,
                            Identifier.of(MisticMod.MOD_ID, "mistic")))
    );

    public static void register() {
        MisticMod.LOGGER.info("Registering entities for " + MisticMod.MOD_ID);
    }
}
