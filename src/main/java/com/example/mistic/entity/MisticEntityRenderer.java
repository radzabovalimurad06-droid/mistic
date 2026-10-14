package com.example.mistic.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class MisticEntityRenderer extends MobEntityRenderer<MisticEntity, MisticEntityModel> {

    // Заглушка — берём текстуру эндермена
    // Позже заменишь на свою: mistic:textures/entity/mistic.png
    private static final Identifier TEXTURE =
            Identifier.of("mistic", "textures/entity/mistic.png");

    public MisticEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MisticEntityModel(
                context.getPart(EntityModelLayers.ENDERMAN)), 0.5f);
    }

    @Override
    public Identifier getTexture(MisticEntity entity) {
        return TEXTURE;
    }
}
