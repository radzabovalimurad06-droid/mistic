package com.example.mistic;

import com.example.mistic.entity.MisticSpawner;
import com.example.mistic.registry.ModEntities;
import com.example.mistic.registry.ModSounds;
import com.example.mistic.registry.ModSpawnEggs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MisticMod implements ModInitializer {
    public static final String MOD_ID = "mistic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModSounds.register();
        ModEntities.register();
        ModSpawnEggs.register();
        MisticSpawner.register();

        FabricDefaultAttributeRegistry.register(
                ModEntities.MISTIC,
                com.example.mistic.entity.MisticEntity.createMisticAttributes()
        );

        LOGGER.info("Mistic mod initialized. He is watching...");
    }
}
