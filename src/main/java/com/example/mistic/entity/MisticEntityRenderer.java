package com.example.mistic.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.util.Identifier;

public class MisticEntityRenderer extends MobEntityRenderer<MisticEntity, ZombieEntityRenderState, ZombieEntityModel<ZombieEntityRenderState>> {

    private static final Identifier TEXTURE =
            Identifier.of("mistic", "textures/entity/mistic.png");

    public MisticEntityRenderer(EntityRendererFactory.Context context) {
        super(context,
                new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)),
                0.5f);
    }

    @Override
    public ZombieEntityRenderState createRenderState() {
        return new ZombieEntityRenderState();
    }

    @Override
    public Identifier getTexture(ZombieEntityRenderState state) {
        return TEXTURE;
    }
}
