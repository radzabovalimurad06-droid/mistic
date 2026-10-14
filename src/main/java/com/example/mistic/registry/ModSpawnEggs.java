package com.example.mistic.registry;

import com.example.mistic.MisticMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModSpawnEggs {

    public static final Identifier MISTIC_EGG_ID = Identifier.of(MisticMod.MOD_ID, "mistic_spawn_egg");
    public static final RegistryKey<Item> MISTIC_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, MISTIC_EGG_ID);

    public static final Item MISTIC_SPAWN_EGG = new SpawnEggItem(
            ModEntities.MISTIC,
            new Item.Settings().registryKey(MISTIC_EGG_KEY)
    );

    public static void register() {
        Registry.register(Registries.ITEM, MISTIC_EGG_KEY, MISTIC_SPAWN_EGG);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(MISTIC_SPAWN_EGG);
        });

        MisticMod.LOGGER.info("Registering spawn eggs for " + MisticMod.MOD_ID);
    }
}
