package com.gungnir.client.mixin;

import com.gungnir.GungnirMod;
import com.gungnir.GungnirTridentState;
import com.gungnir.client.render.GungnirSpearModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTridentRenderer.class)
public class ThrownTridentRendererMixin {
	@Unique
	private static final ResourceLocation GUNGNIR_LOCATION = GungnirMod.id("textures/entity/gungnir.png");
	@Unique
	private static final GungnirSpearModel GUNGNIR_MODEL = new GungnirSpearModel(GungnirSpearModel.createLayer().bakeRoot());

	@Inject(method = "render(Lnet/minecraft/world/entity/projectile/ThrownTrident;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
	private void gungnir$renderAsSpear(ThrownTrident trident, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo info) {
		if (!((GungnirTridentState) trident).gungnir$isGungnirProjectile()) {
			return;
		}

		matrices.pushPose();
		matrices.mulPose(Axis.YP.rotationDegrees(Mth.lerp(tickDelta, trident.yRotO, trident.getYRot()) - 90.0F));
		matrices.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(tickDelta, trident.xRotO, trident.getXRot()) + 90.0F));
		matrices.scale(0.92F, 0.92F, 0.92F);
		VertexConsumer vertices = vertexConsumers.getBuffer(GUNGNIR_MODEL.renderType(GUNGNIR_LOCATION));
		GUNGNIR_MODEL.renderToBuffer(matrices, vertices, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		matrices.popPose();
		info.cancel();
	}
}
