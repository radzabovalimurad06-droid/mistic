package com.example.mistic.registry;

import com.example.mistic.MisticMod;
import com.example.mistic.entity.MisticEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<MisticEntity> MISTIC = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MisticMod.MOD_ID, "mistic"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MisticEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 2.0f)) // 0.6 ширина, 2.0 высота
                    .trackRangeBlocks(96)  // видно с 96 блоков (для спавна на 55-70)
                    .trackedUpdateRate(3)
                    .build()
    );

    public static void register() {
        MisticMod.LOGGER.info("Registering entities for " + MisticMod.MOD_ID);
    }
}
