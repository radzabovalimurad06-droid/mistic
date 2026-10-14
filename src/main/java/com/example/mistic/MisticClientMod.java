package com.example.mistic;

import com.example.mistic.entity.MisticEntityRenderer;
import com.example.mistic.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class MisticClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.MISTIC, MisticEntityRenderer::new);
        MisticMod.LOGGER.info("Mistic client initialized.");
    }
}
