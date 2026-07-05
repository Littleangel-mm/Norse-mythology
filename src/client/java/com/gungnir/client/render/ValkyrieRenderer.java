package com.gungnir.client.render;

import com.gungnir.GungnirMod;
import com.gungnir.entity.ValkyrieEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class ValkyrieRenderer extends HumanoidMobRenderer<ValkyrieEntity, ValkyrieModel<ValkyrieEntity>> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(GungnirMod.id("valkyrie"), "main");
	private static final ResourceLocation TEXTURE = GungnirMod.id("textures/entity/valkyrie.png");

	public ValkyrieRenderer(EntityRendererProvider.Context context) {
		super(context, new ValkyrieModel<>(context.bakeLayer(LAYER_LOCATION)), 0.45F);
		addLayer(new HumanoidArmorLayer<>(
			this,
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
			context.getModelManager()
		));
		addLayer(new ValkyrieElytraLayer(this, context.getModelSet()));
		addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(ValkyrieEntity entity) {
		return TEXTURE;
	}
}
