package com.gungnir.client.render;

import com.gungnir.GungnirMod;
import com.gungnir.entity.EinherjarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class EinherjarRenderer extends HumanoidMobRenderer<EinherjarEntity, HumanoidModel<EinherjarEntity>> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(GungnirMod.id("einherjar"), "main");
	private static final ResourceLocation TEXTURE = GungnirMod.id("textures/entity/einherjar.png");

	public EinherjarRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(LAYER_LOCATION)), 0.5F);
		addLayer(new HumanoidArmorLayer<>(
			this,
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
			context.getModelManager()
		));
		addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	public static LayerDefinition createBodyLayer() {
		return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
	}

	@Override
	public ResourceLocation getTextureLocation(EinherjarEntity entity) {
		return TEXTURE;
	}
}
