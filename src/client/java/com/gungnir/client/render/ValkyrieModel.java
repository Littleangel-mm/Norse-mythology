package com.gungnir.client.render;

import com.gungnir.entity.ValkyrieEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class ValkyrieModel<T extends ValkyrieEntity> extends HumanoidModel<T> {
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public ValkyrieModel(ModelPart root) {
		super(root);
		this.leftWing = this.body.getChild("left_wing");
		this.rightWing = this.body.getChild("right_wing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition body = mesh.getRoot().getChild("body");
		body.addOrReplaceChild(
			"left_wing",
			CubeListBuilder.create()
				.texOffs(40, 32)
				.addBox(0.0F, -3.0F, -0.5F, 13.0F, 22.0F, 1.0F, CubeDeformation.NONE),
			PartPose.offsetAndRotation(2.0F, -1.0F, 2.8F, 0.16F, -0.35F, 0.12F)
		);
		body.addOrReplaceChild(
			"right_wing",
			CubeListBuilder.create()
				.texOffs(40, 32)
				.mirror()
				.addBox(-13.0F, -3.0F, -0.5F, 13.0F, 22.0F, 1.0F, CubeDeformation.NONE),
			PartPose.offsetAndRotation(-2.0F, -1.0F, 2.8F, 0.16F, 0.35F, -0.12F)
		);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		float flap = entity.isFlying() ? Mth.sin(ageInTicks * 0.45F) * 0.38F : Mth.sin(ageInTicks * 0.08F) * 0.08F;
		float spread = entity.isFlying() ? 0.75F : 0.35F;
		this.leftWing.yRot = -spread - flap;
		this.rightWing.yRot = spread + flap;
		this.leftWing.xRot = 0.12F;
		this.rightWing.xRot = 0.12F;
		this.leftWing.zRot = entity.isFlying() ? 0.18F : 0.08F;
		this.rightWing.zRot = entity.isFlying() ? -0.18F : -0.08F;
	}
}
