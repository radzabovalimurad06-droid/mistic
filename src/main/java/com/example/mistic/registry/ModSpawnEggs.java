package com.example.mistic.registry;

import com.example.mistic.MisticMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModSpawnEggs {
    public static final Item MISTIC_SPAWN_EGG = new SpawnEggItem(
            ModEntities.MISTIC,
            new Item.Settings()
    );

    public static void register() {
        Registry.register(Registries.ITEM,
                Identifier.of(MisticMod.MOD_ID, "mistic_spawn_egg"),
                MISTIC_SPAWN_EGG);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(MISTIC_SPAWN_EGG);
        });

        MisticMod.LOGGER.info("Registering spawn eggs for " + MisticMod.MOD_ID);
    }
}
