package com.gungnir.client.render;

import com.gungnir.GungnirMod;
import com.gungnir.entity.ValkyrieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class ValkyrieElytraLayer extends RenderLayer<ValkyrieEntity, ValkyrieModel<ValkyrieEntity>> {
	private static final ResourceLocation TEXTURE = GungnirMod.id("textures/entity/valkyrie_elytra.png");
	private final ElytraModel<ValkyrieEntity> elytraModel;

	public ValkyrieElytraLayer(RenderLayerParent<ValkyrieEntity, ValkyrieModel<ValkyrieEntity>> renderer, EntityModelSet modelSet) {
		super(renderer);
		this.elytraModel = new ElytraModel<>(modelSet.bakeLayer(ModelLayers.ELYTRA));
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ValkyrieEntity valkyrie, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, 0.125D);
		this.getParentModel().copyPropertiesTo(this.elytraModel);
		this.elytraModel.setupAnim(valkyrie, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(TEXTURE), false, false);
		this.elytraModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}
}
